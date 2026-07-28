package com.universidade.bot.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;
import com.universidade.bot.security.EncryptionService;
import com.universidade.bot.service.OptimizerService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIServer {
    private static APIServer instance;
    private Javalin app;
    private final Gson gson = new Gson();
    private final OptimizerService optimizerService = new OptimizerService();
    private final EncryptionService encryption = EncryptionService.getInstance();

    private APIServer() {}

    public static synchronized APIServer getInstance() {
        if (instance == null) {
            instance = new APIServer();
        }
        return instance;
    }

    public void iniciar(int porta) {
        app = Javalin.create(config -> {
            config.staticFiles.add("/web", Location.CLASSPATH);
        })
        .before("/*", ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, X-Session-Token");
            if (ctx.method().name().equals("OPTIONS")) {
                ctx.status(200);
            }
        })
        .get("/", ctx -> ctx.redirect("/web/index.html"));

        registrarRotas();
        app.start(porta);
        System.out.println("API Server rodando na porta " + porta);
    }

    private void registrarRotas() {
        app.get("/api/health", this::healthCheck);

        app.post("/api/auth/login", this::login);

        app.get("/api/disciplinas", this::listarDisciplinas);
        app.post("/api/disciplinas", this::adicionarDisciplina);
        app.delete("/api/disciplinas/{nome}", this::removerDisciplina);

        app.get("/api/eventos", this::listarEventos);
        app.get("/api/eventos/proximos/{dias}", this::listarEventosProximos);

        app.get("/api/cronograma", this::gerarCronograma);

        app.post("/api/configurar-email", this::configurarEmail);
        app.get("/api/config-email", this::obterConfigEmail);
        app.delete("/api/config-email", this::removerConfigEmail);
    }

    private String resolveUserId(Context ctx) {
        String token = ctx.header("X-Session-Token");
        if (token == null || token.isEmpty()) return null;
        return encryption.getSession(token);
    }

    private void healthCheck(Context ctx) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ok");
        response.addProperty("timestamp", LocalDateTime.now().toString());
        ctx.json(response.toString());
    }

    private void login(Context ctx) {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
            String email = body.get("email").getAsString();
            String senha = body.get("senha").getAsString();

            String internalId = "user_" + Math.abs((email + senha).hashCode());

            String token = encryption.getOrCreateSession(internalId);

            String[][] configs = DatabaseManager.getInstance().listarTodasEmailConfigs().toArray(new String[0][]);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("token", token);
            ctx.json(response.toString());
        } catch (Exception e) {
            ctx.status(401);
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", "Credenciais invalidas");
            ctx.json(response.toString());
        }
    }

    private void listarDisciplinas(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }
        List<Disciplina> disciplinas = DatabaseManager.getInstance().listarPorUsuario(userId);
        ctx.json(gson.toJson(disciplinas));
    }

    private void adicionarDisciplina(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
            String nome = body.get("nome").getAsString();
            int peso = body.get("peso").getAsInt();
            int dificuldade = body.get("dificuldade").getAsInt();
            int horas = body.get("horas").getAsInt();

            Disciplina disciplina = new Disciplina(null, nome, peso, dificuldade, horas, userId);
            DatabaseManager.getInstance().adicionarDisciplina(disciplina);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("id", disciplina.getId());
            ctx.json(response.toString());
        } catch (Exception e) {
            ctx.status(400);
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
            ctx.json(response.toString());
        }
    }

    private void removerDisciplina(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        String nome = ctx.pathParam("nome");
        boolean removido = DatabaseManager.getInstance().removerDisciplina(nome, userId);

        JsonObject response = new JsonObject();
        response.addProperty("success", removido);
        ctx.json(response.toString());
    }

    private void listarEventos(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        List<Disciplina> disciplinas = DatabaseManager.getInstance().listarPorUsuario(userId);
        List<EventoAcademico> todosEventos = DatabaseManager.getInstance().listarTodosEventos();

        List<EventoAcademico> eventosUsuario = todosEventos.stream()
                .filter(e -> disciplinas.stream().anyMatch(d -> d.getId().equals(e.getDisciplinaId())))
                .toList();

        ctx.json(gson.toJson(eventosUsuario));
    }

    private void listarEventosProximos(Context ctx) {
        int dias = Integer.parseInt(ctx.pathParam("dias"));
        List<EventoAcademico> eventos = DatabaseManager.getInstance().listarEventosProximos(dias);
        ctx.json(gson.toJson(eventos));
    }

    private void gerarCronograma(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        List<Disciplina> disciplinas = DatabaseManager.getInstance().listarPorUsuario(userId);
        List<EventoAcademico> eventos = DatabaseManager.getInstance().listarTodosEventos();

        var fila = optimizerService.gerarFilaDePrioridade(disciplinas, eventos);
        var blocos = optimizerService.alocarBlocosDeEstudo(fila);

        Map<String, Object> response = new HashMap<>();
        response.put("blocos", blocos);
        response.put("totalBlocos", blocos.size());
        response.put("cronograma", optimizerService.formatarCronograma(blocos));

        ctx.json(gson.toJson(response));
    }

    private void configurarEmail(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
            String email = body.get("email").getAsString();
            String senha = body.get("senha").getAsString();

            DatabaseManager.getInstance().salvarEmailConfig(userId, email, senha);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            ctx.json(response.toString());
        } catch (Exception e) {
            ctx.status(400);
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
            ctx.json(response.toString());
        }
    }

    private void obterConfigEmail(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        String[] config = DatabaseManager.getInstance().obterEmailConfig(userId);

        JsonObject response = new JsonObject();
        if (config != null) {
            response.addProperty("configured", true);
            response.addProperty("emailMasked", maskEmail(config[0]));
        } else {
            response.addProperty("configured", false);
        }
        ctx.json(response.toString());
    }

    private void removerConfigEmail(Context ctx) {
        String userId = resolveUserId(ctx);
        if (userId == null) { ctx.status(401); return; }

        boolean removido = DatabaseManager.getInstance().removerEmailConfig(userId);

        JsonObject response = new JsonObject();
        response.addProperty("success", removido);
        ctx.json(response.toString());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        String masked = name.substring(0, Math.min(2, name.length())) + "***@" + parts[1];
        return masked;
    }

    public void parar() {
        if (app != null) {
            app.stop();
        }
    }
}
