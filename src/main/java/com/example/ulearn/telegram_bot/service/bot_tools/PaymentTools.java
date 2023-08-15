package com.example.ulearn.telegram_bot.service.bot_tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PaymentTools {


    public static JSONObject getUrlJson(String payment_description, int price, String url) {
        Map<String, String> jsonMapToGetPayment = new HashMap<>();
        jsonMapToGetPayment.put("bot_event", "get_url");
        jsonMapToGetPayment.put("description", payment_description);
        jsonMapToGetPayment.put("price", String.valueOf(price));
        JSONObject jsonToGetPayment = new JSONObject(jsonMapToGetPayment); //bot_event, description, price

        // sending json request
        return sendJson(jsonToGetPayment, url);
    }

    public static int checkPaymentStatusLoop(String id, String url) {
        Map<String, String> jsonMapToGetStatus = new HashMap<>();
        jsonMapToGetStatus.put("bot_event", "check_id");
        jsonMapToGetStatus.put("id", String.valueOf(id));
        JSONObject jsonToGetStatus = new JSONObject(jsonMapToGetStatus); //bot_event, description, price
        // loop of sending requests
        int limit = 600;
        for (int i = 0; i < limit; i++) {
            String response = "";
            try {
                Thread.sleep(3000);
                response = (String) sendJson(jsonToGetStatus, url).get("checking_result");
            } catch (InterruptedException e) {
                log.error("Thread sleep error");
            }

            if (response.equals("payment.succeeded")) {
                return 1;
            } else if (response.equals("payment.canceled")) {
                return -1;
            } else if (i == limit - 1) {
                return 0;
            }
        }

        return 1;
    }

    public static JSONObject sendJson(JSONObject jsonObject, String url) {

        String responseJSON;
        JSONObject jsonObjectResponse = null;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(url);
            StringEntity postingString = new StringEntity(jsonObject.toString());
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");

            HttpResponse response = httpClient.execute(post);
            responseJSON = EntityUtils.toString(response.getEntity());
            jsonObjectResponse = (JSONObject) new JSONParser().parse(responseJSON);
        } catch (IOException | ParseException exception) {
            log.error("Unable to send json request");
        }
        return jsonObjectResponse;
    }
}
