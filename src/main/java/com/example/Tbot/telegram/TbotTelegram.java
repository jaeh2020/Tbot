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
            System.out.println("⚠️ 메시지가 없거나 텍스트가 아님");
            return;
        }

        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        System.out.println("📩 수신 메시지: " + text);
        System.out.println("📩 Chat ID: " + chatId);

        try {
            // ⭐ chatId를 함께 전달 (알림 기능에 필요)
            System.out.println("🔄 CommandRouter 호출 시작");
            String response = commandRouter.route(text, chatId);

            System.out.println("✅ CommandRouter 응답 받음");
            System.out.println("📤 응답 내용: " + (response != null ? response.substring(0, Math.min(50, response.length())) + "..." : "null"));

            if (response == null || response.trim().isEmpty()) {
                System.err.println("❌ 응답이 비어있음!");
                response = "⚠️ 응답을 생성하지 못했습니다. 다시 시도해주세요.";
            }

            sendMessage(chatId, response);

        } catch (Exception e) {
            System.err.println("❌ 메시지 처리 중 오류 발생");
            e.printStackTrace();

            sendMessage(chatId, "❌ 오류가 발생했습니다.\n" +
                    "오류 내용: " + e.getMessage() + "\n\n" +
                    "잠시 후 다시 시도해주세요.");
        }
    }

    private void sendMessage(Long chatId, String message) {
        System.out.println("📤 sendMessage 호출됨");
        System.out.println("📤 chatId = " + chatId);
        System.out.println("📤 message 길이 = " + (message != null ? message.length() : 0));

        if (message == null || message.trim().isEmpty()) {
            System.err.println("❌ 전송할 메시지가 비어있음!");
            message = "⚠️ 메시지가 비어있습니다.";
        }

        // 텔레그램 메시지 최대 길이는 4096자
        final int MAX_MESSAGE_LENGTH = 4096;

        if (message.length() <= MAX_MESSAGE_LENGTH) {
            // 일반 전송
            sendSingleMessage(chatId, message);
        } else {
            // 긴 메시지를 여러 개로 분할
            System.out.println("⚠️ 메시지가 너무 김 (" + message.length() + "자). 분할 전송 시작");

            int start = 0;
            int partNumber = 1;

            while (start < message.length()) {
                int end = Math.min(start + MAX_MESSAGE_LENGTH, message.length());

                // 중간에 잘리지 않도록 마지막 줄바꿈 위치에서 자르기
                if (end < message.length()) {
                    int lastNewline = message.lastIndexOf('\n', end);
                    if (lastNewline > start) {
                        end = lastNewline;
                    }
                }

                String part = message.substring(start, end);
                String partMessage = String.format("[%d/%d]\n%s",
                        partNumber,
                        (message.length() / MAX_MESSAGE_LENGTH) + 1,
                        part);

                sendSingleMessage(chatId, partMessage);

                start = end;
                partNumber++;

                // 연속 전송 시 너무 빠르지 않도록 약간의 딜레이
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void sendSingleMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .build();

        try {
            execute(sendMessage);
            System.out.println("✅ 메시지 전송 성공");
        } catch (Exception e) {
            System.err.println("❌ 텔레그램 메시지 전송 실패");
            System.err.println("❌ 에러 메시지: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 외부에서 메시지를 보낼 수 있도록 public 메서드
     * (StockAlertService에서 실시간 알림을 보낼 때 사용)
     */
    public void sendMessageToChat(Long chatId, String message) {
        System.out.println("📨 외부에서 메시지 전송 요청");
        sendMessage(chatId, message);
    }
}