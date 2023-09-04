package com.example.ulearn.telegram_bot.service.tools;

import com.example.ulearn.telegram_bot.model.Block;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static com.example.ulearn.telegram_bot.service.tools.SendMessageTools.sendMessage;

public class QuestionsTools {
    public static String QUESTIONS_PATH = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData" + File.separator + "UlearnTestQuestions";

    public static void sendQuestions(TelegramLongPollingBot bot, long chatId, Block block) {
        // sends ulearn questions to user
        List<File> files = getFilesByBlock(block.inEnglish()); // get list of files
        String caption = "Ваши контрольные вопросы " + block.inRussian() + "а";
        if (files != null) sendMessage(bot, chatId, files, caption); // sends media group to user
    }

    private static List<File> getFilesByBlock(String block) {
        // finds them in a directory and returns them in a list
        File dir = new File(QUESTIONS_PATH + File.separator + block);
        if (dir.isDirectory()) {
            return List.of(Objects.requireNonNull(dir.listFiles()));
        }
        return null;
    }

}
