# 텔레그램 주식 봇 🤖📈

실시간 주식 정보를 텔레그램으로 조회할 수 있는 봇입니다.

## 주요 기능

### 1. 실시간 주식 정보 조회
- 개별 종목 현재가 조회
- 여러 종목 동시 조회
- 코스피/코스닥 지수 확인
- 인기 검색 종목 TOP 10

### 2. 실시간 알림 (선택 기능)
- 특정 종목 가격 변동 시 자동 알림
- 30초마다 가격 체크

### 3. CLI 명령어 실행
- 서버에서 직접 명령어 실행

## 명령어 사용법

### 📊 주식 조회
```
/stock 삼성전자
→ 삼성전자의 현재가, 등락률, 거래량 표시

/stocks 삼성전자,네이버,카카오
→ 여러 종목을 한 번에 조회

/market
→ 코스피, 코스닥 지수 확인

/popular
→ 실시간 인기 검색 종목 TOP 10

/list
→ 조회 가능한 주요 종목 리스트
```

### 🔔 실시간 알림 (선택 기능)
```
/alert 삼성전자
→ 삼성전자 가격 변동 시 자동 알림 설정

/unalert
→ 알림 해제

/mystatus
→ 현재 구독 중인 종목 확인
```

### 💻 시스템 명령어
```
/cli ls -la
→ 서버에서 CLI 명령어 실행

/help
→ 도움말 표시
```

## 설치 방법

### 1. 의존성 추가 (pom.xml)
```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Telegram Bots -->
    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots</artifactId>
        <version>6.8.0</version>
    </dependency>

    <!-- Jackson -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### 2. application.properties 설정
```properties
telegram.bot.token=YOUR_BOT_TOKEN
telegram.bot.username=YOUR_BOT_USERNAME
```

### 3. 파일 구조
```
src/main/java/com/example/Tbot/
├── TbotApplication.java           # 메인 애플리케이션
├── service/
│   ├── CliService.java            # CLI 명령 실행
│   ├── StockService.java          # 주식 정보 조회
│   └── StockAlertService.java     # 실시간 알림 (선택)
└── telegram/
    ├── TbotTelegram.java          # 텔레그램 봇
    └── CommandRouter.java         # 명령어 라우팅
```

## 조회 가능한 주요 종목

- 삼성전자
- SK하이닉스
- 네이버
- 카카오
- 현대차
- LG에너지솔루션
- 셀트리온
- 삼성바이오로직스
- 포스코홀딩스
- KB금융

*더 많은 종목을 추가하려면 `StockService.java`의 `stockCodes` Map에 추가하세요.*

## 사용 예시

### 예시 1: 삼성전자 현재가 조회
```
사용자: /stock 삼성전자

봇 응답:
📊 삼성전자 (005930)

현재가: 71,500원
🔺 전일대비: +1,500원 (+2.14%)
거래량: 12,345,678주

⏰ 실시간 조회
```

### 예시 2: 여러 종목 동시 조회
```
사용자: /stocks 삼성전자,네이버,카카오

봇 응답:
📈 주식 현황

🔺 삼성전자: 71,500원 (+2.14%)
🔻 네이버: 185,000원 (-1.5%)
🔺 카카오: 52,300원 (+0.8%)
```

### 예시 3: 시장 지수 확인
```
사용자: /market

봇 응답:
📊 시장 지수

KOSPI: 2,543.21 🔺+15.32 (+0.61%)
KOSDAQ: 745.89 🔻-3.21 (-0.43%)
```

### 예시 4: 실시간 알림 설정
```
사용자: /alert 삼성전자

봇 응답:
✅ '삼성전자' 실시간 알림이 설정되었습니다.
가격 변동 시 자동으로 알림을 받습니다.

(30초 후 가격이 변동되면 자동으로 알림)
봇: 🔔 삼성전자 가격 변동

📊 삼성전자 (005930)
현재가: 72,000원
🔺 전일대비: +2,000원 (+2.85%)
...
```

## API 소스

이 봇은 네이버 금융 API를 사용하여 실시간 주식 정보를 가져옵니다.

- 실시간 시세: `https://polling.finance.naver.com/api/realtime`
- 인기 종목: `https://m.stock.naver.com/api/stocks/popular/DOMESTIC`

## 주의사항

1. **실시간 알림 기능**은 선택사항입니다. 필요 없으면 `StockAlertService`를 제거하세요.
2. **API 호출 제한**: 과도한 호출 시 IP 차단될 수 있으니 주의하세요.
3. **CLI 명령어**: 보안상 위험할 수 있으니 프로덕션 환경에서는 제한을 두세요.

## 확장 아이디어

- [ ] 종목 검색 기능 추가
- [ ] 차트 이미지 생성
- [ ] 가격 알림 임계값 설정 (예: 5% 이상 변동 시만 알림)
- [ ] 포트폴리오 관리 기능
- [ ] 뉴스 스크래핑
- [ ] 미국 주식 지원

## 라이선스

MIT License
