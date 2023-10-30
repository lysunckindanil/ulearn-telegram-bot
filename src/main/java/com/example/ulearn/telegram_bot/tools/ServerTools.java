package com.example.ulearn.telegram_bot.tools;

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

@Slf4j
public class ServerTools {
    public static JSONObject sendJson(JSONObject jsonObject, String url) {
        // sends json to passed url
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
