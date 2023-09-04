package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.config.BotConfig;
import com.example.ulearn.telegram_bot.model.*;
import com.example.ulearn.telegram_bot.model.untis.CodeUnit;
import com.example.ulearn.telegram_bot.service.source.BotResources;
import com.example.ulearn.telegram_bot.service.tools.PaymentTools;
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
import java.io.IOException;
import java.util.*;

import static com.example.ulearn.telegram_bot.service.tools.PaymentTools.checkPaymentStatusLoop;
import static com.example.ulearn.telegram_bot.service.tools.PaymentTools.getUrlJson;
import static com.example.ulearn.telegram_bot.service.tools.QuestionsTools.sendQuestions;
import static com.example.ulearn.telegram_bot.service.tools.RegisterTools.registerBlocks;
import static com.example.ulearn.telegram_bot.service.tools.SendMessageTools.sendMessage;
import static com.example.ulearn.telegram_bot.service.tools.SerializationTools.serializeToString;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final BotResources source;
    private final BlockService blockService;

    @Autowired
    public TelegramBot(BotConfig config, UserRepository userRepository, BotResources source, PaymentRepository paymentRepository, PaymentTools paymentTools, BlockService blockService) {
        super(config.getToken());
        this.config = config;
        this.userRepository = userRepository;
        this.source = source;
        this.paymentRepository = paymentRepository;
        this.blockService = blockService;
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (chatId == source.ADMIN_CHATID) {
                if (adminCommandReceived(messageText)) return;
            }
            switch (messageText) {
                case "/start" -> startCommandReceived(update.getMessage());
                case "/show" -> {
                    if (userRepository.findById(chatId).get().getBlocks().isEmpty())
                        sendMessage(this, chatId, EmojiParser.parseToUnicode("Здесь пока пусто :pensive:"));
                    else showUserFiles(chatId);
                }
                case "/buy" -> {
                    if (userRepository.findById(chatId).get().getBlocks().size() == blockService.getBlocks().size()) {
                        String text = EmojiParser.parseToUnicode("У вас уже куплены все блоки, вы большой молодец :blush:");
                        sendMessage(this, chatId, text);
                    } else sendMessage(this, chatId, source.getChoosingTwoOptionsText(), source.getBuyMenu());
                }
                case "/help" -> sendMessage(this, chatId, source.getHelpText());
                default -> {
                    String text = EmojiParser.parseToUnicode("Простите, но я вас не понимаю, чтобы посмотреть мои команды введите /help :relieved:");
                    sendMessage(this, chatId, text);
                }
            }

        } else if (update.hasCallbackQuery()) ifCallbackQueryGot(update.getCallbackQuery());
    }

    private void ifCallbackQueryGot(CallbackQuery callbackQuery) {
        String callBackData = callbackQuery.getData();
        Message message = callbackQuery.getMessage();
        long chatId = message.getChatId();
        EditMessageText editMessageText = new EditMessageText();
        // here two conditions below (BUY_ALL and BUY_ONE) for send user for payment action
        if (callBackData.equals(source.BUY_ALL_STRING)) {
            buy(chatId, null, message);
        } else if (callBackData.equals(source.BUY_ONE_STRING)) {
            // here bot sends user block choosing form in order to know which block the client wants to buy
            editMessageText.setText("Теперь, пожалуйста, выберите блок, который хотите купить");
            editMessageText.setReplyMarkup(source.getBlockChoosingMenu());
            sendMessage(this, editMessageText, message);
        } else if (callBackData.startsWith("block")) {
            // here callBackData from block choosing form
            if (userRepository.findById(chatId).get().getBlocks().stream().map(Block::inEnglish).anyMatch(x -> x.equals(callBackData))) {
                editMessageText.setText(EmojiParser.parseToUnicode("У вас уже куплен этот блок :face_with_monocle:"));
                sendMessage(this, editMessageText, message);
            } else {
                Block block = blockService.getBlocks().stream().filter(x -> x.inEnglish().equals(callBackData)).findFirst().get();
                buy(chatId, block, message);
            }
        } else if (callBackData.startsWith("get")) {
            // callBack on /show -> get any block command
            editMessageText.setText("Получено!");
            getUserFiles(chatId, blockService.getBlocks().stream().filter(x -> ("get" + x.inEnglish()).equals(callBackData)).findFirst().get());
            sendMessage(this, editMessageText, message);
        }
    }

    private void buy(long chatId, Block block, Message message) {
        Runnable task = () -> {
            // order description
            int price = block == null ? source.PRICE_ALL_BLOCKS : source.PRICE_ONE_BLOCK;
            int numberOfOrder = new Random().nextInt(10000, 100000);
            String payment_description = "Order " + numberOfOrder + "\n" + "User: " + chatId + "\n" + "Block: " + (block == null ? "all blocks" : block.inEnglish()) + "\n" + "Price: " + price;
            // create request to get url and payment code
            // creating json request
            JSONObject urlJson = getUrlJson(payment_description, price, source.SERVER_URL);

            String id = (String) urlJson.get("payment_id");
            String url = (String) urlJson.get("payment_url");
            String description = EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " создан :white_check_mark:") + "\n" + (block != null ? source.getOneBlockDescriptionPaymentText(block) : source.getAllBlocksDescriptionPaymentText());

            // sends information about order to user, with link to pay
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setText(description);
            editMessageText.setReplyMarkup(source.getOneButtonKeyboardMarkup("Оплатить", url, null));
            sendMessage(this, editMessageText, message);
            log.info("Created order " + numberOfOrder + " chatId " + chatId + " payment_id " + id);

            // writes payment info to database
            // process of checking whether payment paid or not
            Payment payment = new Payment();
            payment.setId(id);
            payment.setChatId(chatId);
            payment.setStatus("process");
            payment.setBlocks(block == null ? null : block.inEnglish());
            payment.setServer_url(source.SERVER_URL);
            payment.setNumber_of_order(numberOfOrder);
            payment.setDate(new Date(System.currentTimeMillis()).toString());
            // serializes message in order to have an ability to delete link if bot is broken, and we need somehow to restore payment (restorePayment method)
            try {
                payment.setMessage(serializeToString(message));
            } catch (IOException e) {
                log.error("Error to serialize message");
            }
            paymentRepository.save(payment);

            // gets response whether user has paid or not
            int response = checkPaymentStatusLoop(id, source.SERVER_URL, 1800);
            // if response got, analyses it
            if (response == 1) {
                User user = userRepository.findById(chatId).get();
                if (block == null) {
                    registerBlocks(user, blockService.getBlocks());
                    userRepository.save(user);
                    editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики всех блоков :sunglasses: \nЧтобы их получить, перейдите в /show"));

                } else {
                    registerBlocks(user, block);
                    userRepository.save(user);
                    editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " оплачен :white_check_mark:\n" + "Поздравляю! Вы купили практики " + block.inRussian() + "а :sunglasses: \nЧтобы их получить, перейдите в /show"));
                }
                log.info("ChatId " + chatId + " bought block/blocks payment_id " + id);
            } else if (response == -1) {
                editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего платеж был отменен. Повторите покупку или напишите в поддержку!"));
                log.info("ChatId " + chatId + " cancelled payment payment_id " + id);
            } else if (response == 0) {
                editMessageText.setText(EmojiParser.parseToUnicode("Заказ " + numberOfOrder + " отменен :persevere:\nСкорее всего у вас истек срок оплаты. Повторите покупку или напишите в поддержку!"));
                log.info("ChatId " + chatId + " is out of time payment_id " + id);
            }

            // deletes link in replyMarkup in order it wouldn't be available to pay after out of time
            editMessageText.setReplyMarkup(null);
            sendMessage(this, editMessageText, message);

            // status completed if all's gone right no matter was it paid, cancelled or out of time
            payment.setStatus("completed");
            paymentRepository.save(payment);
        };
        Thread thread = new Thread(task);
        thread.start();
    } //todo change text

    private boolean adminCommandReceived(String messageText) {
        // here admin commands
        var commands = messageText.split(" ");
        var users = (List<User>) userRepository.findAll();
        var chatIds = users.stream().map(User::getChatId).toList();

        /* returns true if any condition was passed in order to know whether admin wanted
           to use his commands or not */
        if (messageText.contains("/send") && commands.length > 1) {
            // send text message to all the users in database
            for (User user : users) {
                sendMessage(this, user.getChatId(), messageText.substring(messageText.indexOf(" ")));
            }
            return true;
        } else if (messageText.contains("/register") && commands[1].chars().allMatch(Character::isDigit) && chatIds.contains(Long.parseLong(commands[1])) && commands.length == 3) {
            // gives any user block by pattern /register chat_id block
            if (blockService.getBlocks().stream().map(Block::inEnglish).anyMatch(commands[2]::equals)) {
                long chatId = Long.parseLong(commands[1]);
                Block block = blockService.getBlocks().stream().filter(x -> x.inEnglish().equals(commands[2])).findFirst().get();
                User user = userRepository.findById(chatId).get();
                registerBlocks(user, block);
                userRepository.save(user);
                sendMessage(this, source.ADMIN_CHATID, block + " is registered");
            }
            return true;
        } else if (messageText.contains("/registerAll") && commands[1].chars().allMatch(Character::isDigit) && chatIds.contains(Long.parseLong(commands[1])) && commands.length == 2) {
            // gives user any block by pattern /registerAll chat_id
            long chatId = Long.parseLong(commands[1]);
            User user = userRepository.findById(chatId).get();
            registerBlocks(user, blockService.getBlocks());
            userRepository.save(user);
            sendMessage(this, source.ADMIN_CHATID, "All blocks are registered");
            log.info("Admin: all blocks registered chat_id " + chatId);
            return true;
        }
        return false;
    }

    private void startCommandReceived(Message message) {
        String answer = "Привет, " + message.getChat().getFirstName();
        sendMessage(this, message.getChatId(), answer);
        if (!userRepository.existsById(message.getChatId())) {
            User user = new User();
            user.setChatId(message.getChatId());
            user.setUserName(message.getChat().getUserName());
            user.setFiles("");
            // block1 with questions by default, new block because equals based on number
            registerBlocks(user, new Block(1));
            userRepository.save(user);
        }
    } //todo change text

    private void getUserFiles(long chatId, Block block) {
        // sends all practices by block (it got like "get + block*" where * is a number of block) to user
        // if its empty sends only questions
        if (!block.getCodeUnits().isEmpty()) {
            List<String> codeUnitNames = blockService.getBlocks().stream().filter(x -> x.equals(block)).findFirst().map(Block::getCodeUnits).get().stream().map(CodeUnit::getName).toList();
            List<File> files = Arrays.stream(userRepository.findById(chatId).get().getFiles().split(",")).map(File::new).toList();
            sendMessage(this, chatId, "Ваши практики " + block.inRussian() + "а:");
            for (File file : files) {
                if (codeUnitNames.stream().anyMatch(x -> file.getName().startsWith(x))) {
                    sendMessage(this, chatId, file);
                }
            }
        }
        sendQuestions(this, chatId, block);
    }

    private void showUserFiles(long chatId) {
        // differs from getUserFiles that sends to user all blocks and practices he bought (no files will be sent)
        List<Block> blocks = userRepository.findById(chatId).get().getBlocks();
        StringJoiner joiner = new StringJoiner("\n");
        sendMessage(this, chatId, "Ваши практики: ");

        //joiner to send messages
        //it sends each name of block and practices separately
        for (Block block : blocks) {
            joiner.add("*" + block.inRussian() + "*"); //it makes it look like *блок i*
            // if there are no code units then sends other message
            if (block.getCodeUnits().isEmpty()) joiner.add("Только контрольные вопросы");
            else {
                for (CodeUnit codeUnit : block.getCodeUnits()) {
                    joiner.add(codeUnit.getName());
                }
            }
            InlineKeyboardMarkup inlineKeyboardMarkup = source.getOneButtonKeyboardMarkup("Получить", null, "get" + block.inEnglish());
            sendMessage(this, chatId, joiner.toString(), inlineKeyboardMarkup);
            joiner = new StringJoiner("\n"); //reloads joiner in order to send next block
        }
    }


    @Override
    public String getBotUsername() {
        return config.getName();
    }

}