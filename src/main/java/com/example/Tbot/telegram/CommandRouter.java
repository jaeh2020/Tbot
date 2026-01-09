package com.example.Tbot.telegram;

import com.example.Tbot.service.CliService;
import org.springframework.stereotype.Component;

@Component
public class CommandRouter {

    private final CliService cliService;

    public CommandRouter(CliService cliService) {
        this.cliService = cliService;
    }

    public String route(String message) {

        if (message.startsWith("/cli ")) {
            String command = message.substring(5);
            cliService.executeAsync(command);
            return "CLI 실행 시작: " + command;
        }

        if (message.equals("/help")) {
            return """
                    사용 가능한 명령어:
                    /cli <command>  - 서버 CLI 실행
                    /help          - 도움말
                    """;
        }

        return "알 수 없는 명령어입니다. /help 입력";
    }
}