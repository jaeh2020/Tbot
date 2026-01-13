package com.example.Tbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SystemDiagnosticService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StockService stockService;
    private final PortfolioService portfolioService;
    private final SearchResultCache searchResultCache;

    public SystemDiagnosticService(StockService stockService,
                                   PortfolioService portfolioService,
                                   SearchResultCache searchResultCache) {
        this.stockService = stockService;
        this.portfolioService = portfolioService;
        this.searchResultCache = searchResultCache;
    }

    /**
     * 전체 시스템 진단
     */
    public String runDiagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("🔧 시스템 진단 보고서\n");
        report.append("━━━━━━━━━━━━━━━━━━━━\n\n");

        int totalTests = 0;
        int passedTests = 0;

        // 1. 네이버 금융 API 테스트
        report.append("1️⃣ 네이버 금융 API (시세 조회)\n");
        DiagnosticResult priceApiTest = testPriceApi();
        report.append(priceApiTest.message).append("\n\n");
        totalTests++;
        if (priceApiTest.success) passedTests++;

        // 2. 네이버 인기 종목 API 테스트
        report.append("2️⃣ 네이버 인기 종목 API\n");
        DiagnosticResult popularApiTest = testPopularApi();
        report.append(popularApiTest.message).append("\n\n");
        totalTests++;
        if (popularApiTest.success) passedTests++;

        // 3. 네이버 시장 지수 API 테스트
        report.append("3️⃣ 네이버 시장 지수 API\n");
        DiagnosticResult indexApiTest = testMarketIndexApi();
        report.append(indexApiTest.message).append("\n\n");
        totalTests++;
        if (indexApiTest.success) passedTests++;

        // 4. StockService 테스트
        report.append("4️⃣ StockService (종목 검색)\n");
        DiagnosticResult stockServiceTest = testStockService();
        report.append(stockServiceTest.message).append("\n\n");
        totalTests++;
        if (stockServiceTest.success) passedTests++;

        // 5. SearchResultCache 테스트
        report.append("5️⃣ SearchResultCache (캐시 시스템)\n");
        DiagnosticResult cacheTest = testSearchCache();
        report.append(cacheTest.message).append("\n\n");
        totalTests++;
        if (cacheTest.success) passedTests++;

        // 6. PortfolioService 테스트
        report.append("6️⃣ PortfolioService (포트폴리오)\n");
        DiagnosticResult portfolioTest = testPortfolioService();
        report.append(portfolioTest.message).append("\n\n");
        totalTests++;
        if (portfolioTest.success) passedTests++;

        // 7. 종목 데이터베이스 테스트
        report.append("7️⃣ 종목 데이터베이스\n");
        DiagnosticResult databaseTest = testStockDatabase();
        report.append(databaseTest.message).append("\n\n");
        totalTests++;
        if (databaseTest.success) passedTests++;

        // 최종 결과
        report.append("━━━━━━━━━━━━━━━━━━━━\n");
        report.append("📊 최종 결과: ").append(passedTests).append("/").append(totalTests).append(" 통과\n\n");

        if (passedTests == totalTests) {
            report.append("✅ 모든 시스템이 정상 작동 중입니다!\n\n");
            report.append("💡 시스템 상태: 최적\n");
            report.append("💡 권장 사항: 없음");
        } else {
            report.append("⚠️ 일부 시스템에 문제가 있습니다.\n\n");

            // 실패한 항목별 권장 사항
            if (!popularApiTest.success) {
                report.append("📌 인기 종목 API 실패\n");
                report.append("   └ 영향: /popular 명령어에서 대체 데이터 표시\n");
                report.append("   └ 조치: 핵심 기능 아님, 정상 사용 가능\n\n");
            }
            if (!priceApiTest.success) {
                report.append("📌 시세 조회 API 실패\n");
                report.append("   └ 영향: 주식 가격 조회 불가 (심각)\n");
                report.append("   └ 조치: 네이버 금융 API 상태 확인 필요\n\n");
            }
            if (!indexApiTest.success) {
                report.append("📌 시장 지수 API 실패\n");
                report.append("   └ 영향: /market 명령어 사용 불가\n");
                report.append("   └ 조치: 네이버 금융 API 상태 확인\n\n");
            }
            if (!stockServiceTest.success) {
                report.append("📌 StockService 실패\n");
                report.append("   └ 영향: 검색 및 조회 기능 불가 (심각)\n");
                report.append("   └ 조치: 서비스 로직 점검 필요\n\n");
            }
            if (!cacheTest.success) {
                report.append("📌 SearchResultCache 실패\n");
                report.append("   └ 영향: 번호 선택 기능 사용 불가\n");
                report.append("   └ 조치: 캐시 서비스 재시작 권장\n\n");
            }
            if (!portfolioTest.success) {
                report.append("📌 PortfolioService 실패\n");
                report.append("   └ 영향: 포트폴리오 기능 사용 불가\n");
                report.append("   └ 조치: 데이터베이스 연결 확인\n\n");
            }
            if (!databaseTest.success) {
                report.append("📌 종목 데이터베이스 실패\n");
                report.append("   └ 영향: 종목 검색 제한적\n");
                report.append("   └ 조치: StockService 초기화 확인\n\n");
            }

            // 전체 시스템 상태
            double successRate = (double) passedTests / totalTests * 100;
            if (successRate >= 80) {
                report.append("💡 시스템 상태: 양호 (일부 기능 제한)\n");
                report.append("💡 서비스 가능: 예");
            } else if (successRate >= 60) {
                report.append("💡 시스템 상태: 주의 (주요 기능 영향)\n");
                report.append("💡 서비스 가능: 제한적");
            } else {
                report.append("💡 시스템 상태: 심각 (즉시 조치 필요)\n");
                report.append("💡 서비스 가능: 불가");
            }
        }

        return report.toString();
    }

    /**
     * 1. 네이버 금융 시세 조회 API 테스트
     */
    private DiagnosticResult testPriceApi() {
        try {
            String url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:005930";
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                return new DiagnosticResult(false, "❌ FAIL: API 응답 없음");
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("result").path("areas").get(0).path("datas").get(0);
            String stockName = result.path("nm").asText();
            String price = result.path("nv").asText();

            if (stockName.isEmpty() || price.isEmpty()) {
                return new DiagnosticResult(false, "❌ FAIL: 데이터 파싱 실패");
            }

            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ 테스트: 삼성전자 (005930)\n" +
                            "   └ 현재가: " + price + "원");

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 2. 인기 종목 API 테스트
     */
    private DiagnosticResult testPopularApi() {
        try {
            String url = "https://m.stock.naver.com/api/stocks/popular/DOMESTIC";
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                return new DiagnosticResult(false, "❌ FAIL: API 응답 없음");
            }

            JsonNode items = objectMapper.readTree(response);
            if (items.size() == 0) {
                return new DiagnosticResult(false, "❌ FAIL: 데이터 없음");
            }

            String firstStock = items.get(0).path("stockName").asText();

            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ 인기 종목 1위: " + firstStock);

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 3. 시장 지수 API 테스트
     */
    private DiagnosticResult testMarketIndexApi() {
        try {
            String kospiUrl = "https://polling.finance.naver.com/api/realtime?query=SERVICE_INDEX:KOSPI";
            String response = restTemplate.getForObject(kospiUrl, String.class);

            if (response == null || response.isEmpty()) {
                return new DiagnosticResult(false, "❌ FAIL: API 응답 없음");
            }

            JsonNode kospi = objectMapper.readTree(response).path("result")
                    .path("areas").get(0).path("datas").get(0);
            String kospiValue = kospi.path("nv").asText();

            if (kospiValue.isEmpty()) {
                return new DiagnosticResult(false, "❌ FAIL: 데이터 파싱 실패");
            }

            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ KOSPI: " + kospiValue);

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 4. StockService 테스트
     */
    private DiagnosticResult testStockService() {
        try {
            // 검색 테스트
            String searchResult = stockService.searchStocks("삼성", null);

            if (searchResult.contains("❌")) {
                return new DiagnosticResult(false,
                        "❌ FAIL: 검색 실패\n" +
                                "   └ " + searchResult.substring(0, Math.min(50, searchResult.length())));
            }

            // 조회 테스트
            String priceResult = stockService.getStockPrice("삼성전자");

            if (priceResult.contains("❌")) {
                return new DiagnosticResult(false,
                        "❌ FAIL: 조회 실패\n" +
                                "   └ " + priceResult.substring(0, Math.min(50, priceResult.length())));
            }

            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ 검색 및 조회 기능 정상");

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 5. SearchResultCache 테스트
     */
    private DiagnosticResult testSearchCache() {
        try {
            Long testChatId = 999999999L;

            // 캐시 저장 테스트
            java.util.List<SearchResultCache.SearchResult> testResults = new java.util.ArrayList<>();
            testResults.add(new SearchResultCache.SearchResult("테스트종목", "000000", "KOSPI"));

            searchResultCache.saveSearchResults(testChatId, testResults);

            // 캐시 조회 테스트
            SearchResultCache.SearchResult retrieved = searchResultCache.getResultByIndex(testChatId, 1);

            if (retrieved == null || !retrieved.name.equals("테스트종목")) {
                return new DiagnosticResult(false, "❌ FAIL: 캐시 저장/조회 실패");
            }

            // 캐시 삭제 테스트
            searchResultCache.clear(testChatId);

            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ 저장, 조회, 삭제 기능 정상");

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 6. PortfolioService 테스트
     */
    private DiagnosticResult testPortfolioService() {
        try {
            Long testChatId = 999999999L;

            // 포트폴리오가 정상 작동하는지만 확인 (실제 데이터 추가는 하지 않음)
            int stockCount = portfolioService.getStockCount(testChatId);

            // 서비스가 예외 없이 실행되면 성공
            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ PortfolioService 응답 정상");

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 7. 종목 데이터베이스 테스트
     */
    private DiagnosticResult testStockDatabase() {
        try {
            String listResult = stockService.getStockList();

            if (listResult.contains("❌")) {
                return new DiagnosticResult(false, "❌ FAIL: 종목 리스트 조회 실패");
            }

            // 주요 종목들이 있는지 확인
            boolean hasSamsung = listResult.contains("삼성전자");
            boolean hasHyundai = listResult.contains("현대차");
            boolean hasNaver = listResult.contains("네이버");

            if (!hasSamsung || !hasHyundai || !hasNaver) {
                return new DiagnosticResult(false,
                        "❌ FAIL: 주요 종목 누락\n" +
                                "   └ 삼성전자: " + (hasSamsung ? "O" : "X") +
                                " / 현대차: " + (hasHyundai ? "O" : "X") +
                                " / 네이버: " + (hasNaver ? "O" : "X"));
            }

            return new DiagnosticResult(true,
                    "✅ PASS: 정상 작동\n" +
                            "   └ 주요 종목 데이터 정상");

        } catch (Exception e) {
            return new DiagnosticResult(false,
                    "❌ FAIL: " + e.getClass().getSimpleName() + "\n" +
                            "   └ " + e.getMessage());
        }
    }

    /**
     * 진단 결과 클래스
     */
    private static class DiagnosticResult {
        boolean success;
        String message;

        DiagnosticResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}