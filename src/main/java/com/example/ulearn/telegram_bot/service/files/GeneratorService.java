package com.example.ulearn.telegram_bot.service.files;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Service
public class GeneratorService {

    public File getFabricFile(Path file, Path pattern, Path destination) {
        // path to sources dir
        // path to folder where the method gets file or generated if it's empty
        if (isDirEmpty(destination)) {
            // path where generator creates folder with generated files
            Generator generator = new Generator();
            try {
                generator.generate(file, pattern, destination);
            } catch (IOException e) {
                log.error("Unable to generate file, origin file: " + file);
            }
        }
        return Objects.requireNonNull(destination.toFile().listFiles())[0];
    }

    private static boolean isDirEmpty(final Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ignored) {
        }
        return true;
    }

}
