package com.example.ulearn;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Properties;

import static com.example.ulearn.telegram_bot.service.PaymentService.sendJson;

@SpringBootApplication
@Slf4j
public class UlearnApplication {

    public static void main(String[] args) {
        if (checkServer()) {
            SpringApplication.run(UlearnApplication.class, args);
        } else {
            log.error("Server is not started!");
        }
    }

    static boolean checkServer() {
        Properties prop = new Properties();
        String server_url;
        try {
            prop.load(UlearnApplication.class.getClassLoader().getResourceAsStream("application.properties"));
            server_url = prop.getProperty("server.url");
        } catch (IOException e) {
            log.error("Unable to load server url from properties");
            return false;
        }
        return sendJson(new JSONObject(), server_url) != null;
    }

}
