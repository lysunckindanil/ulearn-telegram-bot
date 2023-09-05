package com.example.ulearn.telegram_bot;

import com.example.ulearn.telegram_bot.config.BotConfig;
import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.CodeUnit;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.repo.UserRepository;
import com.example.ulearn.telegram_bot.service.PaymentService;
import com.example.ulearn.telegram_bot.service.UserService;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
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

import static com.example.ulearn.telegram_bot.service.source.Resources.*;
import static com.example.ulearn.telegram_bot.service.tools.SendMessageTools.sendMessage;

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
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
                    if (userRepository.findById(chatId).get().getBlocks().size() == userService.getBlocks().size()) {
                        String text = EmojiParser.parseToUnicode("У вас уже куплены все блоки, вы большой молодец :blush:");
                        sendMessage(this, chatId, text);
                    } else sendMessage(this, chatId, paymentService.getChoosingTwoOptionsText(), getBuyMenu());
                }
                case "/help" -> sendMessage(this, chatId, getHelpText());
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
        if (callBackData.equals(BUY_ALL)) {
            paymentService.proceedPayment(this, chatId, null, message);
        } else if (callBackData.equals(BUY_ONE)) {
            // here bot sends user block choosing form in order to know which block the client wants to buy
            editMessageText.setText("Теперь, пожалуйста, выберите блок, который хотите купить");
            editMessageText.setReplyMarkup(getBlockChoosingMenu());
            sendMessage(this, editMessageText, message);
        } else if (callBackData.startsWith("block")) {
            // here callBackData from block choosing form
            if (userRepository.findById(chatId).get().getBlocks().stream().map(Block::inEnglish).anyMatch(x -> x.equals(callBackData))) {
                editMessageText.setText(EmojiParser.parseToUnicode("У вас уже куплен этот блок :face_with_monocle:"));
                sendMessage(this, editMessageText, message);
            } else {
                Block block = userService.getBlocks().stream().filter(x -> x.inEnglish().equals(callBackData)).findFirst().get();
                paymentService.proceedPayment(this, chatId, block, message);
            }
        } else if (callBackData.startsWith("get")) {
            // callBack on /show -> get any block command
            // finds the block by get+block regex
            Block blockToGet = userService.getBlocks().stream().filter(x -> ("get" + x.inEnglish()).equals(callBackData)).findFirst().get();
            editMessageText.setText("Получено!");
            sendUserFilesByBlock(chatId, blockToGet);
            sendMessage(this, editMessageText, message);
        }
    }

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
            if (userService.getBlocks().stream().map(Block::inEnglish).anyMatch(commands[2]::equals)) {
                long chatId = Long.parseLong(commands[1]);
                Block block = userService.getBlocks().stream().filter(x -> x.inEnglish().equals(commands[2])).findFirst().get();
                User user = userRepository.findById(chatId).get();
                userService.registerBlocks(user, block);
                sendMessage(this, userService.ADMIN_CHATID, block + " is registered");
            }
            return true;
        } else if (messageText.contains("/registerAll") && commands[1].chars().allMatch(Character::isDigit) && chatIds.contains(Long.parseLong(commands[1])) && commands.length == 2) {
            // gives user any block by pattern /registerAll chat_id
            long chatId = Long.parseLong(commands[1]);
            User user = userRepository.findById(chatId).get();
            userService.registerBlocks(user, userService.getBlocks());
            sendMessage(this, userService.ADMIN_CHATID, "All blocks are registered");
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
            // block1 with questions by default, new block because equals based on number
            userService.registerBlocks(user, new Block(1));
            userRepository.save(user);
        }
    } //todo change text

    private void showUserFiles(User user) {
        // differs from getUserFiles that sends to user all blocks and practices he bought (no files will be sent)
        List<Block> blocks = user.getBlocks();
        StringJoiner joiner = new StringJoiner("\n");
        sendMessage(this, user.getChatId(), "Ваши практики: ");

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
            InlineKeyboardMarkup inlineKeyboardMarkup = getOneButtonKeyboardMarkup("Получить", "get" + block.inEnglish());
            sendMessage(this, user.getChatId(), joiner.toString(), inlineKeyboardMarkup);
            joiner = new StringJoiner("\n"); //reloads joiner in order to send next block
        }
    }

    private void sendUserFilesByBlock(long chatId, Block block) {
        // sends all practices by block (it got like "get + block*" where * is a number of block) to user
        // if its empty sends only questions
        List<File> files = new ArrayList<>();
        if (!block.getCodeUnits().isEmpty()) {
            for (CodeUnit codeUnit : block.getCodeUnits()) {
                Optional<File> file = userService.getUserFileByCodeUnit(chatId, codeUnit);
                file.ifPresent(files::add);
            }
        }
        log.error(files.toString());
        if (files.isEmpty()) {
            sendMessage(this, chatId, "Я не нашел у вас доступных практик этого блока");
            sendQuestions(chatId, block);
        } else {
            sendMessage(this, chatId, "Ваши практики " + block.inRussian() + "а:");
            for (File file : files)
                sendMessage(this, chatId, file);
            sendQuestions(chatId, block);
        }
    }

    public void sendQuestions(long chatId, Block block) {
        // sends ulearn questions to user
        final String QUESTIONS_PATH = SOURCE + File.separator + "UlearnTestQuestions";
        List<File> files; // get list of files
        File dir = new File(QUESTIONS_PATH + File.separator + block.inEnglish());
        if (dir.isDirectory()) {
            files = List.of(Objects.requireNonNull(dir.listFiles()));
            if (!files.isEmpty())
                sendMessage(this, chatId, files, "Ваши контрольные вопросы " + block.inRussian() + "а"); // sends media group to user
        }
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }

}