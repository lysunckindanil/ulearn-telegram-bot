package com.example.ulearn.telegram_bot.service.tools;


import com.example.ulearn.generator.units.CodeUnit;
import com.example.ulearn.generator.units.FormattedCodeUnit;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.service.source.Block;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;


@Slf4j
public class RegisterTools {

    private static final String UsersCodeFiles = "src/main/resources/CodeData" + File.separator + "UsersCodeFiles";

    public static User registerUserAllBlocks(User user, List<Block> blocks) {
        // add all blocks user doesn't have to database and resources

        String[] string = user.getBlocks().split(",");
        for (Block block : blocks) {
            if (Arrays.stream(string).noneMatch(x -> x.equals(block.toString()))) {
                user = registerUserBlock(user, block);
            }
        }
        return user;

    }

    public static User registerUserBlock(User user, Block block) {
        // transfer files from resources to user folder and adds information to database
        String files = transferDataToUserFiles(user.getChatId(), block.getCodeUnits()); //transfer files from resources to user folder
        String user_files = user.getFiles();
        // adds strings to database decided whether user data is empty or not
        if (user_files.isEmpty()) {
            user.setFiles(files);
            user.setBlocks(block.toString());
        } else {
            user.setFiles(user_files + "," + files);
            user.setBlocks(user.getBlocks() + "," + block);
        }
        return user;
    }

    private static String transferDataToUserFiles(Long chatId, List<CodeUnit> codeUnits) {
        StringJoiner joiner = new StringJoiner(",");
        Path path = Paths.get(UsersCodeFiles + File.separator + chatId);
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log.error("Unable to create directory " + path);
            }
        }
        for (CodeUnit codeUnit : codeUnits) {
            File file = codeUnit.getFile();
            joiner.add(path + File.separator + file.getName());
            try {
                if (codeUnit instanceof FormattedCodeUnit) {
                    Files.move(file.toPath(), Paths.get(path + File.separator + file.getName()));
                } else {
                    Files.copy(file.toPath(), Paths.get(path + File.separator + file.getName()));
                }
            } catch (IOException e) {
                log.error("Unable to move " + file + " to " + path);
            }
        }
        return joiner.toString();
    }
}
