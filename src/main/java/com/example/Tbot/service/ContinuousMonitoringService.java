package com.example.Tbot.service;

import com.example.Tbot.telegram.TbotTelegram;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 연속 모니터링 서비스 (포트폴리오 정보 포함)
 * 10초마다 주식 정보를 크롤링하여 텔레그램으로 전송
 */
@Service
public class ContinuousMonitoringService {

    private final StockService stockService;
    private final TbotTelegram telegram;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    // 연속 모니터링 중인 사용자 (chatId -> stockName)
    private final Map<Long, String> monitoring = new ConcurrentHashMap<>();

    // 모니터링 카운터 (몇 번 업데이트되었는지)
    private final Map<Long, Integer> updateCounts = new ConcurrentHashMap<>();

    public ContinuousMonitoringService(StockService stockService, @Lazy TbotTelegram telegram) {
        this.stockService = stockService;
        this.telegram = telegram;
    }

    /**
     * 연속 모니터링 시작
     */
    public String startMonitoring(Long chatId, String stockName) {
        monitoring.put(chatId, stockName);
        updateCounts.put(chatId, 0);

        return "🔄 '" + stockName + "' 연속 모니터링을 시작합니다.\n" +
                "10초마다 최신 정보를 전송합니다.\n" +
                "💼 포트폴리오 정보도 함께 표시됩니다.\n\n" +
                "중지하려면 /stop 입력";
    }

    /**
     * 연속 모니터링 중지
     */
    public String stopMonitoring(Long chatId) {
        String stockName = monitoring.remove(chatId);
        Integer count = updateCounts.remove(chatId);

        if (stockName != null) {
            return "⏹️ '" + stockName + "' 모니터링을 중지했습니다.\n" +
                    "총 " + count + "회 업데이트되었습니다.";
        }
        return "❌ 진행 중인 모니터링이 없습니다.";
    }

    /**
     * 10초마다 모니터링 중인 종목 정보 전송 (포트폴리오 정보 포함)
     */
    @Scheduled(fixedRate = 10000) // 10초
    public void sendContinuousUpdates() {
        if (monitoring.isEmpty()) {
            return;
        }

        String currentTime = dateFormat.format(new Date());
        System.out.println("🔄 [" + currentTime + "] 연속 모니터링 업데이트 - 모니터링 중: " + monitoring.size());

        monitoring.forEach((chatId, stockName) -> {
            try {
                // 주식 정보 조회 (포트폴리오 정보 포함)
                String stockInfo = stockService.getStockPrice(stockName, chatId);

                // 업데이트 횟수 증가
                int count = updateCounts.getOrDefault(chatId, 0) + 1;
                updateCounts.put(chatId, count);

                // 메시지 생성
                String message = String.format(
                        "🔄 실시간 모니터링 #%d\n⏰ %s\n\n%s\n\n중지하려면 /stop 입력",
                        count, currentTime, stockInfo
                );

                // 텔레그램 전송
                telegram.sendMessageToChat(chatId, message);

                System.out.println("✅ 모니터링 업데이트 전송: " + stockName + " (#" + count + ")");

            } catch (Exception e) {
                System.err.println("❌ 모니터링 오류 (" + stockName + "): " + e.getMessage());

                telegram.sendMessageToChat(chatId,
                        "⚠️ 모니터링 중 오류 발생\n" +
                                "종목: " + stockName + "\n" +
                                "오류: " + e.getMessage() + "\n\n" +
                                "중지하려면 /stop 입력");
            }
        });
    }

    /**
     * 현재 모니터링 상태 확인
     */
    public String getMonitoringStatus(Long chatId) {
        String stockName = monitoring.get(chatId);
        if (stockName != null) {
            int count = updateCounts.getOrDefault(chatId, 0);
            return "🔄 현재 모니터링 중: " + stockName + "\n" +
                    "⏱️ 업데이트 주기: 10초\n" +
                    "📊 업데이트 횟수: " + count + "회\n" +
                    "💼 포트폴리오 정보 포함\n\n" +
                    "중지하려면 /stop 입력";
        }
        return "❌ 진행 중인 모니터링이 없습니다.";
    }

    /**
     * 모니터링 중인지 확인
     */
    public boolean isMonitoring(Long chatId) {
        return monitoring.containsKey(chatId);
    }
}