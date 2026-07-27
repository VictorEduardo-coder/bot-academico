package com.universidade.bot.commands;

import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AdicionarDisciplinaCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("adicionar_disciplina")) return;

        String nome = event.getOption("nome").getAsString();
        int peso = event.getOption("peso").getAsInt();
        int dificuldade = event.getOption("dificuldade").getAsInt();
        int horas = event.getOption("horas").getAsInt();
        String userId = event.getUser().getId();

        if (peso < 1 || peso > 10 || dificuldade < 1 || dificuldade > 10) {
            event.replyEmbeds(EmbedUtils.embedErro(
                    "Valores Inválidos",
                    "Peso e dificuldade devem estar entre 1 e 10."
            )).setEphemeral(true).queue();
            return;
        }

        Disciplina disciplina = new Disciplina(null, nome, peso, dificuldade, horas, userId);
        DatabaseManager.getInstance().adicionarDisciplina(disciplina);

        event.replyEmbeds(EmbedUtils.embedSucesso(
                "Disciplina Adicionada!",
                String.format("""
                        **%s** foi adicionada com sucesso!
                        
                        **Peso:** %d/10
                        **Dificuldade:** %d/10
                        **Horas semanais:** %d
                        
                        Use `/gerar_rotina` para ver seu cronograma otimizado.
                        """, nome, peso, dificuldade, horas
                )
        )).queue();
    }
}
