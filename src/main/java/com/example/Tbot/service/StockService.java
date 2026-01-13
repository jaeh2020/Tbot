package com.example.Tbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class StockService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PortfolioService portfolioService;

    // 주요 종목 코드 매핑
    private final Map<String, String> stockCodes = new HashMap<>() {{
        put("삼성전자", "005930");
        put("sk하이닉스", "000660");
        put("네이버", "035420");
        put("카카오", "035720");
        put("현대차", "005380");
        put("lg에너지솔루션", "373220");
        put("셀트리온", "068270");
        put("삼성바이오로직스", "207940");
        put("포스코홀딩스", "005490");
        put("kb금융", "105560");
    }};

    public StockService(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * 주식 현재가 조회 (포트폴리오 정보 포함)
     */
    public String getStockPrice(String stockName) {
        return getStockPrice(stockName, null);
    }

    /**
     * 주식 현재가 조회 (포트폴리오 정보 포함 가능)
     */
    public String getStockPrice(String stockName, Long chatId) {
        try {
            String code = getStockCode(stockName);
            if (code == null) {
                return "❌ '" + stockName + "' 종목을 찾을 수 없습니다.\n사용 가능한 종목: " +
                        String.join(", ", stockCodes.keySet());
            }

            // 네이버 금융 API 호출
            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + code;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("result").path("areas").get(0).path("datas").get(0);

            String name = result.path("nm").asText();
            String currentPriceStr = result.path("nv").asText();
            String changePrice = result.path("cv").asText();
            String changeRate = result.path("cr").asText();
            String volume = result.path("aq").asText();

            // 현재가 숫자로 변환
            double currentPrice = Double.parseDouble(currentPriceStr.replace(",", ""));

            // 등락 상태 표시
            String arrow = changePrice.startsWith("-") ? "🔻" : "🔺";

            String basicInfo = String.format(
                    "📊 %s (%s)\n\n" +
                            "현재가: %s원\n" +
                            "%s 전일대비: %s원 (%s%%)\n" +
                            "거래량: %s주\n\n" +
                            "⏰ 실시간 조회",
                    name, code,
                    formatNumber(currentPriceStr),
                    arrow, changePrice, changeRate,
                    formatNumber(volume)
            );

            // 포트폴리오 정보 추가
            if (chatId != null && portfolioService.hasStock(chatId, stockName)) {
                String profitInfo = portfolioService.calculateProfit(chatId, stockName, currentPrice);
                return basicInfo + profitInfo;
            }

            return basicInfo;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 주식 정보 조회 실패: " + e.getMessage();
        }
    }

    /**
     * 여러 종목의 간단한 정보 조회
     */
    public String getMultipleStocks(String[] stockNames) {
        StringBuilder result = new StringBuilder("📈 주식 현황\n\n");

        for (String stockName : stockNames) {
            String info = getSimpleStockInfo(stockName.trim());
            result.append(info).append("\n");
        }

        return result.toString();
    }

    /**
     * 간단한 주식 정보 (한 줄)
     */
    private String getSimpleStockInfo(String stockName) {
        try {
            String code = getStockCode(stockName);
            if (code == null) return "❌ " + stockName + " - 종목을 찾을 수 없습니다";

            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + code;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode result = objectMapper.readTree(response).path("result")
                    .path("areas").get(0).path("datas").get(0);

            String currentPrice = result.path("nv").asText();
            String changeRate = result.path("cr").asText();
            String arrow = changeRate.startsWith("-") ? "🔻" : "🔺";

            return String.format("%s %s: %s원 (%s%%)",
                    arrow, stockName, formatNumber(currentPrice), changeRate);

        } catch (Exception e) {
            return "❌ " + stockName + " - 조회 실패";
        }
    }

    /**
     * 인기 검색 종목 TOP 10
     */
    public String getPopularStocks() {
        try {
            String url = "https://m.stock.naver.com/api/stocks/popular/DOMESTIC";
            String response = restTemplate.getForObject(url, String.class);

            JsonNode items = objectMapper.readTree(response);
            StringBuilder result = new StringBuilder("🔥 실시간 인기 검색 종목\n\n");

            int rank = 1;
            for (JsonNode item : items) {
                String name = item.path("stockName").asText();
                String code = item.path("stockCode").asText();
                String price = item.path("closePrice").asText();
                String changeRate = item.path("compareToPreviousClosePrice").asText();

                String arrow = changeRate.startsWith("-") ? "🔻" : "🔺";
                result.append(String.format("%d. %s %s: %s원 (%s%%)\n",
                        rank++, arrow, name, formatNumber(price), changeRate));

                if (rank > 10) break;
            }

            return result.toString();

        } catch (Exception e) {
            return "❌ 인기 종목 조회 실패: " + e.getMessage();
        }
    }

    /**
     * 코스피/코스닥 지수 조회
     */
    public String getMarketIndex() {
        try {
            StringBuilder result = new StringBuilder("📊 시장 지수\n\n");

            // 코스피
            String kospiUrl = "https://polling.finance.naver.com/api/realtime?query=SERVICE_INDEX:KOSPI";
            String kospiResponse = restTemplate.getForObject(kospiUrl, String.class);
            JsonNode kospi = objectMapper.readTree(kospiResponse).path("result")
                    .path("areas").get(0).path("datas").get(0);

            String kospiValue = kospi.path("nv").asText();
            String kospiChange = kospi.path("cv").asText();
            String kospiRate = kospi.path("cr").asText();
            String kospiArrow = kospiChange.startsWith("-") ? "🔻" : "🔺";

            result.append(String.format("KOSPI: %s %s (%s%%)\n",
                    kospiValue, kospiArrow + kospiChange, kospiRate));

            // 코스닥
            String kosdaqUrl = "https://polling.finance.naver.com/api/realtime?query=SERVICE_INDEX:KOSDAQ";
            String kosdaqResponse = restTemplate.getForObject(kosdaqUrl, String.class);
            JsonNode kosdaq = objectMapper.readTree(kosdaqResponse).path("result")
                    .path("areas").get(0).path("datas").get(0);

            String kosdaqValue = kosdaq.path("nv").asText();
            String kosdaqChange = kosdaq.path("cv").asText();
            String kosdaqRate = kosdaq.path("cr").asText();
            String kosdaqArrow = kosdaqChange.startsWith("-") ? "🔻" : "🔺";

            result.append(String.format("KOSDAQ: %s %s (%s%%)",
                    kosdaqValue, kosdaqArrow + kosdaqChange, kosdaqRate));

            return result.toString();

        } catch (Exception e) {
            return "❌ 시장 지수 조회 실패: " + e.getMessage();
        }
    }

    /**
     * 종목명으로 코드 찾기
     */
    private String getStockCode(String stockName) {
        String normalizedName = stockName.toLowerCase().trim();
        return stockCodes.get(normalizedName);
    }

    /**
     * 숫자 포맷팅 (천 단위 콤마)
     */
    private String formatNumber(String number) {
        try {
            long num = Long.parseLong(number.replace(",", ""));
            return String.format("%,d", num);
        } catch (Exception e) {
            return number;
        }
    }

    /**
     * 지원 종목 리스트 반환
     */
    public String getSupportedStocks() {
        StringBuilder result = new StringBuilder("📋 조회 가능한 주요 종목\n\n");
        stockCodes.keySet().forEach(name -> result.append("• ").append(name).append("\n"));
        return result.toString();
    }

    /**
     * 현재가를 숫자로 반환 (포트폴리오 계산용)
     */
    public double getCurrentPriceAsNumber(String stockName) {
        try {
            String code = getStockCode(stockName);
            if (code == null) return 0;

            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + code;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode result = objectMapper.readTree(response).path("result")
                    .path("areas").get(0).path("datas").get(0);

            String currentPrice = result.path("nv").asText();
            return Double.parseDouble(currentPrice.replace(",", ""));

        } catch (Exception e) {
            return 0;
        }
    }
}