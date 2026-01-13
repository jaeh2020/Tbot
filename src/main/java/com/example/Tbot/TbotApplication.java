package com.example.Tbot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TbotApplication {
	// Test
	public static void main(String[] args) {
		SpringApplication.run(TbotApplication.class, args);
	}

	@PostConstruct
	public void started() {
		System.out.println("""
                
        ======================================
        🚀 Tbot 서버 정상 실행 중
        🤖 Telegram Bot 연결 완료
        ⏳ 종료하려면 Ctrl + C
        ======================================
                
        """);
	}
}