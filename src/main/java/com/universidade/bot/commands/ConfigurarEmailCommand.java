package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ConfigurarEmailCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("configurar_email")) return;

        String email = event.getOption("email").getAsString();
        String senha = event.getOption("senha").getAsString();
        String userId = event.getUser().getId();

        if (!email.contains("@")) {
            event.replyEmbeds(EmbedUtils.embedErro(
                    "Email Inválido",
                    "Por favor, insira um endereço de email válido."
            )).setEphemeral(true).queue();
            return;
        }

        DatabaseManager.getInstance().salvarEmailConfig(userId, email, senha);

        event.replyEmbeds(EmbedUtils.embedSucesso(
                "Email Configurado!",
                String.format("""
                        Seu email foi configurado com sucesso!
                        
                        **Email:** %s
                        **Servidor:** imap.gmail.com
                        
                        O bot verificará automaticamente novos prazos a cada 6 horas.
                        Use `/remover_email` para remover a configuração.
                        """, email
                )
        )).setEphemeral(true).queue();
    }
}
