package com.example.ulearn.telegram_bot.service.files;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
@Slf4j
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
                log.error("Unable to move, file already exists");
            }
        }
    }

    public void transferFile(Path file, Path transferTo) {
        try {
            Files.copy(file, Path.of(transferTo + File.separator + file.getFileName()));
        } catch (IOException e) {
            log.error("Unable to copy, file already exists");
        }
    }
}
