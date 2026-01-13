package com.example.Tbot.service;

import com.example.Tbot.telegram.TbotTelegram;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 주식 알림 서비스 (포트폴리오 정보 포함)
 * 10초마다 가격 체크하여 변동 시 알림
 */
@Service
public class StockAlertService {

    private final StockService stockService;
    private final TbotTelegram telegram;

    // 사용자별 구독 종목 저장 (chatId -> stockName)
    private final Map<Long, String> subscriptions = new ConcurrentHashMap<>();

    // 종목별 전체 정보 저장 (비교용)
    private final Map<String, String> previousInfos = new ConcurrentHashMap<>();

    public StockAlertService(StockService stockService, @Lazy TbotTelegram telegram) {
        this.stockService = stockService;
        this.telegram = telegram;
    }

    /**
     * 주식 알림 구독
     */
    public String subscribe(Long chatId, String stockName) {
        subscriptions.put(chatId, stockName);

        // 초기 가격 저장 (포트폴리오 정보 포함)
        try {
            String initialInfo = stockService.getStockPrice(stockName, chatId);
            previousInfos.put(stockName, initialInfo);

            return "✅ '" + stockName + "' 실시간 알림이 설정되었습니다.\n" +
                    "10초마다 가격을 체크하여 변동 시 알림을 보냅니다.\n\n" +
                    "현재 정보:\n" + initialInfo;
        } catch (Exception e) {
            return "✅ '" + stockName + "' 알림이 설정되었습니다.\n" +
                    "(초기 정보 조회 실패: " + e.getMessage() + ")";
        }
    }

    /**
     * 주식 알림 구독 취소
     */
    public String unsubscribe(Long chatId) {
        String stockName = subscriptions.remove(chatId);
        if (stockName != null) {
            return "❌ '" + stockName + "' 알림이 해제되었습니다.";
        }
        return "❌ 구독 중인 종목이 없습니다.";
    }

    /**
     * 10초마다 구독 종목 체크 및 알림 (포트폴리오 정보 포함)
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void checkPriceChanges() {
        if (subscriptions.isEmpty()) {
            return;
        }

        System.out.println("🔍 [" + new java.util.Date() + "] 주식 가격 체크 시작 - 구독자 수: " + subscriptions.size());

        subscriptions.forEach((chatId, stockName) -> {
            try {
                // 현재 주식 정보 조회 (포트폴리오 정보 포함)
                String currentInfo = stockService.getStockPrice(stockName, chatId);
                String previousInfo = previousInfos.get(stockName);

                // 가격이 변경되었는지 확인
                if (previousInfo == null || !currentInfo.equals(previousInfo)) {

                    // 가격 변동 알림 전송
                    String alertMessage = buildAlertMessage(stockName, currentInfo, previousInfo);
                    telegram.sendMessageToChat(chatId, alertMessage);

                    // 이전 정보 업데이트
                    previousInfos.put(stockName, currentInfo);

                    System.out.println("📤 알림 전송: " + stockName + " -> chatId: " + chatId);
                } else {
                    System.out.println("⏸️ 가격 변동 없음: " + stockName);
                }

            } catch (Exception e) {
                System.err.println("❌ 알림 전송 오류 (" + stockName + "): " + e.getMessage());

                // 오류 발생 시 사용자에게 알림
                telegram.sendMessageToChat(chatId,
                        "⚠️ '" + stockName + "' 정보 조회 중 오류가 발생했습니다.\n" +
                                "오류: " + e.getMessage());
            }
        });

        System.out.println("✅ 주식 가격 체크 완료\n");
    }

    /**
     * 알림 메시지 생성
     */
    private String buildAlertMessage(String stockName, String currentInfo, String previousInfo) {
        if (previousInfo == null) {
            return "🔔 " + stockName + " 실시간 알림 시작\n\n" + currentInfo;
        } else {
            return "🔔 " + stockName + " 가격 변동 알림!\n\n" + currentInfo;
        }
    }

    /**
     * 현재 구독 상태 확인
     */
    public String getSubscriptionStatus(Long chatId) {
        String stockName = subscriptions.get(chatId);
        if (stockName != null) {
            String currentInfo = previousInfos.get(stockName);

            String status = "📌 현재 구독 중: " + stockName + "\n";
            status += "⏱️ 체크 주기: 10초\n\n";

            if (currentInfo != null) {
                status += "마지막 확인 정보:\n" + currentInfo;
            }

            return status;
        }
        return "❌ 구독 중인 종목이 없습니다.";
    }

    /**
     * 전체 구독 목록 확인 (디버깅용)
     */
    public String getAllSubscriptions() {
        if (subscriptions.isEmpty()) {
            return "현재 구독자가 없습니다.";
        }

        StringBuilder sb = new StringBuilder("📊 전체 구독 현황\n\n");
        subscriptions.forEach((chatId, stockName) -> {
            sb.append("• ChatID: ").append(chatId)
                    .append(" → ").append(stockName)
                    .append("\n");
        });

        return sb.toString();
    }
}