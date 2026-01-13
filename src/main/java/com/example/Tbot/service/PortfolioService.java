package com.example.Tbot.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 포트폴리오 관리 서비스
 * 사용자의 보유 주식 정보 저장 및 수익률 계산
 */
@Service
public class PortfolioService {

    // 사용자별 포트폴리오 저장 (chatId -> Portfolio)
    private final Map<Long, Map<String, Stock>> portfolios = new ConcurrentHashMap<>();

    /**
     * 주식 정보 클래스
     */
    public static class Stock {
        private String stockName;      // 종목명
        private double buyPrice;       // 매수가
        private int quantity;          // 수량
        private double totalBuyPrice;  // 총 매수금액

        public Stock(String stockName, double buyPrice, int quantity) {
            this.stockName = stockName;
            this.buyPrice = buyPrice;
            this.quantity = quantity;
            this.totalBuyPrice = buyPrice * quantity;
        }

        public String getStockName() { return stockName; }
        public double getBuyPrice() { return buyPrice; }
        public int getQuantity() { return quantity; }
        public double getTotalBuyPrice() { return totalBuyPrice; }
    }

    /**
     * 주식 추가 또는 업데이트
     */
    public String addStock(Long chatId, String stockName, double buyPrice, int quantity) {
        if (buyPrice <= 0 || quantity <= 0) {
            return "❌ 매수가와 수량은 0보다 커야 합니다.";
        }

        portfolios.putIfAbsent(chatId, new ConcurrentHashMap<>());
        Map<String, Stock> userPortfolio = portfolios.get(chatId);

        Stock stock = new Stock(stockName, buyPrice, quantity);
        userPortfolio.put(stockName, stock);

        return String.format(
                "✅ 포트폴리오에 추가되었습니다!\n\n" +
                        "📊 %s\n" +
                        "💰 매수가: %,.0f원\n" +
                        "📦 수량: %,d주\n" +
                        "💵 총 매수금액: %,.0f원",
                stockName, buyPrice, quantity, buyPrice * quantity
        );
    }

    /**
     * 주식 삭제
     */
    public String removeStock(Long chatId, String stockName) {
        Map<String, Stock> userPortfolio = portfolios.get(chatId);
        if (userPortfolio == null || !userPortfolio.containsKey(stockName)) {
            return "❌ '" + stockName + "'이(가) 포트폴리오에 없습니다.";
        }

        userPortfolio.remove(stockName);
        return "✅ '" + stockName + "'을(를) 포트폴리오에서 삭제했습니다.";
    }

    /**
     * 포트폴리오 전체 조회
     */
    public String getPortfolio(Long chatId) {
        Map<String, Stock> userPortfolio = portfolios.get(chatId);
        if (userPortfolio == null || userPortfolio.isEmpty()) {
            return "❌ 포트폴리오가 비어있습니다.\n\n" +
                    "/add <종목명> <매수가> <수량> 으로 추가하세요.\n" +
                    "예: /add 삼성전자 71000 10";
        }

        StringBuilder sb = new StringBuilder("📊 내 포트폴리오\n\n");
        userPortfolio.values().forEach(stock -> {
            sb.append(String.format(
                    "• %s\n" +
                            "  매수가: %,.0f원 × %,d주 = %,.0f원\n\n",
                    stock.getStockName(),
                    stock.getBuyPrice(),
                    stock.getQuantity(),
                    stock.getTotalBuyPrice()
            ));
        });

        return sb.toString();
    }

    /**
     * 특정 종목의 수익률 계산
     */
    public String calculateProfit(Long chatId, String stockName, double currentPrice) {
        Map<String, Stock> userPortfolio = portfolios.get(chatId);
        if (userPortfolio == null || !userPortfolio.containsKey(stockName)) {
            return null; // 포트폴리오에 없으면 null 반환
        }

        Stock stock = userPortfolio.get(stockName);

        // 현재가 기준 평가금액
        double currentValue = currentPrice * stock.getQuantity();

        // 수익금
        double profit = currentValue - stock.getTotalBuyPrice();

        // 수익률
        double profitRate = (profit / stock.getTotalBuyPrice()) * 100;

        String profitIcon = profit >= 0 ? "🔺" : "🔻";
        String profitColor = profit >= 0 ? "+" : "";

        return String.format(
                "\n💼 내 포트폴리오\n" +
                        "━━━━━━━━━━━━━━━━━━\n" +
                        "매수가: %,.0f원 × %,d주\n" +
                        "매수금액: %,.0f원\n" +
                        "━━━━━━━━━━━━━━━━━━\n" +
                        "현재가: %,.0f원 × %,d주\n" +
                        "평가금액: %,.0f원\n" +
                        "━━━━━━━━━━━━━━━━━━\n" +
                        "%s 손익: %s%,.0f원\n" +
                        "%s 수익률: %s%.2f%%",
                stock.getBuyPrice(), stock.getQuantity(),
                stock.getTotalBuyPrice(),
                currentPrice, stock.getQuantity(),
                currentValue,
                profitIcon, profitColor, profit,
                profitIcon, profitColor, profitRate
        );
    }

    /**
     * 포트폴리오에 종목이 있는지 확인
     */
    public boolean hasStock(Long chatId, String stockName) {
        Map<String, Stock> userPortfolio = portfolios.get(chatId);
        return userPortfolio != null && userPortfolio.containsKey(stockName);
    }

    /**
     * 특정 종목 정보 가져오기
     */
    public Stock getStock(Long chatId, String stockName) {
        Map<String, Stock> userPortfolio = portfolios.get(chatId);
        if (userPortfolio == null) {
            return null;
        }
        return userPortfolio.get(stockName);
    }

    /**
     * 전체 포트폴리오 통계
     */
    public int getStockCount(Long chatId) {
        Map<String, Stock> userPortfolio = portfolios.get(chatId);
        return userPortfolio == null ? 0 : userPortfolio.size();
    }
}