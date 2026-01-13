package com.example.Tbot.telegram;

import com.example.Tbot.service.*;
import com.example.Tbot.service.MenuSessionService.MenuLevel;
import org.springframework.stereotype.Component;

@Component
public class CommandRouter {

    private final CliService cliService;
    private final StockService stockService;
    private final StockAlertService stockAlertService;
    private final ContinuousMonitoringService monitoringService;
    private final PortfolioService portfolioService;
    private final SystemDiagnosticService diagnosticService;

    // ✅ 메뉴 관련 서비스 추가
    private final MenuService menuService;
    private final MenuSessionService menuSessionService;

    // 개발자 계정 (환경변수나 설정 파일에서 관리 권장)
    private static final Long DEVELOPER_CHAT_ID = 8501154254L; // 실제 개발자 chatId로 변경

    public CommandRouter(CliService cliService,
                         StockService stockService,
                         StockAlertService stockAlertService,
                         ContinuousMonitoringService monitoringService,
                         PortfolioService portfolioService,
                         SystemDiagnosticService diagnosticService,
                         MenuService menuService,
                         MenuSessionService menuSessionService) {
        this.cliService = cliService;
        this.stockService = stockService;
        this.stockAlertService = stockAlertService;
        this.monitoringService = monitoringService;
        this.portfolioService = portfolioService;
        this.diagnosticService = diagnosticService;
        this.menuService = menuService;
        this.menuSessionService = menuSessionService;
    }

    public String route(String message) {
        return route(message, null);
    }

