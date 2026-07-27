package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ConfigurarNotificacaoCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("configurar_notificacao")) return;

        int diasAntes = event.getOption("dias") != null ?
                event.getOption("dias").getAsInt() : 3;
        String hora = event.getOption("hora") != null ?
                event.getOption("hora").getAsString() : "09:00";

        String userId = event.getUser().getId();
        String canalId = event.getChannel().getId();

        if (!hora.matches("\\d{2}:\\d{2}")) {
            event.replyEmbeds(EmbedUtils.embedErro(
                    "Formato Invalido",
                    "Use o formato HH:MM para a hora (ex: 09:00, 14:30)"
            )).setEphemeral(true).queue();
            return;
        }

        DatabaseManager.getInstance().salvarNotificacao(userId, canalId, diasAntes, hora);

        event.replyEmbeds(EmbedUtils.embedSucesso(
                "Notificacao Configurada!",
                String.format("""
                        Suas notificacoes foram configuradas:
                        
                        **Canal:** %s
                        **Dias de antecedencia:** %d
                        **Hora da notificacao:** %s
                        
                        O bot enviara lembretes automaticos quando houver eventos proximos.
                        """, canalId, diasAntes, hora
                )
        )).setEphemeral(true).queue();
    }
}
