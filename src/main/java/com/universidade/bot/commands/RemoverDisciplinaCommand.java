package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RemoverDisciplinaCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("remover_disciplina")) return;

        String nome = event.getOption("nome").getAsString();
        String userId = event.getUser().getId();

        boolean removido = DatabaseManager.getInstance().removerDisciplina(nome, userId);

        if (removido) {
            event.replyEmbeds(EmbedUtils.embedSucesso(
                    "Disciplina Removida",
                    String.format("**%s** foi removida do sistema.", nome)
            )).queue();
        } else {
            event.replyEmbeds(EmbedUtils.embedErro(
                    "Disciplina Não Encontrada",
                    String.format("Nenhuma disciplina chamada **%s** foi encontrada.", nome)
            )).setEphemeral(true).queue();
        }
    }
}
