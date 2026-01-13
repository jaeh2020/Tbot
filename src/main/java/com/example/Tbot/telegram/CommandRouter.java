package com.example.Tbot.telegram;

import com.example.Tbot.service.CliService;
import com.example.Tbot.service.StockService;
import com.example.Tbot.service.StockAlertService;
import com.example.Tbot.service.ContinuousMonitoringService;
import com.example.Tbot.service.PortfolioService;
import org.springframework.stereotype.Component;

@Component
public class CommandRouter {

    private final CliService cliService;
    private final StockService stockService;
    private final StockAlertService stockAlertService;
    private final ContinuousMonitoringService monitoringService;
    private final PortfolioService portfolioService;

    public CommandRouter(CliService cliService,
                         StockService stockService,
                         StockAlertService stockAlertService,
                         ContinuousMonitoringService monitoringService,
                         PortfolioService portfolioService) {
        this.cliService = cliService;
        this.stockService = stockService;
        this.stockAlertService = stockAlertService;
        this.monitoringService = monitoringService;
        this.portfolioService = portfolioService;
    }

    public String route(String message) {
        return route(message, null);
    }

    public String route(String message, Long chatId) {

        // CLI 명령어
        if (message.startsWith("/cli ")) {
            String command = message.substring(5);
            cliService.executeAsync(command);
            return "✅ CLI 실행 시작: " + command;
        }

        // ⭐ 포트폴리오에 주식 추가
        if (message.startsWith("/add ")) {
            if (chatId == null) {
                return "❌ 포트폴리오 추가 실패: chatId가 필요합니다.";
            }

            try {
                String[] parts = message.substring(5).trim().split("\\s+");
                if (parts.length != 3) {
                    return "❌ 형식이 올바르지 않습니다.\n\n" +
                            "사용법: /add <종목명> <매수가> <수량>\n" +
                            "예: /add 삼성전자 71000 10";
                }

                String stockName = parts[0];
                double buyPrice = Double.parseDouble(parts[1]);
                int quantity = Integer.parseInt(parts[2]);

                return portfolioService.addStock(chatId, stockName, buyPrice, quantity);

            } catch (NumberFormatException e) {
                return "❌ 매수가와 수량은 숫자여야 합니다.\n" +
                        "예: /add 삼성전자 71000 10";
            }
        }

        // ⭐ 포트폴리오에서 주식 삭제
        if (message.startsWith("/remove ")) {
            if (chatId == null) {
                return "❌ 포트폴리오 삭제 실패: chatId가 필요합니다.";
            }
            String stockName = message.substring(8).trim();
            return portfolioService.removeStock(chatId, stockName);
        }

        // ⭐ 내 포트폴리오 조회
        if (message.equals("/portfolio") || message.equals("/mystock")) {
            if (chatId == null) {
                return "❌ 포트폴리오 조회 실패: chatId가 필요합니다.";
            }
            return portfolioService.getPortfolio(chatId);
        }

        // 주식 조회 명령어 (포트폴리오 정보 포함)
        if (message.startsWith("/stock ")) {
            String stockName = message.substring(7).trim();
            return stockService.getStockPrice(stockName, chatId);
        }

        // 여러 종목 조회
        if (message.startsWith("/stocks ")) {
            String stockNames = message.substring(8).trim();
            String[] stocks = stockNames.split(",");
            return stockService.getMultipleStocks(stocks);
        }

        // 시장 지수
        if (message.equals("/market")) {
            return stockService.getMarketIndex();
        }

        // 인기 종목
        if (message.equals("/popular")) {
            return stockService.getPopularStocks();
        }

        // 지원 종목 리스트
        if (message.equals("/list")) {
            return stockService.getSupportedStocks();
        }

        // 실시간 알림 구독 (가격 변동 시에만 알림, 포트폴리오 정보 포함)
        if (message.startsWith("/alert ")) {
            if (chatId == null) {
                return "❌ 알림 설정 실패: chatId가 필요합니다.";
            }
            String stockName = message.substring(7).trim();
            return stockAlertService.subscribe(chatId, stockName);
        }

        // 연속 모니터링 시작 (10초마다 무조건 알림, 포트폴리오 정보 포함)
        if (message.startsWith("/monitor ")) {
            if (chatId == null) {
                return "❌ 모니터링 시작 실패: chatId가 필요합니다.";
            }
            String stockName = message.substring(9).trim();
            return monitoringService.startMonitoring(chatId, stockName);
        }

        // 모니터링/알림 중지
        if (message.equals("/stop")) {
            if (chatId == null) {
                return "❌ 중지 실패: chatId가 필요합니다.";
            }

            // 모니터링과 알림 모두 중지
            String monitoringResult = monitoringService.stopMonitoring(chatId);
            String alertResult = stockAlertService.unsubscribe(chatId);

            if (monitoringResult.contains("진행 중인") && alertResult.contains("구독 중인")) {
                return "❌ 실행 중인 모니터링이나 알림이 없습니다.";
            }

            StringBuilder result = new StringBuilder();
            if (!monitoringResult.contains("진행 중인")) {
                result.append(monitoringResult).append("\n");
            }
            if (!alertResult.contains("구독 중인")) {
                result.append(alertResult);
            }

            return result.toString().trim();
        }

        // 알림 구독 취소
        if (message.equals("/unalert")) {
            if (chatId == null) {
                return "❌ 알림 해제 실패: chatId가 필요합니다.";
            }
            return stockAlertService.unsubscribe(chatId);
        }

        // 상태 확인
        if (message.equals("/status") || message.equals("/mystatus")) {
            if (chatId == null) {
                return "❌ 상태 조회 실패: chatId가 필요합니다.";
            }

            String alertStatus = stockAlertService.getSubscriptionStatus(chatId);
            String monitorStatus = monitoringService.getMonitoringStatus(chatId);
            int portfolioCount = portfolioService.getStockCount(chatId);

            StringBuilder status = new StringBuilder("📊 내 현황\n\n");

            // 포트폴리오 상태
            status.append("💼 포트폴리오:\n");
            status.append("• 보유 종목 수: ").append(portfolioCount).append("개\n\n");

            // 알림 상태
            status.append("🔔 가격 변동 알림:\n");
            if (alertStatus.contains("구독 중")) {
                status.append(alertStatus).append("\n\n");
            } else {
                status.append("• 없음\n\n");
            }

            // 모니터링 상태
            status.append("🔄 연속 모니터링:\n");
            if (monitorStatus.contains("모니터링 중")) {
                status.append(monitorStatus);
            } else {
                status.append("• 없음");
            }

            return status.toString();
        }

        // 도움말
        if (message.equals("/help") || message.equals("/start")) {
            return """
                    📱 텔레그램 주식 봇
                    
                    💰 주식 조회
                    /stock <종목명>          - 주식 현재가 조회
                    /stocks <종목1>,<종목2>  - 여러 종목 조회
                    /market                  - 코스피/코스닥 지수
                    /popular                 - 인기 검색 종목 TOP10
                    /list                    - 조회 가능한 종목 리스트
                    
                    💼 포트폴리오 관리
                    /add <종목명> <매수가> <수량>  - 보유 주식 추가
                    /remove <종목명>              - 보유 주식 삭제
                    /portfolio                    - 내 포트폴리오 조회
                    
                    🔔 실시간 알림 (가격 변동 시)
                    /alert <종목명>          - 가격 변동 시 알림
                    /unalert                 - 알림 해제
                    
                    🔄 연속 모니터링 (10초마다)
                    /monitor <종목명>        - 10초마다 정보 전송
                    /stop                    - 모니터링/알림 중지
                    /status                  - 현재 상태 확인
                    
                    💻 시스템
                    /cli <command>           - 서버 CLI 실행
                    /help                    - 도움말
                    """;
        }

        return "❓ 알 수 없는 명령어입니다.\n/help를 입력하여 사용법을 확인하세요.";
    }
}