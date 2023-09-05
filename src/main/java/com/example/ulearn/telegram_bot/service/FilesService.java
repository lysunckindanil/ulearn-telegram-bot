package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.generator.engine.Generator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class FilesService {
    public static String SOURCE = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData";
    private static final String QUESTIONS_PATH = SOURCE + File.separator + "UlearnTestQuestions";
    private static final String USERS_CODE_FILES = SOURCE + File.separator + "UsersCodeFiles";
    private static final String FORMATTED_FILES = SOURCE + File.separator + "CodeFormattedFiles";
    private static final String PATTERN_FILES = SOURCE + File.separator + "CodePatternFiles";

    /*
     transfer files from main directory to user directories
     */

    public void transferFabricateCodeUnit(long chatId, File file) {
        Path path = createUserDirectory(chatId);
        String name = FilenameUtils.removeExtension(file.getName());
        Optional<File> fabricFile = getFabricFile(file, name);
        if (fabricFile.isPresent()) {
            try {
                Files.move(fabricFile.get().toPath(), Paths.get(path + File.separator + fabricFile.get().getName()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void transferCodeUnit(long chatId, File file) {
        Path path = createUserDirectory(chatId);
        try {
            Files.copy(file.toPath(), Paths.get(path + File.separator + file.getName()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path createUserDirectory(long chatId) {
        Path path = Paths.get(USERS_CODE_FILES + File.separator + chatId); // user files directory path
        // creates user directory if there isn't any
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log.error("Unable to create directory " + path);
            }
        }
        return path;
    }

    private static Optional<File> getFabricFile(File file, String name) {
        // path to sources dir
        // path where pattern should be by default
        Path pattern = Path.of(PATTERN_FILES + File.separator + file.getName());
        // path to folder where the method gets file or generated if it's empty
        Path generatedFilesFolder = Path.of(FORMATTED_FILES + File.separator + name);

        if (isDirEmpty(generatedFilesFolder)) {
            try {
                // path where generator creates folder with generated files
                Path pathWhereGenerateFolder = Path.of(FORMATTED_FILES);
                Generator generator = new Generator();
                generator.generate(file.toPath(), pattern, pathWhereGenerateFolder);
                return Optional.ofNullable(Objects.requireNonNull(generatedFilesFolder.toFile().listFiles())[0]);
            } catch (IOException e) {
                log.error("Unable to generate files");
                return Optional.empty();
            }
        } else return Optional.ofNullable(Objects.requireNonNull(generatedFilesFolder.toFile().listFiles())[0]);
    }

    private static boolean isDirEmpty(final Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ignored) {
        }
        return true;
    }

    /*
     get files from user's repositories
     */
    public Optional<File> getUserFileByShortName(long chatId, String name) {
        File directory = new File(USERS_CODE_FILES + File.separator + chatId);
        if (directory.exists())
            return Arrays.stream(Objects.requireNonNull(directory.listFiles())).toList().stream().filter(x -> x.getName().contains(name)).findFirst();

        else return Optional.empty();
    }

    public List<File> getQuestionFilesByFolder(String folderName) {
        // sends ulearn questions to user
        List<File> files = new ArrayList<>(); // get list of files
        File dir = new File(QUESTIONS_PATH + File.separator + folderName);
        if (dir.isDirectory()) {
            files = List.of(Objects.requireNonNull(dir.listFiles()));
        }
        return files;
    }


}
