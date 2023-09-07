package com.example.ulearn.telegram_bot;

import com.example.ulearn.telegram_bot.config.BotConfig;
import com.example.ulearn.telegram_bot.exceptions.BlockRegistrationException;
import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.CodeUnit;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.repo.UserRepository;
import com.example.ulearn.telegram_bot.service.PaymentService;
import com.example.ulearn.telegram_bot.service.UserService;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static com.example.ulearn.telegram_bot.source.Resources.*;
import static com.example.ulearn.telegram_bot.tools.SendMessageTools.sendMessage;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PaymentService paymentService;

    @Autowired
    public TelegramBot(BotConfig config, UserRepository userRepository, PaymentService paymentService, UserService userService) {
        super(config.getToken());
        this.config = config;
        this.userRepository = userRepository;
        this.userService = userService;
        this.paymentService = paymentService;
        paymentService.restorePayments(this);
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
        // checks whether there is call back data
        if (update.hasCallbackQuery()) ifCallbackQueryGot(update.getCallbackQuery());
            // if there's not then handle text from message
        else if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            // if chatId equals admin's one then checks whether some of admin's methods was executed
            // if it was then all below are skipped, otherwise admin had wanted something else and code below will be handled
            if (chatId == userService.ADMIN_CHATID) {
                if (adminCommandReceived(messageText)) return;
            }
            switch (messageText) {
                case "/start" -> startCommandReceived(update.getMessage());
                case "/show" -> {
                    User user = userRepository.findById(chatId).get();
                    if (user.getBlocks().isEmpty())
                        sendMessage(this, chatId, EmojiParser.parseToUnicode("Здесь пока пусто :pensive:"));
                    else showUserFiles(user);
                }
                case "/buy" -> {
                    if (userRepository.findById(chatId).get().getBlocks().size() == userService.getBlocks().size())
                        sendMessage(this, chatId, EmojiParser.parseToUnicode("У вас уже куплены все блоки, вы большой молодец :blush:"));
                    else sendMessage(this, chatId, paymentService.getChoosingTwoOptionsText(), getBuyMenu());
                }
                case "/help" -> sendHelpMessage(update.getMessage());
                default -> {
                    String text = EmojiParser.parseToUnicode("Простите, но я вас не понимаю, чтобы посмотреть мои команды введите /help :relieved:");
                    sendMessage(this, chatId, text);
                }
            }
        }

    }

    private void ifCallbackQueryGot(CallbackQuery callbackQuery) {
        String callBackData = callbackQuery.getData();
        Message message = callbackQuery.getMessage();
        long chatId = message.getChatId();
        EditMessageText editMessageText = new EditMessageText();

        // bot redirects user to payment action
        if (callBackData.equals(BUY_ALL))
            paymentService.proceedPayment(this, chatId, null, message);

            // here bot redirects user to block choosing form in order to know which block the client wants to buy
        else if (callBackData.equals(BUY_ONE)) {
            editMessageText.setText("Теперь, пожалуйста, выберите блок, который хотите купить");
            editMessageText.setReplyMarkup(getBlockChoosingMenu(userService.getBlocks().subList(1, userService.getBlocks().size())));
            sendMessage(this, editMessageText, message);
        }

        // here callBackData from block choosing form and redirects to payment action
        else if (callBackData.startsWith("block")) {
            if (userRepository.findById(chatId).get().getBlocks().stream().map(Block::inEnglish).anyMatch(x -> x.equals(callBackData))) {
                editMessageText.setText(EmojiParser.parseToUnicode("У вас уже куплен этот блок :face_with_monocle:"));
                sendMessage(this, editMessageText, message);
            } else {
                Block block = userService.getBlocks().stream().filter(x -> x.inEnglish().equals(callBackData)).findFirst().get();
                paymentService.proceedPayment(this, chatId, block, message);
            }
        }

        // here call back data from show user files, sends user files to user by block
        else if (callBackData.startsWith("get")) {
            // callBack on /show -> get any block command
            // finds the block by get+block regex
            Block blockToGet = userService.getBlocks().stream().filter(x -> ("get" + x.inEnglish()).equals(callBackData)).findFirst().get();
            editMessageText.setText("Получено!");
            sendUserFilesByBlock(chatId, blockToGet);
            sendMessage(this, editMessageText, message);
        }
    }

    private boolean adminCommandReceived(String messageText) {
        /* returns true if any condition was passed in order to know whether admin wanted
           to use his commands or not */

        // sends text message to all users
        if (Pattern.matches("/send [\\s\\S]*", messageText)) {
            var users = (List<User>) userRepository.findAll();
            var message = messageText.substring(messageText.indexOf(" "));
            for (User user : users) {
                sendMessage(this, user.getChatId(), message);
            }
            sendMessage(this, userService.ADMIN_CHATID, "Message was successfully sent to everybody");
            return true;
        }

        // registers only one block to user
        else if (Pattern.matches("/register \\d{9} block\\d*", messageText)) {
            var commands = messageText.split(" ");
            var block = userService.getBlocks().stream().filter(x -> x.inEnglish().equals(commands[2])).findFirst();
            var user = userRepository.findById(Long.parseLong(commands[1]));
            // check whether user or block exists or not
            if (block.isEmpty()) {
                sendMessage(this, userService.ADMIN_CHATID, "Block doesn't exist");
                return true;
            }
            if (user.isEmpty()) {
                sendMessage(this, userService.ADMIN_CHATID, "User doesn't exist in database");
                return true;
            }
            try {
                userService.registerBlocks(user.get(), block.get());
                sendMessage(this, userService.ADMIN_CHATID, block.get().inEnglish() + " is registered to the user");
            } catch (BlockRegistrationException e) {
                sendMessage(this, userService.ADMIN_CHATID, "Unable to register block to the user");
            }
            return true;
        }

        // registers all blocks to user
        else if (Pattern.matches("/register \\d{9}", messageText)) {
            // gives user any block by pattern /registerAll chat_id
            var commands = messageText.split(" ");
            Optional<User> user = userRepository.findById(Long.parseLong(commands[1]));
            if (user.isPresent()) {
                try {
                    userService.registerBlocks(user.get(), userService.getBlocks());
                    sendMessage(this, userService.ADMIN_CHATID, "All blocks are registered to the user");
                } catch (BlockRegistrationException e) {
                    sendMessage(this, userService.ADMIN_CHATID, "Unable to register blocks to the user");
                }
            } else {
                sendMessage(this, userService.ADMIN_CHATID, "User doesn't exists in database");
            }
            return true;
        }

        // clears user folder and deletes block from database, usually to be used if something went wrong
        else if (Pattern.matches("/clear \\d{9}", messageText)) {
            var chatId = Long.parseLong(messageText.split(" ")[1]);
            var userDir = new File(USERS_CODE_FILES + File.separator + chatId);
            Optional<User> userOpt = userRepository.findById(chatId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setBlocks(new ArrayList<>());
                userRepository.save(user);
                sendMessage(this, userService.ADMIN_CHATID, "User blocks successfully deleted");
                if (userDir.exists()) {
                    try {
                        FileUtils.cleanDirectory(userDir);
                        sendMessage(this, userService.ADMIN_CHATID, "User directory was successfully cleaned");
                    } catch (IOException e) {
                        sendMessage(this, userService.ADMIN_CHATID, "Unable to clean user folder");
                    }
                } else {
                    sendMessage(this, userService.ADMIN_CHATID, "User directory doesn't exist");
                }
            } else {
                sendMessage(this, userService.ADMIN_CHATID, "User doesn't exist");
            }
            return true;
        }
        return false;
    }

    private void startCommandReceived(Message message) {
        sendHelpMessage(message);
        if (!userRepository.existsById(message.getChatId())) {
            User user = new User();
            user.setChatId(message.getChatId());
            user.setUserName(message.getChat().getUserName());
            // block1 with questions by default, new block because equals based on number
            try {
                userService.registerBlocks(user, new Block(1));
            } catch (BlockRegistrationException e) {
                sendMessage(this, user.getChatId(), "Не получилось получить контрольные вопросы первого блок. Напишите, пожалуйста, в поддержку!");
            }
            userRepository.save(user);
        }
    }

    private void showUserFiles(User user) {
        // differs from getUserFiles that sends to user all blocks and practices he bought (no files will be sent)
        StringJoiner joiner = new StringJoiner("\n");
        sendMessage(this, user.getChatId(), "Ваши практики: ");

        //joiner to send messages
        //it sends each name of block and practices separately
        for (Block block : user.getBlocks()) {
            joiner.add(EmojiParser.parseToUnicode(block.inRussian().toUpperCase()));
            // if there are no code units then sends other message
            if (block.getCodeUnits().isEmpty()) joiner.add("Только контрольные вопросы");
            else {
                for (CodeUnit codeUnit : block.getCodeUnits()) {
                    if (codeUnit.isFabricate())
                        joiner.add(EmojiParser.parseToUnicode(":small_blue_diamond:Практика " + codeUnit.getName()));
                    else joiner.add(EmojiParser.parseToUnicode(":small_blue_diamond:Задание " + codeUnit.getName()));
                }
                joiner.add(EmojiParser.parseToUnicode(":small_blue_diamond:Ответы на контрольные вопросы"));
            }
            InlineKeyboardMarkup inlineKeyboardMarkup = getOneButtonKeyboardMarkup("Получить", "get" + block.inEnglish());
            sendMessage(this, user.getChatId(), joiner.toString(), inlineKeyboardMarkup);
            joiner = new StringJoiner("\n"); //reloads joiner in order to send next block
        }
    }

    private void sendUserFilesByBlock(long chatId, Block block) {
        // sends all practices by block (it got like "get + block*" where * is a number of block) to user
        // if its empty sends only questions
        List<File> files = new ArrayList<>();
        try {
            files = userService.getUserFilesByBlock(chatId, block);
        } catch (FileNotFoundException e) {
            sendMessage(this, chatId, "Произошла ошибка при загрузке ваших практик и заданий " + block.inRussian() + "a. Напишите, пожалуйста, в поддержку!");
        }
        if (!files.isEmpty()) {
            sendMessage(this, chatId, "Ваши практики и задания " + block.inRussian() + "а:");
            for (File file : files)
                sendMessage(this, chatId, file);
        }
        sendQuestionsByBlock(chatId, block);
    }

    public void sendQuestionsByBlock(long chatId, Block block) {
        // sends ulearn questions to user
        // if folder with the same name as the block exists and there are no files, it throws error
        // if folder doesn't exist, then block is considered to be with no questions,
        // therefore no messages related to questions will not be sent
        List<File> files;
        try {
            files = userService.getQuestionFilesByBlock(block);
            if (!files.isEmpty())
                sendMessage(this, chatId, files, "Ваши контрольные вопросы " + block.inRussian() + "а"); // sends media group to user
        } catch (FileNotFoundException e) {
            sendMessage(this, chatId, "Произошла ошибка при загрузке ваших контрольных вопросов " + block.inRussian() + "a. Напишите, пожалуйста, в поддержку!"); // sends media group to user
        }
    }

    public void sendHelpMessage(Message message) {
        InlineKeyboardMarkup inlineKeyboardMarkup = getOneButtonLinkKeyboardMarkup("Видео инструкция", "https://disk.yandex.com/i/CZaD3BIYTqER1w");
        sendMessage(this, message.getChatId(), getHelpText(message.getChat()), inlineKeyboardMarkup);

    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }

}