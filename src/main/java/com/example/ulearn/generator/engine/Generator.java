package com.example.ulearn.generator.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Component
public class Generator {

    private static final int GENERATION_LIMIT = 1024;

    public static void generate(Path original, Path pattern, Path destination) throws IOException {
        String originalString = readFileWithCode(original);
        String folder = FilenameUtils.removeExtension(original.getFileName().toString());
        List<String> stringSet = getFormattedStrings(pattern, originalString);
        saveFormattedStrings(stringSet, destination, folder);
    }

    private static String readFileWithCode(Path file) {
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

    private static List<String> getFormattedStrings(Path pattern, String original) {
        List<List<String>> replacements = new ArrayList<>();
        // pattern generate algorithm
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
        // replace code variables
        for (int i = 0; i < GENERATION_LIMIT; i++) {
            String s = original;
            for (List<String> replace : replacements) {
                s = s.replaceAll("(\\W)(" + replace.get(0) + ")(\\W)", "$1" + replace.get(random.nextInt(replace.size())) + "$3");
            }
            strings.add(s);
        }
        return strings.stream().toList();
    }

    private static void saveFormattedStrings(List<String> strings, Path destination, String folder) throws IOException {
        // saves code files to block/practice/ dir
        Path path = Paths.get(destination + File.separator + folder);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }

        for (int i = 0; i < strings.size(); i++) {
            String fileString = path + File.separator + folder + (i + 1) + ".txt";
            File file = new File(fileString);
            try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
                if (!file.exists()) {
                    if (!file.createNewFile()) log.error("Generator: " + file + " is already created");
                }
                out.print(strings.get(i));
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
    }


}
