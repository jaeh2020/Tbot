package com.example.Tbot.telegram;

import com.example.Tbot.service.CliService;
import com.example.Tbot.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class CommandRouter {

    private final CliService cliService;
    private final StockService stockService;

    public CommandRouter(CliService cliService, StockService stockService) {
        this.cliService = cliService;
        this.stockService = stockService;
    }

    public String route(String message) {

        // CLI 명령어
        if (message.startsWith("/cli ")) {
            String command = message.substring(5);
            cliService.executeAsync(command);
            return "CLI 실행 시작: " + command;
        }

        // 주식 조회 명령어
        if (message.startsWith("/stock ")) {
            String stockName = message.substring(7).trim();
            return stockService.getStockPrice(stockName);
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

        // 도움말
        if (message.equals("/help")) {
            return """
                    📱 사용 가능한 명령어:
                    
                    💰 주식 관련
                    /stock <종목명>     - 주식 현재가 조회
                    /stocks <종목1>,<종목2>  - 여러 종목 조회
                    /market             - 코스피/코스닥 지수
                    /popular            - 인기 검색 종목 TOP10
                    /list               - 조회 가능한 종목 리스트
                    
                    💻 시스템 관련
                    /cli <command>      - 서버 CLI 실행
                    /help               - 도움말
                    
                    📌 예시:
                    /stock 삼성전자
                    /stocks 삼성전자,네이버,카카오
                    """;
        }

        return "❓ 알 수 없는 명령어입니다. /help 입력";
    }
}