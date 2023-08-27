package com.example.ulearn.telegram_bot.service.tools;

import com.example.ulearn.telegram_bot.service.source.Block;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static com.example.ulearn.telegram_bot.service.tools.SendMessageTools.sendMessage;

public class QuestionsTools {
    public static void sendQuestions(TelegramLongPollingBot bot, long chatId, Block block) {
        List<File> files = getFilesByBlock(block.toString());
        String caption = "Ваши контрольные вопросы " + block.inRussian() + "а";
        if (files != null) sendMessage(bot, chatId, files, caption);
    }

    private static List<File> getFilesByBlock(String block) {
        File dir = new File("src/main/resources/CodeData/UlearnTestQuestions" + File.separator + block);
        if (dir.isDirectory()) {
            return List.of(Objects.requireNonNull(dir.listFiles()));
        }
        return null;
    }

}
