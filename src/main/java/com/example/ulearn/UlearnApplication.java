package com.example.ulearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class UlearnApplication {

    public static void main(String[] args) {
        flaskLocal();
        SpringApplication.run(UlearnApplication.class, args);
    }

    static void flaskServer() {
        String dir = new File("").getAbsolutePath();
        File python = new File(dir + "/BOOT-INF/classes/flaskUlearn/venv/bin/python");
        File app = new File(dir + "/BOOT-INF/classes/flaskUlearn/app.py");

        try {
            Process process0 = new ProcessBuilder("jar", "xf", "ulearn-0.0.1-SNAPSHOT.jar", "/BOOT-INF/classes/flaskUlearn").start();
            Thread.sleep(500);
            Process process1 = new ProcessBuilder("chmod", "+x", python.getPath()).start();
            Thread.sleep(500);
            Process process2 = new ProcessBuilder(python.getPath(), app.getPath()).start();
            Process process3 = new ProcessBuilder("ngrok", "http", "5000").start();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void flaskLocal() {
        File python = new File("src/main/resources/flaskUlearn/venv/bin/python");
        File app = new File("src/main/resources/flaskUlearn/app.py");
        try {
            Process process0 = new ProcessBuilder(python.getAbsolutePath(), app.getAbsolutePath()).start();
            Process process1 = new ProcessBuilder("ngrok", "http", "5000").start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