    public String route(String message, Long chatId) {

        // 시작: 메인 메뉴
        if (message.equals("/start")) {
            if (chatId != null) {
                menuSessionService.setMenuState(chatId, MenuLevel.MAIN);
            }
            return menuService.showMainMenu();
        }

        // CLI 명령어
        if (message.startsWith("/cli ")) {
            String command = message.substring(5);
            cliService.executeAsync(command);
            return "✅ CLI 실행 시작: " + command;
        }

        // 🔍 종목 검색 (상태 기록 포함)
        if (message.startsWith("/search ") || message.startsWith("/find ")) {
            String keyword = message.substring(message.indexOf(" ") + 1).trim();
            if (chatId != null) {
                menuSessionService.setMenuState(chatId, MenuLevel.STOCK_SEARCH, "SEARCH_RESULTS");
            }
            return stockService.searchStocks(keyword, chatId);
        }

        // 종목 코드로 직접 조회
        if (message.startsWith("/code ")) {
            String code = message.substring(6).trim();
            return stockService.getStockByCode(code);
        }

        // 📂 메뉴 기반 라우팅 (숫자 입력: 1.항목/2.항목/3.항목 선택)
        if (chatId != null) {
            MenuSessionService.MenuState state = menuSessionService.getMenuState(chatId);

            // 메인 메뉴 선택
            if (state.level == MenuLevel.MAIN && message.matches("^\\d+$")) {
                switch (message) {
                    case "1" -> {
                        menuSessionService.setMenuState(chatId, MenuLevel.STOCK_SEARCH);
                        return menuService.showStockSearchMenu();
                    }
                    case "2" -> {
                        menuSessionService.setMenuState(chatId, MenuLevel.MARKET_INFO);
                        return menuService.showMarketInfoMenu();
                    }
                    case "3" -> {
                        menuSessionService.setMenuState(chatId, MenuLevel.PORTFOLIO);
                        return menuService.showPortfolioMenu();
                    }
                    case "4" -> {
                        menuSessionService.setMenuState(chatId, MenuLevel.ALERT_MONITOR);
                        return menuService.showAlertMonitorMenu();
                    }
                    case "5" -> {
                        menuSessionService.setMenuState(chatId, MenuLevel.HELP);
                        return menuService.showExamplesMenu();
                    }
                    default -> {
                        return menuService.getInvalidInputMessage();
                    }
                }
            }

            // 공통: 0은 메인으로
            if (!state.level.equals(MenuLevel.MAIN) && message.equals("0")) {
                menuSessionService.goToMain(chatId);
                return menuService.getBackMessage() + "\n\n" + menuService.showMainMenu();
            }

            // 1) 종목 검색 서브 메뉴
            if (state.level == MenuLevel.STOCK_SEARCH) {
                // 서브 선택
                if (message.matches("^\\d+$")) {
                    switch (message) {
                        case "1" -> {
                            menuSessionService.setMenuState(chatId, MenuLevel.STOCK_SEARCH, "WAIT_KEYWORD");
                            return menuService.showSearchPrompt();
                        }
                        case "2" -> {
                            return stockService.getStockList();
                        }
                        case "3" -> {
                            menuSessionService.setMenuState(chatId, MenuLevel.STOCK_SEARCH, "WAIT_CODE");
                            return menuService.showCodePrompt();
                        }
                    }
                }

                // 키워드 입력 대기 상태
                if ("WAIT_KEYWORD".equals(state.data) && !message.startsWith("/")) {
                    String keyword = message.trim();
                    if (keyword.isEmpty()) return menuService.getInvalidInputMessage();

                    // 검색 실행 후: 번호 선택 단계로(검색 결과 선택은 기존 숫자 로직 사용)
                    menuSessionService.setMenuState(chatId, MenuLevel.STOCK_SEARCH, "SEARCH_RESULTS");
                    return stockService.searchStocks(keyword, chatId);
                }

                // 코드 입력 대기 상태
                if ("WAIT_CODE".equals(state.data) && !message.startsWith("/")) {
                    String code = message.trim();
                    if (!code.matches("^\\d{6}$")) {
                        return "❌ 6자리 종목 코드를 입력하세요.\n예: 005930\n\n0️⃣ 이전 메뉴로";
                    }
                    return stockService.getStockByCode(code);
                }
            }

            // 2) 시장 정보 서브 메뉴
            if (state.level == MenuLevel.MARKET_INFO && message.matches("^\\d+$")) {
                switch (message) {
                    case "1" -> {
                        return stockService.getMarketIndex();
                    }
                    case "2" -> {
                        return stockService.getPopularStocks();
                    }
                    default -> {
                        return menuService.getInvalidInputMessage();
                    }
                }
            }

            // 3) 포트폴리오 서브 메뉴
            if (state.level == MenuLevel.PORTFOLIO) {
                if (message.matches("^\\d+$")) {
                    switch (message) {
                        case "1" -> {
                            return portfolioService.getPortfolio(chatId);
                        }
                        case "2" -> {
                            menuSessionService.setMenuState(chatId, MenuLevel.PORTFOLIO, "WAIT_ADD");
                            return menuService.showAddStockPrompt();
                        }
                        case "3" -> {
                            menuSessionService.setMenuState(chatId, MenuLevel.PORTFOLIO, "WAIT_REMOVE");
                            return menuService.showRemoveStockPrompt();
                        }
                    }
                }

                if ("WAIT_ADD".equals(state.data) && !message.startsWith("/")) {
                    // 형식: 종목명 매수가 수량
                    try {
                        String[] parts = message.trim().split("\\s+");
                        if (parts.length != 3) return menuService.getInvalidInputMessage();
                        String stockName = parts[0];
                        double buyPrice = Double.parseDouble(parts[1]);
                        int quantity = Integer.parseInt(parts[2]);
                        return portfolioService.addStock(chatId, stockName, buyPrice, quantity);
                    } catch (Exception e) {
                        return menuService.getInvalidInputMessage();
                    }
                }

                if ("WAIT_REMOVE".equals(state.data) && !message.startsWith("/")) {
                    String stockName = message.trim();
                    if (stockName.isEmpty()) return menuService.getInvalidInputMessage();
                    return portfolioService.removeStock(chatId, stockName);
                }
            }

            // 4) 알림/모니터링 서브 메뉴
            if (state.level == MenuLevel.ALERT_MONITOR) {
                if (message.matches("^\\d+$")) {
                    switch (message) {
                        case "1" -> {
                            menuSessionService.setMenuState(chatId, MenuLevel.ALERT_MONITOR, "WAIT_ALERT");
                            return menuService.showAlertPrompt();
                        }
                        case "2" -> {
                            menuSessionService.setMenuState(chatId, MenuLevel.ALERT_MONITOR, "WAIT_MONITOR");
                            return menuService.showMonitorPrompt();
                        }
                        case "3" -> {
                            return route("/status", chatId);
                        }
                        case "4" -> {
                            return route("/stop", chatId);
                        }
                    }
                }

                if ("WAIT_ALERT".equals(state.data) && !message.startsWith("/")) {
                    String stockName = message.trim();
                    if (stockName.isEmpty()) return menuService.getInvalidInputMessage();
                    return stockAlertService.subscribe(chatId, stockName);
                }

                if ("WAIT_MONITOR".equals(state.data) && !message.startsWith("/")) {
                    String stockName = message.trim();
                    if (stockName.isEmpty()) return menuService.getInvalidInputMessage();
                    return monitoringService.startMonitoring(chatId, stockName);
                }
            }

            // 5) 사용 예시 메뉴: 숫자 처리 없음(0만 처리됨)
            if (state.level == MenuLevel.HELP && message.matches("^\\d+$")) {
                return menuService.getInvalidInputMessage();
            }
        }

        // 💡 숫자만 입력한 경우 - 검색 결과에서 선택 (기존 로직 유지)
        if (message.matches("^\\d+$")) {
            if (chatId == null) {
                return "❓ 알 수 없는 명령어입니다.\n/help를 입력하여 사용법을 확인하세요.";
            }

            int index = Integer.parseInt(message);

            // 검색 결과 캐시에서 가져오기
            var searchResult = stockService.getSearchResultByIndex(chatId, index);

            if (searchResult == null) {
                return "❌ 검색 결과가 없습니다.\n\n" +
                        "💡 먼저 /search 명령어로 종목을 검색하세요.\n" +
                        "예: /search 현대\n\n" +
                        "검색 결과는 5분간 유지됩니다.";
            }

            // 선택된 종목 조회
            return stockService.getStockPrice(searchResult.name, chatId);
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
        if (message.startsWith("/stock ") || message.startsWith("/주식 ")) {
            String stockName = message.substring(message.indexOf(" ") + 1).trim();
            return stockService.getStockPrice(stockName, chatId);
        }

        // 여러 종목 조회
        if (message.startsWith("/stocks ")) {
            String stockNames = message.substring(8).trim();
            String[] stocks = stockNames.split(",");
            return stockService.getMultipleStocks(stocks);
        }

        // 시장 지수
        if (message.equals("/market") || message.equals("/지수")) {
            return stockService.getMarketIndex();
        }

        // 인기 종목
        if (message.equals("/popular") || message.equals("/인기")) {
            return stockService.getPopularStocks();
        }

        // 지원 종목 리스트
        if (message.equals("/list")) {
            return stockService.getStockList();
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

        // 도움말 (/help만)
        if (message.equals("/help")) {
            return """
                    📱 텔레그램 주식 봇
                    
                    🔍 종목 검색
                    /search <키워드> - 종목 검색 (부분검색 가능)
                    /stock <종목명> - 주식 현재가 조회
                    
                    📊 시장 정보
                    /market - 코스피/코스닥 지수
                    /popular - 인기 검색 종목
                    
                    💼 포트폴리오
                    /add <종목명> <매수가> <수량> - 주식 추가
                    /remove <종목명> - 주식 삭제
                    /portfolio - 내 포트폴리오
                    
                    🔔 알림/모니터링
                    /alert <종목명> - 가격 변동 알림
                    /monitor <종목명> - 10초마다 모니터링
                    /stop - 알림/모니터링 중지
                    /status - 현재 상태
                    
                    💡 사용 예시
                    /search 현대 → 현대 관련 종목 검색
                    /stock 삼성전자 → 시세 조회
                    /add 카카오 50000 5 → 포트폴리오 추가
                    """;
        }

        // API 테스트 명령어 (개발자 전용)
        if (message.equals("/test")) {
            // 개발자 권한 확인
            if (chatId == null || !chatId.equals(DEVELOPER_CHAT_ID)) {
                return "❌ 이 명령어는 개발자만 사용할 수 있습니다.";
            }

            return diagnosticService.runDiagnostics();
        }

        // 빠른 API 테스트 (개발자 전용)
        if (message.equals("/quicktest")) {
            if (chatId == null || !chatId.equals(DEVELOPER_CHAT_ID)) {
                return "❌ 이 명령어는 개발자만 사용할 수 있습니다.";
            }

            try {
                String testUrl = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:005930";
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                String response = restTemplate.getForObject(testUrl, String.class);

                if (response == null) {
                    return "❌ API 응답 없음";
                }

                return "✅ API 정상 작동\n\n" +
                        "URL: " + testUrl + "\n\n" +
                        "응답 길이: " + response.length() + "자\n\n" +
                        "응답 내용 (처음 500자):\n" +
                        response.substring(0, Math.min(500, response.length()));

            } catch (Exception e) {
                return "❌ API 테스트 실패\n\n" +
                        "오류: " + e.getClass().getSimpleName() + "\n" +
                        "메시지: " + e.getMessage();
            }
        }

        return "❓ 알 수 없는 명령어입니다.\n/help를 입력하여 사용법을 확인하세요.";
    }
}