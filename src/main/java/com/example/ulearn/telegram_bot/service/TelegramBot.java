package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.generator.Block;
import com.example.ulearn.generator.CodeUnit;
import com.example.ulearn.telegram_bot.config.BotConfig;
import com.example.ulearn.telegram_bot.model.Payment;
import com.example.ulearn.telegram_bot.model.PaymentRepository;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.UserRepository;
import com.example.ulearn.telegram_bot.service.bot_tools.PaymentTools;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;

import static com.example.ulearn.telegram_bot.service.bot_tools.PaymentTools.checkPaymentStatusLoop;
import static com.example.ulearn.telegram_bot.service.bot_tools.PaymentTools.getUrlJson;
import static com.example.ulearn.telegram_bot.service.bot_tools.QuestionsTools.sendQuestions;
import static com.example.ulearn.telegram_bot.service.bot_tools.RegisterTools.registerUserAllBlocks;
import static com.example.ulearn.telegram_bot.service.bot_tools.RegisterTools.registerUserBlock;
import static com.example.ulearn.telegram_bot.service.bot_tools.SendMessageTools.sendMessage;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final UserRepository users;
    private final BotResources source;
    private final PaymentRepository paymentRepository;

    @Autowired
    public TelegramBot(BotConfig config, UserRepository userRepository, BotResources source, PaymentRepository paymentRepository, PaymentTools paymentTools) {
        this.config = config;
        this.users = userRepository;
        this.source = source;
        this.paymentRepository = paymentRepository;
        paymentTools.restorePayments(this);
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/show", "Показать купленные блоки"));
        listOfCommands.add(new BotCommand("/buy", "Купить блоки"));
        listOfCommands.add(new BotCommand("/help", "Помощь"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException exception) {
            log.error("Error setting bot command list " + exception.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        //todo change text
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            log.info("Message received : " + messageText + " from " + update.getMessage().getChat().getUserName());
            if (chatId == source.ADMIN_CHATID) {
                if (!adminCommandReceived(messageText)) {
                    switch (messageText) {
                        case "/start" -> startCommandReceived(update.getMessage());
                        case "/show" -> {
                            sortBlocks(update.getMessage()); //sorts before show blocks to user
                            showUserFiles(chatId);
                        }
                        case "/buy" -> {
                            if (users.findById(chatId).isPresent() && users.findById(chatId).get().getBlocks().split(",").length == source.blocks.size()) {
                                String text = EmojiParser.parseToUnicode("У вас уже куплены все блоки, вы большой молодец :blush:");
                                sendMessage(this, chatId, text); //todo text
                            } else sendMessage(this, chatId, source.getChoosingTwoOptionsText(), source.getBuyMenu());
                        }
                        case "/help" -> sendMessage(this, chatId, source.getHelpText());
                        default -> {
                            String text = EmojiParser.parseToUnicode("Я вас не понимаю, если что-то не понятно, нажимайте /help :relieved:");
                            sendMessage(this, chatId, text);
                        }//todo text
                    }
                }
            }
        } else if (update.hasCallbackQuery()) ifCallbackQueryGot(update.getCallbackQuery());
    }

    private void ifCallbackQueryGot(CallbackQuery callbackQuery) {
        //todo change text
        String callBackData = callbackQuery.getData();
        Message message = callbackQuery.getMessage();
        long chatId = message.getChatId();
        EditMessageText editMessageText = new EditMessageText();


        if (callBackData.equals(source.BUY_ALL_STRING)) {
            buy(chatId, null, message);
        } else if (callBackData.equals(source.BUY_ONE_STRING)) {
            editMessageText.setText("Теперь, пожалуйста, выберите блок, который хотите купить");
            editMessageText.setReplyMarkup(source.getBlockChoosingMenu());
            sendMessage(this, editMessageText, message);
        } else if (callBackData.startsWith("block")) {
            if (users.findById(chatId).isPresent() && Arrays.asList(users.findById(chatId).get().getBlocks().split(",")).contains(callBackData)) {
                editMessageText.setText(EmojiParser.parseToUnicode("У вас уже куплен этот блок :face_with_monocle:")); //todo text
                sendMessage(this, editMessageText, message);
            } else {
                if (users.existsById(chatId)) {
                    Block block = source.blocks.stream().filter(x -> x.toString().equals(callBackData)).findFirst().get();
                    buy(chatId, block, message);
                }
            }
        } else if (callBackData.startsWith("get")) {
            editMessageText.setText("Получено!");
            getUserFiles(chatId, callBackData);
            sendMessage(this, editMessageText, message);
        }
    }

    private void buy(long chatId, Block block, Message message) {
        //todo change text
        Runnable task = () -> {
            // order description
            int price = block == null ? source.PRICE_ALL_BLOCKS : source.PRICE_ONE_BLOCK;
            int numberOfOrder = new Random().nextInt(10000, 100000);
            String payment_description = "Order " + numberOfOrder + "\n" + "User: " + chatId + "\n" + "Block: " + (block == null ? "all blocks" : block) + "\n" + "Price: " + price;
            // create request to get url and payment code
            // creating json request
            JSONObject urlJson = getUrlJson(payment_description, price, source.SERVER_URL);

            String id = (String) urlJson.get("payment_id");
            String url = (String) urlJson.get("payment_url");

            String description = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " создан :white_check_mark:") + "\n" + (block != null ? source.getOneBlockDescriptionPaymentText(block) : source.getAllBlocksDescriptionPaymentText());
            //sending information about order to user
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setText(description);
            editMessageText.setReplyMarkup(source.getOneButtonKeyboardMarkup("Оплатить", url, null));
            sendMessage(this, editMessageText, message);

            // process of checking whether payment paid or not
            Payment payment = new Payment();
            payment.setId(id);
            payment.setChatId(chatId);
            payment.setStatus("process");
            payment.setBlocks(block == null ? null : block.toString());
            payment.setServer_url(source.SERVER_URL);
            payment.setNumber_of_order(numberOfOrder);
            payment.setDate(new Date(System.currentTimeMillis()).toString());
            paymentRepository.save(payment);

            int response = checkPaymentStatusLoop(id, source.SERVER_URL);

            if (response == 1) {
                editMessageText.setReplyMarkup(null);
                if (block == null) {
                    users.save(registerUserAllBlocks(users.findById(chatId).get(), source.blocks));
                    editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики всех блоков :sunglasses: \nЧтобы их получить, перейдите в /show"));

                } else {
                    users.save(registerUserBlock(users.findById(chatId).get(), block));
                    editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики " + block.inRussian() + "а :sunglasses: \nЧтобы их получить, перейдите в /show"));
                }

            } else if (response == -1) {
                editMessageText.setReplyMarkup(null);
                editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего платеж был отменен. Повторите покупку или напишите в поддержку!"));
            } else if (response == 0) {
                editMessageText.setReplyMarkup(null);
                editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего у вас истек срок оплаты. Повторите покупку или напишите в поддержку!"));
            }
            sendMessage(this, editMessageText, message);
            paymentRepository.delete(paymentRepository.findById(numberOfOrder).get());
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private boolean adminCommandReceived(String messageText) {
        String[] commands = messageText.split(" ");
        List<User> users1 = (List<User>) users.findAll();
        var chatIds = users1.stream().map(User::getChatId).toList();
        if (messageText.contains("/send") && commands.length > 1) {
            for (User user : users1) {
                sendMessage(this, user.getChatId(), messageText.substring(messageText.indexOf(" ")));
            }
            return true;
        } else if (messageText.contains("/registerOne") && commands[1].chars().allMatch(Character::isDigit) && chatIds.contains(Long.parseLong(commands[1])) && commands.length == 3) {
            List<String> blocks = source.blocks.stream().map(Block::toString).toList();
            if (blocks.stream().anyMatch(commands[2]::equals)) {
                Block block = source.blocks.stream().filter(x -> x.toString().equals(commands[2])).findFirst().get();
                users.save(registerUserBlock(users.findById(Long.parseLong(commands[1])).get(), block));
                sendMessage(this, source.ADMIN_CHATID, block + " successfully registered");
            }
            return true;
        } else if (messageText.contains("/registerAll") && commands[1].chars().allMatch(Character::isDigit) && chatIds.contains(Long.parseLong(commands[1])) && commands.length == 2) {
            users.save(registerUserAllBlocks(users.findById(Long.parseLong(commands[1])).get(), source.blocks));
            sendMessage(this, source.ADMIN_CHATID, "all blocks successfully registered");
            return true;
        }
        return false;
    }

    private void startCommandReceived(Message message) {
        String answer = "Привет, " + message.getChat().getFirstName();
        sendMessage(this, message.getChatId(), answer);
        if (!users.existsById(message.getChatId())) {
            User user = new User();
            user.setChatId(message.getChatId());
            user.setUserName(message.getChat().getUserName());
            user.setFiles("");
            user.setBlocks("");
            users.save(user);
        }
    }

    private void getUserFiles(long chatId, String callBackDataBlock) {
        // sends all practices by block (it got like "get + block*" where * is a number of block) to user
        List<String> codeUnitNames = source.blocks.stream().filter(x -> ("get" + x.toString()).equals(callBackDataBlock)).findFirst().map(Block::getCodeUnits).get().stream().map(CodeUnit::getName).toList();
        List<File> files = Arrays.stream(users.findById(chatId).get().getFiles().split(",")).map(File::new).toList();
        Block block = source.blocks.stream().filter(x -> ("get" + x.toString()).equals(callBackDataBlock)).findFirst().get();
        sendMessage(this, chatId, "Ваши практики " + block.inRussian() + "а:");
        for (File file : files) {
            if (codeUnitNames.stream().anyMatch(x -> file.getName().startsWith(x))) {
                sendMessage(this, chatId, file);
            }
        }
        sendQuestions(this, chatId, block);
    }

    private void showUserFiles(long chatId) {
        // differs from getUserFiles that sends to user all blocks and practices he bought (no files will be sent)
        // if there aren't any blocks in database then tells user it's empty, otherwise sends what user has bought
        if (users.findById(chatId).get().getBlocks().isEmpty()) {
            sendMessage(this, chatId, EmojiParser.parseToUnicode("Здесь пока пусто :pensive:")); //todo change text
        } else {
            User user = users.findById(chatId).get();
            String[] blocks = user.getBlocks().split(","); //get block string split
            StringJoiner joiner = new StringJoiner("\n");
            sendMessage(this, chatId, "Ваши практики: ");
            //joiner to send messages
            //it sends each name of block and practices separately
            for (String block_string : blocks) {
                Block block = source.blocks.stream().filter(x -> x.toString().equals(block_string)).findFirst().get();
                joiner.add("*" + block.inRussian() + "*"); //it makes it look like *блок i*
                for (CodeUnit codeUnit : block.getCodeUnits()) {
                    joiner.add(codeUnit.getName());
                }
                InlineKeyboardMarkup inlineKeyboardMarkup = source.getOneButtonKeyboardMarkup("Получить", null, "get" + block);
                sendMessage(this, chatId, joiner.toString(), inlineKeyboardMarkup);
                joiner = new StringJoiner("\n"); //reloads joiner in order to send next block
            }
        }
    }

    private void sortBlocks(Message message) {
        // gets user blocks string from database
        // sorts blocks them in right order and saves to database
        User user = null;
        if (users.findById(message.getChatId()).isPresent()) {
            user = users.findById(message.getChatId()).get();
        }
        if (user != null && !user.getBlocks().isEmpty()) {
            List<String> blocks_string = Arrays.stream(user.getBlocks().split(",")).toList();
            List<Block> blocks = new ArrayList<>(blocks_string.stream().flatMap(x -> source.blocks.stream().filter(y -> y.toString().equals(x))).toList());
            blocks = blocks.stream().sorted().toList();
            StringJoiner stringJoiner = new StringJoiner(",");
            blocks.stream().map(Block::toString).forEach(stringJoiner::add);
            user.setBlocks(stringJoiner.toString());
            users.save(user);
            log.info(user + " blocks sorted");
        } else {
            log.info(user + " no blocks to sort");
        }
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

}