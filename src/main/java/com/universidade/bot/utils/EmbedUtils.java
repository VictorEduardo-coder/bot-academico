package com.universidade.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.LocalDateTime;

public class EmbedUtils {

    private static final Color COR_PRINCIPAL = new Color(0, 153, 255);
    private static final Color COR_SUCESSO = new Color(0, 200, 83);
    private static final Color COR_ERRO = new Color(244, 67, 54);
    private static final Color COR_AVISO = new Color(255, 193, 7);

    public static MessageEmbed embedSucesso(String titulo, String descricao) {
        return new EmbedBuilder()
                .setTitle(titulo)
                .setDescription(descricao)
                .setColor(COR_SUCESSO)
                .setTimestamp(LocalDateTime.now())
                .setFooter("Bot Acadêmico")
                .build();
    }

    public static MessageEmbed embedErro(String titulo, String descricao) {
        return new EmbedBuilder()
                .setTitle(titulo)
                .setDescription(descricao)
                .setColor(COR_ERRO)
                .setTimestamp(LocalDateTime.now())
                .setFooter("Bot Acadêmico")
                .build();
    }

    public static MessageEmbed embedInfo(String titulo, String descricao) {
        return new EmbedBuilder()
                .setTitle(titulo)
                .setDescription(descricao)
                .setColor(COR_PRINCIPAL)
                .setTimestamp(LocalDateTime.now())
                .setFooter("Bot Acadêmico")
                .build();
    }

    public static MessageEmbed embedAviso(String titulo, String descricao) {
        return new EmbedBuilder()
                .setTitle(titulo)
                .setDescription(descricao)
                .setColor(COR_AVISO)
                .setTimestamp(LocalDateTime.now())
                .setFooter("Bot Acadêmico")
                .build();
    }

    public static EmbedBuilder builderPadrao(String titulo) {
        return new EmbedBuilder()
                .setTitle(titulo)
                .setColor(COR_PRINCIPAL)
                .setTimestamp(LocalDateTime.now())
                .setFooter("Bot Acadêmico");
    }
}
