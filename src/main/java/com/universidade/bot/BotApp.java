package com.universidade.bot;

import com.universidade.bot.api.APIServer;
import com.universidade.bot.bot.BotManager;
import com.universidade.bot.database.DatabaseManager;
import com.universidade.bot.service.EmailReaderService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotApp {
    public static void main(String[] args) {
        System.out.println("=== Bot Academico Discord ===");
        System.out.println("Iniciando sistema...");

        DatabaseManager.getInstance().inicializar();

        int apiPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        APIServer.getInstance().iniciar(apiPort);

        BotManager botManager = BotManager.getInstance();
        botManager.iniciar();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                new EmailReaderService().verificarNovosPrazos();
            } catch (Exception e) {
                System.err.println("Erro ao verificar emails: " + e.getMessage());
            }
        }, 1, 6, TimeUnit.HOURS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando bot...");
            botManager.encerrar();
            APIServer.getInstance().parar();
            scheduler.shutdown();
            DatabaseManager.getInstance().fechar();
        }));
    }
}
