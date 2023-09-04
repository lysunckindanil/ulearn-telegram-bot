package com.example.ulearn.telegram_bot.service.tools;


import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.untis.CodeUnit;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static com.example.ulearn.generator.engine.Generator.generate;


@Slf4j
public class RegisterTools {

    private static final String UsersCodeFiles = "src/main/resources/CodeData" + File.separator + "UsersCodeFiles";

    public static void registerBlocks(User user, List<Block> blocksToAdd) {
        // add all blocks user doesn't have to database and resources
        List<Block> userBlocks = user.getBlocks();
        for (Block block : blocksToAdd) {
            if (userBlocks.stream().noneMatch(x -> x.equals(block))) {
                register(user, block);
                log.info("Registered " + block + " chatId: " + user.getChatId());
            } else {
                log.warn(block + " is yet registered to the user");
            }
        }
    }

    public static void registerBlocks(User user, Block block) {
        registerBlocks(user, List.of(block));
    }

    private static void register(User user, Block block) {
        // transfer files from resources to user folder and adds information to database
        String files;
        if (block.getCodeUnits().isEmpty())
            files = ""; // if there are no code units then there are no files and nothing to transfer
        else
            files = transferDataToUserFiles(user.getChatId(), block.getCodeUnits()); //transfer files from resources to user folder
        // adds strings to database decided whether user data is empty or not
        if (user.getBlocks().isEmpty()) {
            user.setFiles(files);
            user.addBlock(block);
        } else {
            user.setFiles(user.getFiles() + "," + files);
            user.addBlock(block);
        }
    }

    private static String transferDataToUserFiles(Long chatId, List<CodeUnit> codeUnits) {
        // moves or copies files to user folder
        StringJoiner joiner = new StringJoiner(",");
        Path path = Paths.get(UsersCodeFiles + File.separator + chatId);

        // creates user directory if there isn't any
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log.error("Unable to create directory " + path);
            }
        }

        // moves or copies (based on whether it's FormattedCodeUnit or not) code units from generator folders to the directory
        for (CodeUnit codeUnit : codeUnits) {
            File file = getFile(codeUnit);
            joiner.add(path + File.separator + file.getName());
            try {
                if (codeUnit.isFabricate()) {
                    Files.move(file.toPath(), Paths.get(path + File.separator + file.getName()));
                } else {
                    Files.copy(file.toPath(), Paths.get(path + File.separator + file.getName()));
                }
            } catch (IOException e) {
                log.error("Unable to move " + file + " to " + path);
            }
        }
        log.info("Transferred files " + codeUnits + " chatId: " + chatId);
        return joiner.toString();
    }

    public static File getFile(CodeUnit codeUnit) {
        if (codeUnit.isFabricate()) {
            // path do sources
            String src = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData";
            // path where pattern should be by default
            Path pattern = Path.of(src + File.separator + "CodePatternFiles" + File.separator + codeUnit.getOriginal().getName());
            // path to folder where the method gets file or generated if it's empty
            Path destination = Path.of(src + File.separator + "CodeFormattedFiles" + File.separator + codeUnit.getName());
            if (isDirEmpty(destination)) {
                try {
                    if (Files.exists(pattern) && Files.exists(destination))
                        generate(codeUnit.getOriginal().toPath(), pattern, destination);
                    else log.error("Pattern or destination isn't exist");
                } catch (IOException e) {
                    log.error("Unable to generate files");
                }
            }
            return Objects.requireNonNull(destination.toFile().listFiles())[0];
        } else {
            return codeUnit.getOriginal();
        }
    }

    private static boolean isDirEmpty(final Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ignored) {
        }
        return true;
    }


}
