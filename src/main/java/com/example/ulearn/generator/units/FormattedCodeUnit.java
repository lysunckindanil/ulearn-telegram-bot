package com.example.ulearn.generator.units;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.example.ulearn.generator.engine.Generator.generate;

@Getter
public class FormattedCodeUnit extends CodeUnit {

    private final Path original;
    private final Path pattern;
    private final Path destination;

    public FormattedCodeUnit(File original, File pattern, File destination) {
        super(original);
        this.original = original.toPath();
        this.pattern = pattern.toPath();
        this.destination = destination.toPath();
    }

    public File getFile() {
        Path dir = Paths.get(destination + File.separator + FilenameUtils.removeExtension(original.getFileName().toString()));
        if (isDirEmpty(dir)) {
            try {
                generate(original, pattern, destination);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
