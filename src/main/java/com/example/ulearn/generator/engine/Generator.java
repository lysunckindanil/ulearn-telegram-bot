package com.example.ulearn.generator.engine;

import com.example.ulearn.generator.units.CodeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Component
public class Generator {

    public static final String src = "src/main/resources/CodeData";

    private static final int GENERATION_LIMIT = 1024;

    static void generate(String practice, String block) {
        String original = readFileWithCode(new File(src + File.separator + "CodeOriginalFiles" + File.separator + practice + ".txt"));
        Path pattern = Paths.get(src + File.separator + "CodePatternFiles" + File.separator + practice + ".txt");
        Set<String> stringSet = getFormattedStrings(pattern, original);
        saveFormattedStrings(stringSet, practice, block);
    }

    private static String readFileWithCode(File file) {
        StringBuilder code = new StringBuilder();
        try (FileReader fileReader = new FileReader(file)) {
            while (fileReader.ready()) {
                code.append((char) fileReader.read());
            }
        } catch (IOException e) {
            log.error("Generator: can't open file for reading code " + file);
        }
        return code.toString();
    }

    private static Set<String> getFormattedStrings(Path pattern, String code) {
        List<List<String>> replacements = new ArrayList<>();

        // pattern generate algorithm
        List<String> patternLines;
        try {
            patternLines = Files.readAllLines(pattern);
        } catch (IOException e) {
            log.error("Generator: unable to read pattern file " + pattern);
            return new HashSet<>();
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
            String s = code;
            for (List<String> replace : replacements) {
                s = s.replaceAll("(\\W)(" + replace.get(0) + ")(\\W)", "$1" + replace.get(random.nextInt(replace.size())) + "$3");
            }
            strings.add(s);
        }
        return strings;
    }

    private static void saveFormattedStrings(Set<String> strings, String practice, String block) {
        // saves code files to block/practice/ dir
        String path = src + File.separator + "CodeFormattedFiles" + File.separator + block + File.separator + practice;
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            log.warn("Generator: " + path + " directories are already created");
        }
        int k = 1;
        for (String string : strings) {
            String fileString = path + File.separator + practice + k + ".txt";
            File file = new File(fileString);
            try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
                if (!file.exists()) {
                    if (!file.createNewFile()) log.error("Generator: " + file + " is already created");
                }
                out.print(string);
            } catch (IOException e) {
                log.error(e.toString());
            }
            k += 1;
        }
    }

    public static File getFile(String practice, String block) {
        Path dir = Paths.get(src + File.separator + "CodeFormattedFiles" + File.separator + block + File.separator + practice);
//        if (isDirEmpty(dir)) generate(practice, block);
        return Objects.requireNonNull(dir.toFile().listFiles())[0];
    }


//    public static File getFileByCodeUnit(CodeUnit codeUnit) {
//        String practice = codeUnit.getName();
//        Path dir = Paths.get(src + File.separator + "CodeFormattedFiles" + File.separator + block + File.separator + practice);
//        if (isDirEmpty(dir)) generate(practice, block);
//        return Objects.requireNonNull(dir.toFile().listFiles())[0];
//    }


}
