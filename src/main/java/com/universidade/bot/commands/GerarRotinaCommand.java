package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.BlocoEstudo;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;
import com.universidade.bot.service.OptimizerService;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class GerarRotinaCommand extends ListenerAdapter {

    private final OptimizerService optimizerService;

    public GerarRotinaCommand(OptimizerService optimizerService) {
        this.optimizerService = optimizerService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("gerar_rotina")) return;

        String userId = event.getUser().getId();
        DatabaseManager db = DatabaseManager.getInstance();

        List<Disciplina> disciplinas = db.listarPorUsuario(userId);
        List<EventoAcademico> eventos = db.listarTodosEventos();

        if (disciplinas.isEmpty()) {
            event.replyEmbeds(EmbedUtils.embedAviso(
                    "Sem Disciplinas",
                    "Cadastre suas disciplinas primeiro usando `/adicionar_disciplina`."
            )).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        PriorityQueue<Disciplina> fila = optimizerService.gerarFilaDePrioridade(disciplinas, eventos);
        List<BlocoEstudo> blocos = optimizerService.alocarBlocosDeEstudo(fila);
        String cronograma = optimizerService.formatarCronograma(blocos);

        EmbedBuilder builder = EmbedUtils.builderPadrao("📋 Cronograma de Estudos");
        builder.setDescription(cronograma);
        builder.addField("Total de Blocos", blocos.size() + " blocos alocados", false);
        builder.addField("Gerado em",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), false);

        if (!eventos.isEmpty()) {
            StringBuilder eventosStr = new StringBuilder();
            for (EventoAcademico evento : eventos) {
                eventosStr.append(String.format("• %s em %s (%d dias)\n",
                        evento.getTipo(),
                        evento.getDataEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        evento.diasAteEvento()));
            }
            builder.addField("Próximos Eventos", eventosStr.toString(), false);
        }

        event.getHook().editOriginalEmbeds(builder.build()).queue();
    }
}
