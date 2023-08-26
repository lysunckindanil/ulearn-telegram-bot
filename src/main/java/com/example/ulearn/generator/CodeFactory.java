package com.example.ulearn.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.example.ulearn.generator.Generator.generate;
import static com.example.ulearn.generator.Generator.src;

public class CodeFactory {
    public static File getFileByCodeUnit(CodeUnit codeUnit) {
        String practice = codeUnit.getName();
        String block = "";
        Path dir = Paths.get(src + File.separator + "CodeFormattedFiles" + File.separator + block + File.separator + practice);
        if (isDirEmpty(dir)) generate(practice, block);
        return Objects.requireNonNull(dir.toFile().listFiles())[0];
    }

    private static boolean isDirEmpty(final Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ignored) {
        }
        return true;
    }

}
