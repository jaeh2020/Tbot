package com.example.Tbot.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SearchResultCache {

    // chatId별 검색 결과 저장
    private final Map<Long, List<SearchResult>> searchCache = new ConcurrentHashMap<>();

    // 검색 결과 만료 시간 (5분)
    private final Map<Long, Long> cacheTimestamp = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5분

    public static class SearchResult {
        public String name;
        public String code;
        public String market;

        public SearchResult(String name, String code, String market) {
            this.name = name;
            this.code = code;
            this.market = market;
        }
    }

    /**
     * 검색 결과 저장
     */
    public void saveSearchResults(Long chatId, List<SearchResult> results) {
        searchCache.put(chatId, results);
        cacheTimestamp.put(chatId, System.currentTimeMillis());
        System.out.println("💾 검색 결과 저장: chatId=" + chatId + ", 개수=" + results.size());
    }

    /**
     * 번호로 검색 결과 가져오기
     */
    public SearchResult getResultByIndex(Long chatId, int index) {
        // 만료 확인
        if (!isValid(chatId)) {
            System.out.println("⚠️ 검색 결과 만료: chatId=" + chatId);
            return null;
        }

        List<SearchResult> results = searchCache.get(chatId);
        if (results == null || index < 1 || index > results.size()) {
            System.out.println("⚠️ 잘못된 인덱스: chatId=" + chatId + ", index=" + index);
            return null;
        }

        System.out.println("✅ 검색 결과 반환: chatId=" + chatId + ", index=" + index);
        return results.get(index - 1); // 1-based index
    }

    /**
     * 검색 결과가 유효한지 확인
     */
    public boolean hasValidResults(Long chatId) {
        return isValid(chatId) && searchCache.containsKey(chatId);
    }

    /**
     * 검색 결과 개수 반환
     */
    public int getResultCount(Long chatId) {
        if (!isValid(chatId)) return 0;
        List<SearchResult> results = searchCache.get(chatId);
        return results != null ? results.size() : 0;
    }

    /**
     * 캐시 만료 확인
     */
    private boolean isValid(Long chatId) {
        Long timestamp = cacheTimestamp.get(chatId);
        if (timestamp == null) return false;

        long elapsed = System.currentTimeMillis() - timestamp;
        return elapsed < CACHE_DURATION;
    }

    /**
     * 검색 결과 삭제
     */
    public void clear(Long chatId) {
        searchCache.remove(chatId);
        cacheTimestamp.remove(chatId);
        System.out.println("🗑️ 검색 결과 삭제: chatId=" + chatId);
    }

    /**
     * 만료된 캐시 정리 (주기적으로 호출)
     */
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        List<Long> expiredKeys = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : cacheTimestamp.entrySet()) {
            if (now - entry.getValue() >= CACHE_DURATION) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (Long key : expiredKeys) {
            searchCache.remove(key);
            cacheTimestamp.remove(key);
        }

        if (!expiredKeys.isEmpty()) {
            System.out.println("🧹 만료된 캐시 정리: " + expiredKeys.size() + "개");
        }
    }
}