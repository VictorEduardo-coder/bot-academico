package com.universidade.bot.bot;

import com.universidade.bot.commands.*;
import com.universidade.bot.service.NotificationService;
import com.universidade.bot.service.OptimizerService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotManager {
    private static BotManager instance;
    private JDA jda;
    private OptimizerService optimizerService;
    private NotificationService notificationService;
    private ScheduledExecutorService scheduler;

    private BotManager() {
        this.optimizerService = new OptimizerService();
    }

    public static synchronized BotManager getInstance() {
        if (instance == null) {
            instance = new BotManager();
        }
        return instance;
    }

    public void iniciar() {
        String token = System.getenv("DISCORD_TOKEN");

        if (token == null || token.isEmpty()) {
            System.err.println("ERRO: DISCORD_TOKEN nao configurado nas variaveis de ambiente.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS
                    )
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Estudando com voce!"))
                    .build();

            jda.awaitReady();

            registrarComandos();
            registrarEventListeners();
            iniciarSchedulerNotificacoes();

            System.out.println("Bot conectado como: " + jda.getSelfUser().getAsTag());
            System.out.println("Servidores: " + jda.getGuilds().size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Conexao interrompida: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao iniciar bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registrarComandos() {
        OptionData nomeDisciplina = new OptionData(
                OptionType.STRING, "nome", "Nome da disciplina", true
        );
        OptionData pesoDisciplina = new OptionData(
                OptionType.INTEGER, "peso", "Peso da disciplina (1-10)", true
        ).setRequiredRange(1, 10);
        OptionData dificuldadeDisciplina = new OptionData(
                OptionType.INTEGER, "dificuldade", "Dificuldade (1-10)", true
        ).setRequiredRange(1, 10);
        OptionData horasDisciplina = new OptionData(
                OptionType.INTEGER, "horas", "Horas semanais necessarias", true
        ).setRequiredRange(1, 40);

        OptionData emailOption = new OptionData(
                OptionType.STRING, "email", "Seu email institucional", true
        );
        OptionData senhaOption = new OptionData(
                OptionType.STRING, "senha", "Senha do email (app password)", true
        );

        OptionData diasOption = new OptionData(
                OptionType.INTEGER, "dias", "Dias de antecedencia para notificar (1-30)", false
        ).setRequiredRange(1, 30);
        OptionData horaOption = new OptionData(
                OptionType.STRING, "hora", "Horario da notificacao (HH:MM)", false
        );

        jda.updateCommands().addCommands(
                Commands.slash("adicionar_disciplina", "Adiciona uma nova disciplina ao sistema")
                        .addOptions(nomeDisciplina, pesoDisciplina, dificuldadeDisciplina, horasDisciplina),

                Commands.slash("remover_disciplina", "Remove uma disciplina do sistema")
                        .addOptions(new OptionData(OptionType.STRING, "nome", "Nome da disciplina", true)),

                Commands.slash("listar_disciplinas", "Lista todas as disciplinas cadastradas"),

                Commands.slash("gerar_rotina", "Gera um cronograma de estudos otimizado"),

                Commands.slash("configurar_email", "Configura seu email para monitoramento de prazos")
                        .addOptions(emailOption, senhaOption),

                Commands.slash("remover_email", "Remove a configuracao de email"),

                Commands.slash("calendario", "Mostra o calendario de eventos academicos"),

                Commands.slash("configurar_notificacao", "Configura notificacoes automaticas")
                        .addOptions(diasOption, horaOption)
        ).queue();

        System.out.println("Comandos slash registrados com sucesso.");
    }

    private void registrarEventListeners() {
        jda.addEventListener(new AdicionarDisciplinaCommand());
        jda.addEventListener(new RemoverDisciplinaCommand());
        jda.addEventListener(new ListarDisciplinasCommand());
        jda.addEventListener(new GerarRotinaCommand(optimizerService));
        jda.addEventListener(new ConfigurarEmailCommand());
        jda.addEventListener(new RemoverEmailCommand());
        jda.addEventListener(new CalendarCommand());
        jda.addEventListener(new ConfigurarNotificacaoCommand());

        System.out.println("Event listeners registrados.");
    }

    private void iniciarSchedulerNotificacoes() {
        notificationService = new NotificationService(jda);
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                notificationService.verificarEEnviarNotificacoes();
            } catch (Exception e) {
                System.err.println("Erro ao verificar notificacoes: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);

        System.out.println("Scheduler de notificacoes iniciado (verifica a cada 1 hora).");
    }

    public void encerrar() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public JDA getJda() {
        return jda;
    }
}
