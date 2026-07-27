package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class ListarDisciplinasCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("listar_disciplinas")) return;

        String userId = event.getUser().getId();
        List<Disciplina> disciplinas = DatabaseManager.getInstance().listarPorUsuario(userId);

        if (disciplinas.isEmpty()) {
            event.replyEmbeds(EmbedUtils.embedAviso(
                    "Nenhuma Disciplina",
                    "Você ainda não cadastrou nenhuma disciplina.\nUse `/adicionar_disciplina` para começar."
            )).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder builder = EmbedUtils.builderPadrao("📚 Suas Disciplinas");
        StringBuilder descricao = new StringBuilder();

        for (int i = 0; i < disciplinas.size(); i++) {
            Disciplina d = disciplinas.get(i);
            descricao.append(String.format(
                    "%d. **%s**\n   Peso: %d | Dificuldade: %d | Horas/sem: %d\n",
                    i + 1, d.getNome(), d.getPeso(), d.getDificuldade(), d.getHorasSemanaisNecessarias()
            ));
        }

        builder.setDescription(descricao.toString());
        builder.addField("Total", disciplinas.size() + " disciplina(s)", false);

        event.replyEmbeds(builder.build()).queue();
    }
}
