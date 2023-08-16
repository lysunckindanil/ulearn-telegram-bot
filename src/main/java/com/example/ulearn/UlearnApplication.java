package com.example.ulearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class UlearnApplication {

    public static void main(String[] args) {
        flask();
        SpringApplication.run(UlearnApplication.class, args);
    }

    static void flask() {
        String python = "/home/danila/IdeaProjects/telegram-bot/src/main/resources/flaskUlearn/venv/bin/python";
        String app = "/home/danila/IdeaProjects/telegram-bot/src/main/resources/flaskUlearn/app.py";
        try {
            Process process = new ProcessBuilder(python, app).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
