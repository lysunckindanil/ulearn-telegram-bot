package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.generator.engine.Generator;
import com.example.ulearn.telegram_bot.model.CodeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.example.ulearn.telegram_bot.service.source.BotResources.SOURCE;

@Service
@Slf4j
public class FilesService {
    private static final String USERS_CODE_FILES = SOURCE + File.separator + "UsersCodeFiles";
    private static final String FORMATTED_FILES = SOURCE + File.separator + "CodeFormattedFiles";

    // moves or copies files to user folder
    public void transferDataToUserFiles(Long chatId, List<CodeUnit> codeUnits) {

        Path path = Paths.get(USERS_CODE_FILES + File.separator + chatId); // user files directory path
        // creates user directory if there isn't any
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log.error("Unable to create directory " + path);
            }
        }

        // moves or copies (based on whether it's Fabricate or not) code units from generator folders to the directory
        for (CodeUnit codeUnit : codeUnits) {
            Optional<File> optionalFile = getFile(codeUnit);
            if (optionalFile.isPresent()) {
                File file = optionalFile.get();
                try {
                    if (codeUnit.isFabricate()) {
                        Files.move(file.toPath(), Paths.get(path + File.separator + file.getName()));
                    } else {
                        Files.copy(file.toPath(), Paths.get(path + File.separator + file.getName()));
                    }
                } catch (IOException e) {
                    log.error("Unable to move " + file + " to " + path + e);
                }
                log.info("Transferred files " + codeUnits + " chatId: " + chatId);

            }
        }
    }

    private static Optional<File> getFile(CodeUnit codeUnit) {
        if (!codeUnit.getOriginal().exists()) return Optional.empty();
        if (codeUnit.isFabricate()) {
            // path to sources dir
            // path where pattern should be by default
            Path pattern = Path.of(SOURCE + File.separator + "CodePatternFiles" + File.separator + codeUnit.getOriginal().getName());
            // path to folder where the method gets file or generated if it's empty
            Path generatedFilesFolder = Path.of(SOURCE + File.separator + "CodeFormattedFiles" + File.separator + codeUnit.getName());

            if (isDirEmpty(generatedFilesFolder)) {
                try {
                    // path where generator creates folder with generated files
                    Path pathWhereGenerateFolder = Path.of(FORMATTED_FILES);
                    Generator generator = new Generator();
                    generator.generate(codeUnit.getOriginal().toPath(), pattern, pathWhereGenerateFolder);
                    return Optional.ofNullable(Objects.requireNonNull(generatedFilesFolder.toFile().listFiles())[0]);
                } catch (IOException e) {
                    log.error("Unable to generate files");
                    return Optional.empty();
                }
            } else return Optional.ofNullable(Objects.requireNonNull(generatedFilesFolder.toFile().listFiles())[0]);
        }
        return Optional.of(codeUnit.getOriginal());
    }

    public Optional<File> getUserFileByShortName(long chatId, String name) {
        File directory = new File(USERS_CODE_FILES + File.separator + chatId);
        if (directory.exists())
            return Arrays.stream(Objects.requireNonNull(directory.listFiles())).toList().stream().filter(x -> x.getName().contains(name)).findFirst();

        else return Optional.empty();
    }

    private static boolean isDirEmpty(final Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ignored) {
        }
        return true;
    }
}
