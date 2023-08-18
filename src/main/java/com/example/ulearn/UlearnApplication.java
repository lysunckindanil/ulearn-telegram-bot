package com.example.ulearn;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@SpringBootApplication
public class UlearnApplication {

    public static void main(String[] args) {
        flask();
        SpringApplication.run(UlearnApplication.class, args);
    }

    static void flask() {
        File python = new File("src/main/resources/flaskUlearn/venv/bin/python");
        File app = new File("src/main/resources/flaskUlearn/app.py");
        try {
            Process process = new ProcessBuilder(python.getAbsolutePath(), app.getAbsolutePath()).start();
            Process process2 = new ProcessBuilder("ngrok", "http", "5000").start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
