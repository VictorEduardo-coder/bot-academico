package com.universidade.bot.service;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationService {

    private final JDA jda;

    public NotificationService(JDA jda) {
        this.jda = jda;
    }

    public void verificarEEnviarNotificacoes() {
        DatabaseManager db = DatabaseManager.getInstance();
        List<String[]> notificacoes = db.listarNotificacoesAtivas();

        for (String[] notif : notificacoes) {
            String userId = notif[0];
            String canalId = notif[1];
            int diasAntes = Integer.parseInt(notif[2]);

            List<Disciplina> disciplinas = db.listarPorUsuario(userId);
            Map<Long, String> disciplinaMap = disciplinas.stream()
                    .collect(Collectors.toMap(Disciplina::getId, Disciplina::getNome));

            List<EventoAcademico> eventosProximos = db.listarEventosProximos(diasAntes);

            List<EventoAcademico> eventosNotificar = eventosProximos.stream()
                    .filter(e -> disciplinaMap.containsKey(e.getDisciplinaId()))
                    .toList();

            if (!eventosNotificar.isEmpty()) {
                enviarNotificacao(canalId, userId, eventosNotificar, disciplinaMap, diasAntes);
            }
        }
    }

    private void enviarNotificacao(String canalId, String userId,
                                    List<EventoAcademico> eventos,
                                    Map<Long, String> disciplinaMap,
                                    int diasAntes) {
        try {
            TextChannel canal = jda.getTextChannelById(canalId);
            if (canal == null) return;

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Notificacao Academica");
            builder.setColor(new Color(255, 165, 0));
            builder.setTimestamp(LocalDateTime.now());

            StringBuilder descricao = new StringBuilder();

            for (EventoAcademico evento : eventos) {
                long diasAte = ChronoUnit.DAYS.between(LocalDateTime.now(), evento.getDataEvento());
                String nomeDisciplina = disciplinaMap.getOrDefault(evento.getDisciplinaId(), "Desconhecida");

                String urgencia;
                if (diasAte <= 1) {
                    urgencia = " **URGENTE!**";
                } else if (diasAte <= 3) {
                    urgencia = " **MUITO PROXIMO!**";
                } else {
                    urgencia = "";
                }

                descricao.append(String.format(
                    "%s %s - %s%s\nData: %s (%d dias)\n\n",
                    getEmoji(evento.getTipo()),
                    evento.getTipo(),
                    nomeDisciplina,
                    urgencia,
                    evento.getDataEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    diasAte
                ));

                DatabaseManager.getInstance().marcarEventoNotificado(evento.getId());
            }

            builder.setDescription(descricao.toString());
            builder.setFooter("Use /calendario para ver todos os eventos");

            canal.sendMessageEmbeds(builder.build()).queue();

        } catch (Exception e) {
            System.err.println("Erro ao enviar notificacao: " + e.getMessage());
        }
    }

    private String getEmoji(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "prova" -> "📝";
            case "trabalho" -> "📄";
            case "entrega" -> "📦";
            case "apresentacao" -> "🎤";
            case "relatorio" -> "📊";
            default -> "📌";
        };
    }
}
