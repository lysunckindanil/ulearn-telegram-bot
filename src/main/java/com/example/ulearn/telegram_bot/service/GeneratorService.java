package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.tools.Generator;
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
        // method throws null if something went wrong,
        // whatever some error in generator or no files were generated
        if (isDirEmpty(destination)) {
            Generator generator = new Generator();
            try {
                generator.generate(file, pattern, destination);
            } catch (IOException e) {
                log.error("Unable to generate file, origin file: " + file);
                throw new NullPointerException();
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
