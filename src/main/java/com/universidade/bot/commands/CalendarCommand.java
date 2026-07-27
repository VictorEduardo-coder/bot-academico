package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("calendario")) return;

        String userId = event.getUser().getId();
        DatabaseManager db = DatabaseManager.getInstance();

        List<EventoAcademico> eventos = db.listarTodosEventos();
        List<Disciplina> disciplinas = db.listarPorUsuario(userId);

        Map<Long, String> disciplinaMap = disciplinas.stream()
                .collect(Collectors.toMap(Disciplina::getId, Disciplina::getNome));

        List<EventoAcademico> eventosUsuario = eventos.stream()
                .filter(e -> disciplinaMap.containsKey(e.getDisciplinaId()))
                .sorted(Comparator.comparing(EventoAcademico::getDataEvento))
                .toList();

        if (eventosUsuario.isEmpty()) {
            event.replyEmbeds(EmbedUtils.embedAviso(
                    "Calendario Vazio",
                    "Nenhum evento encontrado.\n\n" +
                    "**Como adicionar eventos:**\n" +
                    "• Use `/configurar_email` para monitorar automaticamente\n" +
                    "• O bot detecta provas, trabalhos e entregas nos emails"
            )).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder builder = EmbedUtils.builderPadrao("Calendario Academico");

        Map<String, List<EventoAcademico>> eventosPorMes = new LinkedHashMap<>();
        DateTimeFormatter mesFormato = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("pt", "BR"));

        for (EventoAcademico evento : eventosUsuario) {
            String mes = evento.getDataEvento().format(mesFormato);
            eventosPorMes.computeIfAbsent(mes, k -> new ArrayList<>()).add(evento);
        }

        for (Map.Entry<String, List<EventoAcademico>> entry : eventosPorMes.entrySet()) {
            StringBuilder conteudoMes = new StringBuilder();

            for (EventoAcademico evento : entry.getValue()) {
                long diasAte = ChronoUnit.DAYS.between(LocalDateTime.now(), evento.getDataEvento());
                String nomeDisciplina = disciplinaMap.getOrDefault(evento.getDisciplinaId(), "Desconhecida");
                String emoji = getEmojiPorDias(diasAte);

                conteudoMes.append(String.format(
                    "%s **%s** - %s\n`%s` (%d dias)\n\n",
                    emoji,
                    evento.getTipo(),
                    nomeDisciplina,
                    evento.getDataEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    diasAte
                ));
            }

            builder.addField(entry.getKey(), conteudoMes.toString(), false);
        }

        long totalDias = eventosUsuario.isEmpty() ? 0 :
                ChronoUnit.DAYS.between(LocalDateTime.now(), eventosUsuario.get(0).getDataEvento());

        builder.setFooter("Proximo evento em " + totalDias + " dia(s) | Total: " + eventosUsuario.size() + " evento(s)");

        event.replyEmbeds(builder.build()).queue();
    }

    private String getEmojiPorDias(long dias) {
        if (dias <= 1) return "🔴";
        if (dias <= 3) return "🟠";
        if (dias <= 7) return "🟡";
        if (dias <= 14) return "🟢";
        return "⚪";
    }
}
