package com.example.Tbot.service;

import com.example.Tbot.telegram.TbotTelegram;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 주식 알림 서비스 (선택적 기능)
 * 특정 종목을 구독하면 가격 변동 시 자동으로 알림
 */
@Service
public class StockAlertService {

    private final StockService stockService;
    private final TbotTelegram telegram;

    // 사용자별 구독 종목 저장 (chatId -> stockName)
    private final Map<Long, String> subscriptions = new ConcurrentHashMap<>();
    // 이전 가격 저장
    private final Map<String, String> previousPrices = new ConcurrentHashMap<>();

    public StockAlertService(StockService stockService, @Lazy TbotTelegram telegram) {
        this.stockService = stockService;
        this.telegram = telegram;
    }

    /**
     * 주식 알림 구독
     */
    public String subscribe(Long chatId, String stockName) {
        subscriptions.put(chatId, stockName);
        return "✅ '" + stockName + "' 실시간 알림이 설정되었습니다.\n" +
                "가격 변동 시 자동으로 알림을 받습니다.";
    }

    /**
     * 주식 알림 구독 취소
     */
    public String unsubscribe(Long chatId) {
        String stockName = subscriptions.remove(chatId);
        if (stockName != null) {
            return "❌ '" + stockName + "' 알림이 해제되었습니다.";
        }
        return "구독 중인 종목이 없습니다.";
    }

    /**
     * 30초마다 구독 종목 체크
     */
    @Scheduled(fixedRate = 30000)
    public void checkPriceChanges() {
        subscriptions.forEach((chatId, stockName) -> {
            try {
                String currentInfo = stockService.getStockPrice(stockName);
                String previousInfo = previousPrices.get(stockName);

                // 가격이 변경되었으면 알림
                if (!currentInfo.equals(previousInfo)) {
                    telegram.sendMessageToChat(chatId, "🔔 " + stockName + " 가격 변동\n\n" + currentInfo);
                    previousPrices.put(stockName, currentInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 현재 구독 상태 확인
     */
    public String getSubscriptionStatus(Long chatId) {
        String stockName = subscriptions.get(chatId);
        if (stockName != null) {
            return "📌 현재 구독 중: " + stockName;
        }
        return "구독 중인 종목이 없습니다.";
    }
}