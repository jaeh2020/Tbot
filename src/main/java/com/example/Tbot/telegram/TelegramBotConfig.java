package com.example.Tbot.telegram;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {
    // TEST
    private final TbotTelegram tbotTelegram;

    public TelegramBotConfig(TbotTelegram tbotTelegram) {
        this.tbotTelegram = tbotTelegram;
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(tbotTelegram);
            System.out.println("✅ Telegram Bot 등록 완료");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}