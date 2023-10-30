package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.TelegramBot;
import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.CodeUnit;
import com.example.ulearn.telegram_bot.model.Payment;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.repo.PaymentRepository;
import com.example.ulearn.telegram_bot.model.repo.UserRepository;
import com.example.ulearn.telegram_bot.exceptions.BlockRegistrationException;
import com.vdurmont.emoji.EmojiParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.util.*;

import static com.example.ulearn.telegram_bot.source.Resources.getOneButtonLinkKeyboardMarkup;
import static com.example.ulearn.telegram_bot.tools.SendMessageTools.sendMessage;
import static com.example.ulearn.telegram_bot.tools.SerializationTools.deserializeFromString;
import static com.example.ulearn.telegram_bot.tools.SerializationTools.serializeToString;
import static com.example.ulearn.telegram_bot.tools.ServerTools.sendJson;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "DuplicatedCode", "SpringPropertySource"})
@Slf4j
@Service
@RequiredArgsConstructor
@PropertySource("application.properties")
@PropertySource("telegram.properties")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    @Value("${price.one_block}")
    public int PRICE_ONE_BLOCK;
    @Value("${payment.limit}")
    public int LIMIT;
    @Value("${server.url}")
    private String SERVER_URL;
    @Value("${price.all_blocks}")
    private int PRICE_ALL_BLOCKS;

    private static int checkPaymentStatusLoop(String id, String url, int limit) {
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

    private static JSONObject getUrlJson(String payment_description, int price, String url) {
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

    public void proceedPayment(TelegramBot bot, long chatId, Block block, Message message) {
        Runnable task = () -> {
            // order description
            final int price = block == null ? PRICE_ALL_BLOCKS : PRICE_ONE_BLOCK;
            final int numberOfOrder = new Random().nextInt(10000, 100000);
            final String payment_description = "Order " + numberOfOrder + "\n" + "User: " + chatId + "\n" + "Block: " + (block == null ? "all blocks" : block.inEnglish()) + "\n" + "Price: " + price;
            // create request to get url and payment code
            // creating json request
            JSONObject urlJson = getUrlJson(payment_description, price, SERVER_URL);
            final String id = (String) urlJson.get("payment_id");
            final String url = (String) urlJson.get("payment_url");
            // description for user aka message text
            final String description = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " создан :white_check_mark:") + "\n" + (block != null ? getOneBlockDescriptionPaymentText(block) : getAllBlocksDescriptionPaymentText());

            // sends information about order to user, with link to pay
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setText(description);
            editMessageText.setReplyMarkup(getOneButtonLinkKeyboardMarkup("Оплатить", url));
            sendMessage(bot, editMessageText, message);
            log.info("Created order " + numberOfOrder + " chatId " + chatId + " payment_id " + id);

            // writes payment info to database
            // process of checking whether payment paid or not
            Payment payment = storePayment(id, chatId, block, numberOfOrder, message);

            // gets response whether user has paid or not
            // changes editMessageText due to payment description
            String textToUserResponse;
            try {
                textToUserResponse = handleResponse(payment, block);
                payment.setStatus("completed");
            } catch (BlockRegistrationException e) {
                payment.setStatus("error");
                log.error("Payment error: " + payment);
                textToUserResponse = "Просим прощения, произошла непредвиденная ошибка при покупке блоков. Обратитесь, пожалуйста, в поддержку /help!";
            }

            // deletes link in replyMarkup in order it wouldn't be available to pay after out of time
            // sets text of the response
            editMessageText.setText(textToUserResponse);
            editMessageText.setReplyMarkup(null);
            sendMessage(bot, editMessageText, message);

            // status completed if all's gone right no matter was it paid, cancelled or out of time
            paymentRepository.save(payment);
        };
        Thread thread = new Thread(task);
        thread.start();
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
                    EditMessageText editMessageText = new EditMessageText();
                    Block block;
                    if (!payment.getBlocks().isEmpty())
                        block = userService.getBlocks().stream().filter(x -> x.inEnglish().equals(payment.getBlocks())).findFirst().get();
                    else block = null;
                    String textToUserResponse;
                    try {
                        textToUserResponse = handleResponse(payment, block);
                        payment.setStatus("completed with restore");
                    } catch (BlockRegistrationException e) {
                        payment.setStatus("error");
                        log.error("Payment error: " + payment);
                        textToUserResponse = "Просим прощение, произошла непредвиденная ошибка при покупке блоков. Обратитесь, пожалуйста, в поддержку /help!";
                    }
                    // sets text of the response
                    editMessageText.setText(textToUserResponse);
                    editMessageText.setReplyMarkup(null);
                    sendMessage(bot, editMessageText, message);
                    paymentRepository.save(payment);
                };
                Thread thread = new Thread(task);
                thread.start();
            }
        }
    }

    private String handleResponse(Payment payment, Block block) throws BlockRegistrationException {
        String id = payment.getId();
        int numberOfOrder = payment.getNumber_of_order();
        long chatId = payment.getChatId();

        int response = checkPaymentStatusLoop(id, SERVER_URL, LIMIT);
        if (response == 1) {
            // if successful it registers blocks to user
            User user = userRepository.findById(payment.getChatId()).get();
            if (block == null) {
                userService.registerBlocks(user, userService.getBlocks());
                log.info("ChatId " + chatId + " bought block payment_id " + id);
                return EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики всех блоков :sunglasses: \nЧтобы их получить, перейдите в /show");

            } else {
                userService.registerBlocks(user, block);
                log.info("ChatId " + chatId + " bought blocks payment_id " + id);
                return EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики " + block.inRussian() + "а :sunglasses: \nЧтобы их получить, перейдите в /show");
            }
        } else if (response == -1) {
            log.info("ChatId " + chatId + " cancelled payment payment_id " + id);
            return EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего платеж был отменен. Повторите покупку или напишите в поддержку!");
        } else if (response == 0) {
            log.info("ChatId " + chatId + " is out of time payment_id " + id);
            return EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего у вас истек срок оплаты. Повторите покупку или напишите в поддержку!");
        }
        return "Unknown response";
    }

    private Payment storePayment(String id, long chatId, Block block, int numberOfOrder, Message message) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setChatId(chatId);
        payment.setStatus("process");
        payment.setBlocks(block == null ? null : block.inEnglish());
        payment.setServer_url(SERVER_URL);
        payment.setNumber_of_order(numberOfOrder);
        payment.setDate(new Date(System.currentTimeMillis()));
        // serializes message in order to have an ability to delete link if bot is broken, and we need somehow to restore payment (restorePayment method)
        try {
            payment.setMessage(serializeToString(message));
        } catch (IOException e) {
            log.error("Error to serialize message");
        }
        paymentRepository.save(payment);
        return payment;
    }


    // static data

    private String getOneBlockDescriptionPaymentText(Block block) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("После покупки " + block.inRussian().replace(" ", "го ") + "a вы получите:");
        for (CodeUnit codeUnit : block.getCodeUnits()) {
            if (codeUnit.isFabricate())
                joiner.add(EmojiParser.parseToUnicode(":small_blue_diamond:Практика " + codeUnit.getName()));
            else joiner.add(EmojiParser.parseToUnicode(":small_blue_diamond:Задание " + codeUnit.getName()));
        }

        joiner.add(EmojiParser.parseToUnicode(":small_blue_diamond:Ответы на контрольные вопросы"));
        joiner.add("Цена " + PRICE_ONE_BLOCK + " рублей");
        return joiner.toString();
    }

    private String getAllBlocksDescriptionPaymentText() {
        return "Вам будут доступны все блоки!\nЦена " + PRICE_ALL_BLOCKS + " рублей";
    }

    public String getChoosingTwoOptionsText() {
        return EmojiParser.parseToUnicode(String.format("На данный момент цена всех блоков - %d, одного - %d рублей :innocent:\nСколько хотите купить?", PRICE_ALL_BLOCKS, PRICE_ONE_BLOCK));
    }
}
