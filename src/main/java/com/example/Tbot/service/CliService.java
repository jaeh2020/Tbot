package com.example.Tbot.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class CliService {

    public String execute(String command) {
        StringBuilder result = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            process.waitFor();

        } catch (Exception e) {
            return "CLI 실행 오류: " + e.getMessage();
        }

        return result.length() == 0 ? "출력 없음" : result.toString();
    }

    public void executeAsync(String command) {
        new Thread(() -> executeInternal(command)).start();
    }

    private void executeInternal(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null && lineCount < 20) {
                System.out.println("[CLI] " + line);
                lineCount++;
            }

            process.destroy(); // ❗ 강제 종료 (중요)

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}