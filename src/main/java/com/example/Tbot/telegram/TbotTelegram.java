package com.example.Tbot.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TbotTelegram extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.username}")
    private String username;

    private final CommandRouter commandRouter;

    public TbotTelegram(CommandRouter commandRouter) {
        this.commandRouter = commandRouter;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("📩 업데이트 수신");

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String text = update.getMessage().getText();
        System.out.println("📩 메시지: " + text);

        Long chatId = update.getMessage().getChatId();

        // ⭐ chatId를 함께 전달 (알림 기능에 필요)
        String response = commandRouter.route(text, chatId);

        sendMessage(chatId, response);
    }

    private void sendMessage(Long chatId, String message) {
        System.out.println("📤 sendMessage 호출됨");
        System.out.println("📤 chatId = " + chatId);
        System.out.println("📤 message = " + message);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .build();

        try {
            execute(sendMessage);
            System.out.println("✅ 메시지 전송 성공");
        } catch (Exception e) {
            System.err.println("❌ 텔레그램 메시지 전송 실패");
            e.printStackTrace();
        }
    }

    /**
     * 외부에서 메시지를 보낼 수 있도록 public 메서드
     * (StockAlertService에서 실시간 알림을 보낼 때 사용)
     */
    public void sendMessageToChat(Long chatId, String message) {
        sendMessage(chatId, message);
    }
}