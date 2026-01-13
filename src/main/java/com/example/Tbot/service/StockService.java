package com.example.Tbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PortfolioService portfolioService;
    private final SearchResultCache searchResultCache;

    // 주요 종목 데이터베이스 (확장 가능)
    private final Map<String, StockInfo> stockDatabase = new HashMap<>() {{
        // 삼성 그룹
        put("삼성전자", new StockInfo("005930", "삼성전자", "KOSPI"));
        put("삼성바이오로직스", new StockInfo("207940", "삼성바이오로직스", "KOSPI"));
        put("삼성SDI", new StockInfo("006400", "삼성SDI", "KOSPI"));
        put("삼성물산", new StockInfo("028260", "삼성물산", "KOSPI"));
        put("삼성전기", new StockInfo("009150", "삼성전기", "KOSPI"));
        put("삼성생명", new StockInfo("032830", "삼성생명", "KOSPI"));
        put("삼성화재", new StockInfo("000810", "삼성화재", "KOSPI"));

        // SK 그룹
        put("SK하이닉스", new StockInfo("000660", "SK하이닉스", "KOSPI"));
        put("SK이노베이션", new StockInfo("096770", "SK이노베이션", "KOSPI"));
        put("SK텔레콤", new StockInfo("017670", "SK텔레콤", "KOSPI"));
        put("SK스퀘어", new StockInfo("402340", "SK스퀘어", "KOSPI"));
        put("SK바이오팜", new StockInfo("326030", "SK바이오팜", "KOSPI"));

        // 현대차 그룹
        put("현대차", new StockInfo("005380", "현대차", "KOSPI"));
        put("기아", new StockInfo("000270", "기아", "KOSPI"));
        put("현대모비스", new StockInfo("012330", "현대모비스", "KOSPI"));
        put("현대건설", new StockInfo("000720", "현대건설", "KOSPI"));
        put("HD현대일렉트릭", new StockInfo("267260", "HD현대일렉트릭", "KOSPI"));
        put("HD현대중공업", new StockInfo("329180", "HD현대중공업", "KOSPI"));

        // LG 그룹
        put("LG전자", new StockInfo("066570", "LG전자", "KOSPI"));
        put("LG화학", new StockInfo("051910", "LG화학", "KOSPI"));
        put("LG에너지솔루션", new StockInfo("373220", "LG에너지솔루션", "KOSPI"));
        put("LG생활건강", new StockInfo("051900", "LG생활건강", "KOSPI"));
        put("LG디스플레이", new StockInfo("034220", "LG디스플레이", "KOSPI"));

        // IT/인터넷
        put("네이버", new StockInfo("035420", "네이버", "KOSPI"));
        put("카카오", new StockInfo("035720", "카카오", "KOSPI"));
        put("카카오뱅크", new StockInfo("323410", "카카오뱅크", "KOSPI"));
        put("카카오페이", new StockInfo("377300", "카카오페이", "KOSPI"));
        put("엔씨소프트", new StockInfo("036570", "엔씨소프트", "KOSPI"));
        put("넷마블", new StockInfo("251270", "넷마블", "KOSPI"));
        put("크래프톤", new StockInfo("259960", "크래프톤", "KOSPI"));

        // 금융
        put("KB금융", new StockInfo("105560", "KB금융", "KOSPI"));
        put("신한지주", new StockInfo("055550", "신한지주", "KOSPI"));
        put("하나금융지주", new StockInfo("086790", "하나금융지주", "KOSPI"));
        put("우리금융지주", new StockInfo("316140", "우리금융지주", "KOSPI"));

        // 바이오/제약
        put("셀트리온", new StockInfo("068270", "셀트리온", "KOSPI"));
        put("삼성바이오로직스", new StockInfo("207940", "삼성바이오로직스", "KOSPI"));
        put("셀트리온헬스케어", new StockInfo("091990", "셀트리온헬스케어", "KOSDAQ"));

        // 2차전지/배터리
        put("에코프로", new StockInfo("086520", "에코프로", "KOSDAQ"));
        put("에코프로비엠", new StockInfo("247540", "에코프로비엠", "KOSDAQ"));
        put("포스코퓨처엠", new StockInfo("003670", "포스코퓨처엠", "KOSPI"));

        // 기타 주요 종목
        put("포스코홀딩스", new StockInfo("005490", "포스코홀딩스", "KOSPI"));
        put("NAVER", new StockInfo("035420", "네이버", "KOSPI"));
        put("삼전", new StockInfo("005930", "삼성전자", "KOSPI")); // 별칭
    }};

    static class StockInfo {
        String code;
        String name;
        String market;

        StockInfo(String code, String name, String market) {
            this.code = code;
            this.name = name;
            this.market = market;
        }
    }

    public StockService(PortfolioService portfolioService, SearchResultCache searchResultCache) {
        this.portfolioService = portfolioService;
        this.searchResultCache = searchResultCache;
    }

    /**
     * 주식 현재가 조회
     */
    public String getStockPrice(String stockName) {
        return getStockPrice(stockName, null);
    }

    public String getStockPrice(String stockName, Long chatId) {
        try {
            System.out.println("📊 주식 조회: " + stockName);

            // 종목 정보 검색
            StockInfo stockInfo = findStock(stockName);
            if (stockInfo == null) {
                return "❌ '" + stockName + "' 종목을 찾을 수 없습니다.\n\n" +
                        "💡 종목 검색하기:\n/search " + stockName + "\n\n" +
                        "등록된 종목을 보려면: /list";
            }

            // 네이버 금융 API 호출
            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + stockInfo.code;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("result").path("areas").get(0).path("datas").get(0);

            String name = result.path("nm").asText();
            String currentPriceStr = result.path("nv").asText();
            String changePrice = result.path("cv").asText();
            String changeRate = result.path("cr").asText();
            String volume = result.path("aq").asText();

            double currentPrice = Double.parseDouble(currentPriceStr.replace(",", ""));
            String arrow = changePrice.startsWith("-") ? "🔻" : "🔺";

            String basicInfo = String.format(
                    "📊 %s (%s)\n\n" +
                            "현재가: %s원\n" +
                            "%s 전일대비: %s원 (%s%%)\n" +
                            "거래량: %s주\n\n" +
                            "⏰ 실시간 조회",
                    name, stockInfo.code,
                    formatNumber(currentPriceStr),
                    arrow, changePrice, changeRate,
                    formatNumber(volume)
            );

            // 포트폴리오 정보 추가
            if (chatId != null && portfolioService.hasStock(chatId, name)) {
                String profitInfo = portfolioService.calculateProfit(chatId, name, currentPrice);
                return basicInfo + profitInfo;
            }

            return basicInfo;

        } catch (Exception e) {
            System.err.println("❌ 주식 정보 조회 실패: " + stockName);
            e.printStackTrace();
            return "❌ 주식 정보 조회 실패\n\n" +
                    "등록된 종목을 보려면: /list";
        }
    }

    /**
     * 종목 검색
     */
    public String searchStocks(String keyword, Long chatId) {
        System.out.println("🔍 searchStocks 호출: keyword = " + keyword + ", chatId = " + chatId);

        String lowerKeyword = keyword.toLowerCase();

        var matchedStocks = stockDatabase.entrySet().stream()
                .filter(entry ->
                        entry.getKey().toLowerCase().contains(lowerKeyword) ||
                                entry.getValue().name.toLowerCase().contains(lowerKeyword) ||
                                entry.getValue().code.contains(keyword))
                .collect(Collectors.toList());

        if (matchedStocks.isEmpty()) {
            return "❌ '" + keyword + "'에 대한 검색 결과가 없습니다.\n\n" +
                    "💡 등록된 종목을 보려면: /list\n\n" +
                    "종목 코드를 직접 입력하려면:\n/code [종목코드]\n예: /code 005930";
        }

        // 검색 결과를 캐시에 저장
        List<SearchResultCache.SearchResult> cacheResults = new ArrayList<>();

        StringBuilder result = new StringBuilder();
        result.append("🔍 '").append(keyword).append("' 검색 결과\n");
        result.append("━━━━━━━━━━━━━━━━━━━━\n\n");

        int count = 0;
        for (var entry : matchedStocks) {
            if (count >= 15) break;
            StockInfo info = entry.getValue();
            count++;

            // 캐시에 저장
            cacheResults.add(new SearchResultCache.SearchResult(info.name, info.code, info.market));

            result.append(String.format("%d. %s\n", count, info.name));
            result.append(String.format("   └ 코드: %s | %s\n\n", info.code, info.market));
        }

        result.append("━━━━━━━━━━━━━━━━━━━━\n");
        result.append("총 ").append(count).append("개 종목\n\n");
        result.append("💡 사용법:\n");
        result.append("• 번호로 조회: 1~").append(count).append(" 입력\n");
        result.append("• 이름으로 조회: /stock ").append(matchedStocks.get(0).getValue().name);

        // 결과를 캐시에 저장 (chatId가 있을 때만)
        if (chatId != null) {
            searchResultCache.saveSearchResults(chatId, cacheResults);
            result.append("\n\n⏱️ 검색 결과는 5분간 유지됩니다");
        }

        return result.toString();
    }

    /**
     * 검색 결과 인덱스로 가져오기
     */
    public SearchResultCache.SearchResult getSearchResultByIndex(Long chatId, int index) {
        return searchResultCache.getResultByIndex(chatId, index);
    }

    /**
     * 등록된 종목 리스트
     */
    public String getStockList() {
        StringBuilder result = new StringBuilder("📋 등록된 주요 종목\n\n");

        Map<String, java.util.List<StockInfo>> grouped = stockDatabase.values().stream()
                .distinct()
                .collect(Collectors.groupingBy(s -> {
                    if (s.name.contains("삼성")) return "삼성 그룹";
                    if (s.name.contains("SK")) return "SK 그룹";
                    if (s.name.contains("현대") || s.name.contains("HD") || s.name.contains("기아")) return "현대차 그룹";
                    if (s.name.contains("LG")) return "LG 그룹";
                    if (s.name.contains("네이버") || s.name.contains("카카오") || s.name.contains("엔씨") || s.name.contains("넷마블") || s.name.contains("크래프톤")) return "IT/게임";
                    if (s.name.contains("금융") || s.name.contains("KB") || s.name.contains("신한") || s.name.contains("하나") || s.name.contains("우리")) return "금융";
                    if (s.name.contains("셀트리온") || s.name.contains("바이오")) return "바이오/제약";
                    if (s.name.contains("에코프로") || s.name.contains("포스코퓨처엠")) return "2차전지";
                    return "기타";
                }));

        for (var group : grouped.entrySet()) {
            result.append("📌 ").append(group.getKey()).append("\n");
            group.getValue().forEach(stock ->
                    result.append("  • ").append(stock.name).append(" (").append(stock.code).append(")\n"));
            result.append("\n");
        }

        result.append("💡 검색: /search [키워드]\n");
        result.append("💡 조회: /stock [종목명]");

        return result.toString();
    }

    /**
     * 종목 코드로 직접 조회
     */
    public String getStockByCode(String code) {
        try {
            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + code;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("result").path("areas").get(0).path("datas").get(0);

            String name = result.path("nm").asText();
            String currentPriceStr = result.path("nv").asText();
            String changePrice = result.path("cv").asText();
            String changeRate = result.path("cr").asText();
            String volume = result.path("aq").asText();

            String arrow = changePrice.startsWith("-") ? "🔻" : "🔺";

            return String.format(
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

        } catch (Exception e) {
            return "❌ 종목 코드 '" + code + "' 조회 실패\n올바른 6자리 코드인지 확인하세요.";
        }
    }

    /**
     * 종목 찾기 (이름 또는 코드)
     */
    private StockInfo findStock(String query) {
        // 정확한 이름 매칭
        if (stockDatabase.containsKey(query)) {
            return stockDatabase.get(query);
        }

        // 부분 매칭
        String lowerQuery = query.toLowerCase();
        for (var entry : stockDatabase.entrySet()) {
            StockInfo info = entry.getValue();
            if (info.name.toLowerCase().contains(lowerQuery) ||
                    lowerQuery.contains(info.name.toLowerCase())) {
                return info;
            }
        }

        // 코드 매칭
        for (StockInfo info : stockDatabase.values()) {
            if (info.code.equals(query)) {
                return info;
            }
        }

        return null;
    }

    /**
     * 여러 종목 조회
     */
    public String getMultipleStocks(String[] stockNames) {
        StringBuilder result = new StringBuilder("📈 주식 현황\n\n");

        for (String stockName : stockNames) {
            String info = getSimpleStockInfo(stockName.trim());
            result.append(info).append("\n");
        }

        return result.toString();
    }

    private String getSimpleStockInfo(String stockName) {
        try {
            StockInfo stockInfo = findStock(stockName);
            if (stockInfo == null) return "❌ " + stockName + " - 종목을 찾을 수 없습니다";

            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + stockInfo.code;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode result = objectMapper.readTree(response).path("result")
                    .path("areas").get(0).path("datas").get(0);

            String currentPrice = result.path("nv").asText();
            String changeRate = result.path("cr").asText();
            String arrow = changeRate.startsWith("-") ? "🔻" : "🔺";

            return String.format("%s %s: %s원 (%s%%)",
                    arrow, stockInfo.name, formatNumber(currentPrice), changeRate);

        } catch (Exception e) {
            return "❌ " + stockName + " - 조회 실패";
        }
    }

    /**
     * 인기 검색 종목
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
                String price = item.path("closePrice").asText();
                String changeRate = item.path("compareToPreviousClosePrice").asText();

                String arrow = changeRate.startsWith("-") ? "🔻" : "🔺";
                result.append(String.format("%d. %s %s: %s원 (%s%%)\n",
                        rank++, arrow, name, formatNumber(price), changeRate));

                if (rank > 10) break;
            }

            return result.toString();

        } catch (Exception e) {
            System.err.println("❌ 인기 종목 API 오류: " + e.getMessage());

            // API 실패 시 등록된 주요 종목으로 대체
            return "📋 주요 종목 TOP 10\n\n" +
                    "⚠️ 실시간 인기 종목 API가 일시적으로 사용 불가합니다.\n" +
                    "대신 등록된 주요 종목을 표시합니다.\n\n" +
                    "1. 삼성전자 (005930)\n" +
                    "2. SK하이닉스 (000660)\n" +
                    "3. 현대차 (005380)\n" +
                    "4. 네이버 (035420)\n" +
                    "5. 카카오 (035720)\n" +
                    "6. LG에너지솔루션 (373220)\n" +
                    "7. 삼성바이오로직스 (207940)\n" +
                    "8. 셀트리온 (068270)\n" +
                    "9. 기아 (000270)\n" +
                    "10. KB금융 (105560)\n\n" +
                    "💡 개별 종목 조회: /stock [종목명]";
        }
    }

    /**
     * 시장 지수
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
            return "❌ 시장 지수 조회 실패";
        }
    }

    private String formatNumber(String number) {
        try {
            long num = Long.parseLong(number.replace(",", ""));
            return String.format("%,d", num);
        } catch (Exception e) {
            return number;
        }
    }

    public double getCurrentPriceAsNumber(String stockName) {
        try {
            StockInfo stockInfo = findStock(stockName);
            if (stockInfo == null) return 0;

            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:" + stockInfo.code;
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