package com.example.Tbot.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MenuSessionService {

    // chatId별 현재 메뉴 상태 저장
    private final Map<Long, MenuState> menuStates = new ConcurrentHashMap<>();

    // 세션 만료 시간 (10분)
    private final Map<Long, Long> sessionTimestamp = new ConcurrentHashMap<>();
    private static final long SESSION_DURATION = 10 * 60 * 1000;

    public enum MenuLevel {
        MAIN,           // 메인 메뉴
        STOCK_SEARCH,   // 1. 종목 검색
        MARKET_INFO,    // 2. 시장 정보
        PORTFOLIO,      // 3. 포트폴리오
        ALERT_MONITOR,  // 4. 알림/모니터링
        HELP            // 도움말
    }

    public static class MenuState {
        public MenuLevel level;
        public String data; // 추가 데이터 (필요시)

        public MenuState(MenuLevel level) {
            this.level = level;
            this.data = null;
        }

        public MenuState(MenuLevel level, String data) {
            this.level = level;
            this.data = data;
        }
    }

    /**
     * 메뉴 상태 설정
     */
    public void setMenuState(Long chatId, MenuLevel level) {
        setMenuState(chatId, level, null);
    }

    public void setMenuState(Long chatId, MenuLevel level, String data) {
        menuStates.put(chatId, new MenuState(level, data));
        sessionTimestamp.put(chatId, System.currentTimeMillis());
        System.out.println("📂 메뉴 상태 설정: chatId=" + chatId + ", level=" + level);
    }

    /**
     * 현재 메뉴 상태 가져오기
     */
    public MenuState getMenuState(Long chatId) {
        if (!isValid(chatId)) {
            return new MenuState(MenuLevel.MAIN); // 만료되면 메인으로
        }

        MenuState state = menuStates.get(chatId);
        return state != null ? state : new MenuState(MenuLevel.MAIN);
    }

    /**
     * 메인 메뉴로 돌아가기
     */
    public void goToMain(Long chatId) {
        setMenuState(chatId, MenuLevel.MAIN);
    }

    /**
     * 세션 삭제
     */
    public void clear(Long chatId) {
        menuStates.remove(chatId);
        sessionTimestamp.remove(chatId);
    }

    /**
     * 세션 유효성 확인
     */
    private boolean isValid(Long chatId) {
        Long timestamp = sessionTimestamp.get(chatId);
        if (timestamp == null) return false;

        long elapsed = System.currentTimeMillis() - timestamp;
        return elapsed < SESSION_DURATION;
    }

    /**
     * 세션 갱신
     */
    public void refreshSession(Long chatId) {
        if (menuStates.containsKey(chatId)) {
            sessionTimestamp.put(chatId, System.currentTimeMillis());
        }
    }
}