package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RemoverEmailCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("remover_email")) return;

        String userId = event.getUser().getId();

        boolean removido = DatabaseManager.getInstance().removerEmailConfig(userId);

        if (removido) {
            event.replyEmbeds(EmbedUtils.embedSucesso(
                    "Email Removido",
                    "Sua configuração de email foi removida. O bot não verificará mais seus emails."
            )).setEphemeral(true).queue();
        } else {
            event.replyEmbeds(EmbedUtils.embedErro(
                    "Nenhum Email Configurado",
                    "Você não possui nenhum email configurado."
            )).setEphemeral(true).queue();
        }
    }
}
