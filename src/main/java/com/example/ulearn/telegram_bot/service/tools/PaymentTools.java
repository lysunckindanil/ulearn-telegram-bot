package com.example.ulearn.telegram_bot.service.tools;

import com.example.ulearn.telegram_bot.model.*;
import com.example.ulearn.telegram_bot.service.BlockService;
import com.example.ulearn.telegram_bot.service.TelegramBot;
import com.vdurmont.emoji.EmojiParser;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.ulearn.telegram_bot.service.tools.RegisterTools.registerBlocks;
import static com.example.ulearn.telegram_bot.service.tools.SendMessageTools.sendMessage;
import static com.example.ulearn.telegram_bot.service.tools.SerializationTools.deserializeFromString;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTools {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BlockService blockService;

    public static JSONObject getUrlJson(String payment_description, int price, String url) {
        // creates json request to get link for payment
        Map<String, String> jsonMapToGetPayment = new HashMap<>();
        jsonMapToGetPayment.put("bot_event", "get_url");
        jsonMapToGetPayment.put("description", payment_description);
        jsonMapToGetPayment.put("price", String.valueOf(price));
        JSONObject jsonToGetPayment = new JSONObject(jsonMapToGetPayment);
        // json arguments: bot_event, description, price

        // sending json request
        return sendJson(jsonToGetPayment, url);
    }

    public static int checkPaymentStatusLoop(String id, String url, int limit) {
        Map<String, String> jsonMapToGetStatus = new HashMap<>();
        jsonMapToGetStatus.put("bot_event", "check_id");
        jsonMapToGetStatus.put("id", String.valueOf(id));
        JSONObject jsonToGetStatus = new JSONObject(jsonMapToGetStatus);
        // json arguments: bot_event, id
        // loop of sending requests
        for (int i = 0; i < limit; i++) {
            try {
                Thread.sleep(1000);
                String response = (String) sendJson(jsonToGetStatus, url).get("checking_result");
                if (response != null) {
                    if (response.equals("payment.succeeded")) {
                        return 1;
                    } else if (response.equals("payment.canceled")) {
                        return -1;
                    } else if (i == limit - 1) {
                        return 0;
                    }
                }
            } catch (InterruptedException e) {
                log.error("Thread sleep error");
            }
        }
        return 0;
    }

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

    public void restorePayments(TelegramBot bot) {
        // this method required if something went wrong with bot spring application
        // bot always saves payment to database with status "process" the moment link with payment url was sent to user
        // and if bot had been interrupted and closed, and client paid after that, it's not a problem actually
        // after restarting bot application it will be handled as well in this method

        // TelegramBot.buy() all additional commends are in there!
        List<Payment> paymentList = (List<Payment>) paymentRepository.findAll();
        for (Payment payment : paymentList) {
            // if payment's still in "process" stage it means that something went wrong, and it should be handled here
            if (payment.getStatus().equals("process")) {
                Runnable task = () -> {
                    // deserialize message in order to delete link in ReplyMarkup after response
                    Message message;
                    try {
                        message = (Message) deserializeFromString(payment.getMessage());
                    } catch (IOException | ClassNotFoundException e) {
                        log.error("Unable to deserialize message");
                        return;
                    }

                    // then anything's similar to Telegram.buy() method, but changed logs a little bit
                    Long chatId = message.getChatId();
                    int numberOfOrder = payment.getNumber_of_order();
                    String id = payment.getId();
                    int response = checkPaymentStatusLoop(id, payment.getServer_url(), 600);

                    EditMessageText editMessageText = new EditMessageText();
                    if (response == 1) {
                        if (payment.getBlocks() == null) {
                            User user = userRepository.findById(chatId).get();
                            RegisterTools.registerBlocks(user, blockService.getBlocks());
                            userRepository.save(user);
                            editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики всех блоков :sunglasses: \nЧтобы их получить, перейдите в /show"));
                        } else {
                            Block block = blockService.getBlocks().stream().filter(x -> x.inEnglish().equals(payment.getBlocks())).findFirst().get();
                            User user = userRepository.findById(chatId).get();
                            registerBlocks(user, block);
                            userRepository.save(user);
                            editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики " + block.inRussian() + "а :sunglasses: \nЧтобы их получить, перейдите в /show"));
                        }
                        log.info("Restore chatId " + chatId + " bought block/blocks payment_id " + id);
                    } else if (response == -1) {
                        editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего платеж был отменен. Повторите покупку или напишите в поддержку!"));
                        log.info("Restore chatId " + chatId + " cancelled payment payment_id " + id);
                    } else if (response == 0) {
                        editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего у вас истек срок оплаты. Повторите покупку или напишите в поддержку!"));
                        log.info("Restore chatId " + chatId + " is out of time payment_id " + id);
                    }

                    editMessageText.setReplyMarkup(null);
                    sendMessage(bot, editMessageText, message);
                    payment.setStatus("completed with restore");
                    paymentRepository.save(payment);
                };
                Thread thread = new Thread(task);
                thread.start();
            }
        }
    }

}
