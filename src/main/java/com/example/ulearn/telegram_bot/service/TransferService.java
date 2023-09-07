package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.service.GeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class TransferService {
    private final GeneratorService generatorService;

    @Autowired
    public TransferService(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    public void transferFabricFile(Path file, Path pattern, Path destination, Path transferTo) throws IOException {
        File fabricFile;
        try {
            fabricFile = generatorService.getFabricFile(file, pattern, destination);
        } catch (NullPointerException e) {
            throw new IOException();
        }
        Files.move(fabricFile.toPath(), Path.of(transferTo + File.separator + fabricFile.getName()));
    }

    public void transferFile(Path file, Path transferTo) throws IOException {
        Files.copy(file, Path.of(transferTo + File.separator + file.getFileName()));
    }
}
