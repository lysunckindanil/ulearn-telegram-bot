package com.example.ulearn.telegram_bot.service.bot_tools;

import com.example.ulearn.generator.Block;
import com.example.ulearn.telegram_bot.model.Payment;
import com.example.ulearn.telegram_bot.model.PaymentRepository;
import com.example.ulearn.telegram_bot.model.UserRepository;
import com.example.ulearn.telegram_bot.service.BotResources;
import com.vdurmont.emoji.EmojiParser;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.ulearn.telegram_bot.service.bot_tools.RegisterTools.registerUserAllBlocks;
import static com.example.ulearn.telegram_bot.service.bot_tools.RegisterTools.registerUserBlock;
import static com.example.ulearn.telegram_bot.service.bot_tools.SendMessageTools.sendMessage;

@Slf4j
@Component
public class PaymentTools {


    private final PaymentRepository paymentRepository;
    private final UserRepository users;
    private final BotResources source;

    @Autowired
    public PaymentTools(PaymentRepository paymentRepository, UserRepository users, BotResources source) {
        this.paymentRepository = paymentRepository;
        this.users = users;
        this.source = source;
    }

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
            if (response == null) {
            } else if (response.equals("payment.succeeded")) {
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


    public void restorePayments(TelegramLongPollingBot bot) {
        List<Payment> paymentList = (List<Payment>) paymentRepository.findAll();
        for (Payment payment : paymentList) {
            if (payment.getStatus().equals("process")) {
                Runnable task = () -> {
                    Long chatId = payment.getChatId();
                    int numberOfOrder = payment.getNumber_of_order();
                    int response = checkPaymentStatusLoop(payment.getId(), payment.getServer_url());
                    String text = null;
                    if (response == 1) {
                        if (payment.getBlocks() == null) {
                            users.save(registerUserAllBlocks(users.findById(chatId).get(), source.blocks));
                            text = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики всех блоков :sunglasses: \nЧтобы их получить, перейдите в /show");
                        } else {
                            Block block = source.blocks.stream().filter(x -> x.toString().equals(payment.getBlocks())).findFirst().get();
                            users.save(registerUserBlock(users.findById(chatId).get(), block));
                            text = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики " + block.inRussian() + "а :sunglasses: \nЧтобы их получить, перейдите в /show");
                        }
                    } else if (response == -1) {
                        text = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего платеж был отменен. Повторите покупку или напишите в поддержку!");
                    } else if (response == 0) {
                        text = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего у вас истек срок оплаты. Повторите покупку или напишите в поддержку!");
                    }
                    sendMessage(bot, chatId, text);
                    payment.setStatus("completed");
                    paymentRepository.save(payment);
                };
                Thread thread = new Thread(task);
                thread.start();
            }
        }
    }
}
