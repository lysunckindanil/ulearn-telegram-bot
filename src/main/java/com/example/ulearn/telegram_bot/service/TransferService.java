package com.example.ulearn.telegram_bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TransferService {
    private final GeneratorService generatorService;

    @Autowired
    public TransferService(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    public void transferFabricFile(Path file, Path pattern, Path destination, Path transferTo) throws IOException, NullPointerException {
        // This method gets fabric file from GeneratorService
        // Then it moves it to user's repository
        // NullPointerException if something went wrong with getting fabric file
        File fabricFile = generatorService.getFabricFile(file, pattern, destination);
        Files.move(fabricFile.toPath(), Path.of(transferTo + File.separator + fabricFile.getName()));
    }

    public void transferFile(Path file, Path transferTo) throws IOException {
        // This method copies not fabric file to user's repository
        Files.copy(file, Path.of(transferTo + File.separator + file.getFileName()));
    }
}
