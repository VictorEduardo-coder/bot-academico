package com.universidade.bot.service;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailReaderService {

    private static final Map<String, Integer> PALAVRAS_IMPORTANCIA = new LinkedHashMap<>();

    static {
        PALAVRAS_IMPORTANCIA.put("prova final", 10);
        PALAVRAS_IMPORTANCIA.put("exame final", 10);
        PALAVRAS_IMPORTANCIA.put("prova", 8);
        PALAVRAS_IMPORTANCIA.put("exame", 8);
        PALAVRAS_IMPORTANCIA.put("avaliação", 7);
        PALAVRAS_IMPORTANCIA.put("trabalho", 6);
        PALAVRAS_IMPORTANCIA.put("entrega", 5);
        PALAVRAS_IMPORTANCIA.put("relatório", 4);
        PALAVRAS_IMPORTANCIA.put("apresentação", 6);
        PALAVRAS_IMPORTANCIA.put("seminário", 5);
        PALAVRAS_IMPORTANCIA.put("lista de exercícios", 4);
        PALAVRAS_IMPORTANCIA.put("prova prática", 9);
        PALAVRAS_IMPORTANCIA.put("prova teórica", 8);
        PALAVRAS_IMPORTANCIA.put("recuperação", 9);
        PALAVRAS_IMPORTANCIA.put("pendência", 7);
    }

    private static final Pattern[] PADROES_DATA = {
        Pattern.compile("(\\d{1,2})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{2,4})"),
        Pattern.compile("(\\d{1,2})\\s+de\\s+(janeiro|fevereiro|março|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)(?:\\s+de\\s+(\\d{2,4}))?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(segunda|terça|quarta|quinta|sexta|sábado|domingo)\\s+(?:que\\s+ vem|próxima|desta\\s+semana)", Pattern.CASE_INSENSITIVE)
    };

    private static final Map<String, Integer> MESES_MAP = new HashMap<>();

    static {
        MESES_MAP.put("janeiro", 1);
        MESES_MAP.put("fevereiro", 2);
        MESES_MAP.put("março", 3);
        MESES_MAP.put("abril", 4);
        MESES_MAP.put("maio", 5);
        MESES_MAP.put("junho", 6);
        MESES_MAP.put("julho", 7);
        MESES_MAP.put("agosto", 8);
        MESES_MAP.put("setembro", 9);
        MESES_MAP.put("outubro", 10);
        MESES_MAP.put("novembro", 11);
        MESES_MAP.put("dezembro", 12);
    }

    public void verificarNovosPrazos() {
        DatabaseManager db = DatabaseManager.getInstance();
        List<String[]> configs = db.listarTodasEmailConfigs();

        if (configs.isEmpty()) {
            System.out.println("Nenhuma configuracao de email encontrada. Pulando verificacao.");
            return;
        }

        for (String[] config : configs) {
            String userId = config[0];
            String email = config[1];
            String senha = config[2];
            processarEmails(userId, email, senha);
        }
    }

    private void processarEmails(String userId, String email, String senha) {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");

        try {
            Session emailSession = Session.getInstance(properties);
            Store store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", email, senha);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            int processados = 0;

            for (Message message : messages) {
                if (message.isSet(Flags.Flag.SEEN)) {
                    continue;
                }

                String assunto = message.getSubject();
                if (assunto == null) continue;

                int importancia = calcularImportancia(assunto);

                if (importancia > 0) {
                    processarMensagem(message, userId, importancia);
                    processados++;
                }
            }

            inbox.close(true);
            store.close();

            if (processados > 0) {
                System.out.println("Processados " + processados + " emails importantes do usuario " + userId);
            }

        } catch (Exception e) {
            System.err.println("Erro ao verificar emails do usuario " + userId + ": " + e.getMessage());
        }
    }

    private int calcularImportancia(String assunto) {
        String assuntoLower = assunto.toLowerCase();
        int importanciaMaxima = 0;

        for (Map.Entry<String, Integer> entry : PALAVRAS_IMPORTANCIA.entrySet()) {
            if (assuntoLower.contains(entry.getKey())) {
                importanciaMaxima = Math.max(importanciaMaxima, entry.getValue());
            }
        }

        if (assuntoLower.contains("urgente") || assuntoLower.contains("importante")) {
            importanciaMaxima = Math.max(importanciaMaxima, 9);
        }

        if (assuntoLower.contains("amanhã") || assuntoLower.contains("hoje")) {
            importanciaMaxima = Math.max(importanciaMaxima, 10);
        }

        return importanciaMaxima;
    }

    private void processarMensagem(Message message, String userId, int importancia) {
        try {
            String assunto = message.getSubject();
            String corpo = extrairCorpo(message);
            String textoCompleto = assunto + " " + corpo;

            LocalDateTime dataEvento = extrairData(textoCompleto);

            if (dataEvento == null) {
                dataEvento = estimarDataPorUrgencia(importancia);
            }

            if (dataEvento != null && dataEvento.isAfter(LocalDateTime.now())) {
                String tipoEvento = determinarTipoEvento(assunto);

                DatabaseManager db = DatabaseManager.getInstance();
                var disciplinas = db.listarPorUsuario(userId);

                boolean eventoAdicionado = false;

                for (Disciplina disciplina : disciplinas) {
                    if (assunto.toLowerCase().contains(disciplina.getNome().toLowerCase())) {
                        if (!db.eventoDuplicado(disciplina.getId(), tipoEvento, dataEvento)) {
                            EventoAcademico evento = new EventoAcademico(
                                    disciplina.getId(),
                                    tipoEvento,
                                    dataEvento,
                                    assunto
                            );
                            db.adicionarEvento(evento);
                            System.out.println(String.format(
                                    "Evento detectado: %s para %s em %s (Importancia: %d/10)",
                                    tipoEvento,
                                    disciplina.getNome(),
                                    dataEvento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    importancia
                            ));
                            eventoAdicionado = true;
                        }
                        break;
                    }
                }

                if (!eventoAdicionado && !disciplinas.isEmpty()) {
                    Disciplina primeiraDisciplina = disciplinas.get(0);
                    if (!db.eventoDuplicado(primeiraDisciplina.getId(), tipoEvento, dataEvento)) {
                        EventoAcademico evento = new EventoAcademico(
                                primeiraDisciplina.getId(),
                                tipoEvento,
                                dataEvento,
                                assunto
                        );
                        db.adicionarEvento(evento);
                        System.out.println(String.format(
                                "Evento detectado (generico): %s em %s (Importancia: %d/10)",
                                tipoEvento,
                                dataEvento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                importancia
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
        }
    }

    private LocalDateTime extrairData(String texto) {
        String textoLower = texto.toLowerCase();

        for (Pattern padrao : PADROES_DATA) {
            Matcher matcher = padrao.matcher(textoLower);
            if (matcher.find()) {
                try {
                    if (matcher.groupCount() >= 3 && matcher.group(2) != null) {
                        int dia = Integer.parseInt(matcher.group(1));
                        String mesStr = matcher.group(2).toLowerCase();
                        Integer mes = MESES_MAP.get(mesStr);

                        if (mes != null) {
                            int ano = LocalDateTime.now().getYear();
                            if (matcher.group(3) != null) {
                                ano = Integer.parseInt(matcher.group(3));
                                if (ano < 100) ano += 2000;
                            }

                            LocalDateTime data = LocalDateTime.of(ano, mes, dia, 23, 59);
                            if (data.isAfter(LocalDateTime.now())) {
                                return data;
                            }
                        }
                    } else if (matcher.groupCount() >= 2) {
                        int dia = Integer.parseInt(matcher.group(1));
                        int mes = Integer.parseInt(matcher.group(2));
                        int ano = LocalDateTime.now().getYear();

                        if (matcher.group(3) != null) {
                            ano = Integer.parseInt(matcher.group(3));
                            if (ano < 100) ano += 2000;
                        }

                        if (mes >= 1 && mes <= 12 && dia >= 1 && dia <= 31) {
                            LocalDateTime data = LocalDateTime.of(ano, mes, dia, 23, 59);
                            if (data.isAfter(LocalDateTime.now())) {
                                return data;
                            }
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }

        return null;
    }

    private LocalDateTime estimarDataPorUrgencia(int importancia) {
        LocalDateTime agora = LocalDateTime.now();

        if (importancia >= 9) {
            return agora.plusDays(1);
        } else if (importancia >= 7) {
            return agora.plusDays(3);
        } else if (importancia >= 5) {
            return agora.plusDays(7);
        } else {
            return agora.plusDays(14);
        }
    }

    private String extrairCorpo(Message message) {
        try {
            if (message.isMimeType("text/plain")) {
                return (String) message.getContent();
            } else if (message.isMimeType("text/html")) {
                return (String) message.getContent();
            } else if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                return extrairTextoDeMultipart(mimeMultipart);
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private String extrairTextoDeMultipart(MimeMultipart mimeMultipart) {
        StringBuilder resultado = new StringBuilder();
        try {
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    resultado.append(bodyPart.getContent());
                }
            }
        } catch (Exception e) {
            return "";
        }
        return resultado.toString();
    }

    private String determinarTipoEvento(String assunto) {
        String assuntoLower = assunto.toLowerCase();
        if (assuntoLower.contains("prova") || assuntoLower.contains("exame")) {
            return "Prova";
        } else if (assuntoLower.contains("trabalho")) {
            return "Trabalho";
        } else if (assuntoLower.contains("entrega")) {
            return "Entrega";
        } else if (assuntoLower.contains("apresentação") || assuntoLower.contains("seminário")) {
            return "Apresentacao";
        } else if (assuntoLower.contains("relatório")) {
            return "Relatorio";
        }
        return "Avaliacao";
    }
}
