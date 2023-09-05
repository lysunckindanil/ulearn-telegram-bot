package com.example.ulearn.telegram_bot.service.fabricate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("FieldCanBeLocal")
@Slf4j
public class Generator {
    private final int DEFAULT_GENERATION_LIMIT = 1024;

    private final int limit;

    @SuppressWarnings("unused")
    public Generator(int limit) {
        this.limit = limit;
    }

    public Generator() {
        this.limit = DEFAULT_GENERATION_LIMIT;
    }

    // destination is directory where will be created folder with generated files (folder is original file name without extension)
    public void generate(Path original, Path pattern, Path destination) throws IOException {
        if (!Files.exists(original)) throw new IOException() {
            @Override
            public String toString() {
                return "Original file doesn't exist";
            }
        };
        if (!Files.exists(pattern)) throw new IOException() {
            @Override
            public String toString() {
                return "Pattern file doesn't exist";
            }
        };

        String originalString = readFile(original);
        List<String> formattedStrings = getFormattedStrings(pattern, originalString, limit);
        String name = FilenameUtils.removeExtension(original.getFileName().toString());
        saveFormattedStrings(formattedStrings, destination, name);
    }

    @SuppressWarnings("unused")
    public void generate(Path original, Path pattern, Path destination, int limit) throws IOException {
        String originalString = readFile(original);
        List<String> formattedStrings = getFormattedStrings(pattern, originalString, limit);
        String folder = FilenameUtils.removeExtension(original.getFileName().toString());
        saveFormattedStrings(formattedStrings, destination, folder);
    }

    private static String readFile(Path file) {
        // reads file to string
        StringBuilder code = new StringBuilder();
        try (FileReader fileReader = new FileReader(file.toFile())) {
            while (fileReader.ready()) {
                code.append((char) fileReader.read());
            }
        } catch (IOException e) {
            log.error("Generator: can't open file for reading code " + file);
        }
        return code.toString();
    }

    private List<String> getFormattedStrings(Path pattern, String original, int limit) {
        // load replacements
        List<List<String>> replacements = new ArrayList<>();
        List<String> patternLines;
        try {
            patternLines = Files.readAllLines(pattern);
        } catch (IOException e) {
            log.error("Generator: unable to read pattern file " + pattern);
            return null;
        }
        for (String patternLine : patternLines) {
            String[] patternWords = patternLine.split(",");
            ArrayList<String> lineOfReplacements = new ArrayList<>(Arrays.asList(patternWords).subList(0, patternWords.length));
            replacements.add(lineOfReplacements);
        }

        Set<String> strings = new HashSet<>(); // set for formatted code strings
        Random random = new Random();
        // replace code variables therefore get new strings
        for (int i = 0; i < limit; i++) {
            String s = original;
            for (List<String> replace : replacements) {
                s = s.replaceAll("(\\W)(" + replace.get(0) + ")(\\W)", "$1" + replace.get(random.nextInt(replace.size())) + "$3");
            }
            strings.add(s);
        }
        return strings.stream().toList();
    }

    private static void saveFormattedStrings(List<String> strings, Path destination, String name) throws IOException {
        //create folder for strings if there is no any
        if (Files.notExists(destination)) {
            Files.createDirectories(destination);
        }
        //print each string to file and save em to folders
        for (int i = 0; i < strings.size(); i++) {
            File file = new File(destination + File.separator + name + (i + 1) + ".txt");
            try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
                out.print(strings.get(i));
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
    }

}
