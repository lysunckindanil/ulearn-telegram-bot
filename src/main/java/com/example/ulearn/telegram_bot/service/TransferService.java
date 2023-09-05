package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.service.fabricate.GeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class TransferService {
    private final GeneratorService generatorService;

    @Autowired
    public TransferService(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    public void transferFabricFile(Path file, Path pattern, Path destination, Path transferTo) {
        Optional<File> fabricFile = generatorService.getFabricFile(file, pattern, destination);
        if (fabricFile.isPresent()) {
            try {
                Files.move(fabricFile.get().toPath(), Path.of(transferTo + File.separator + fabricFile.get().getName()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void transferFile(Path file, Path transferTo) {
        try {
            Files.copy(file, Path.of(transferTo + File.separator + file.getFileName()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
