package com.example.ulearn.telegram_bot.service;


import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.CodeUnit;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.repo.BlockRepository;
import com.example.ulearn.telegram_bot.model.repo.UserRepository;
import com.example.ulearn.telegram_bot.service.files.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.example.ulearn.telegram_bot.service.source.Resources.*;


@SuppressWarnings("SpringPropertySource")
@Slf4j
@Service
@PropertySource("telegram.properties")
public class UserService {
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final TransferService transferService;
    @Value("${admin.chatId}")
    public long ADMIN_CHATID;

    @Autowired
    public UserService(BlockRepository blockRepository, UserRepository userRepository, TransferService transferService) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.transferService = transferService;
    }

    public List<Block> getBlocks() {
        return blockRepository.findAll();
    }

    public List<File> getUserFilesByBlock(long chatId, Block block) {
        List<File> files = new ArrayList<>();
        File directory = new File(USERS_CODE_FILES + File.separator + chatId);
        if (!block.getCodeUnits().isEmpty()) {
            for (CodeUnit codeUnit : block.getCodeUnits()) {
                Optional<File> file = Arrays.stream(Objects.requireNonNull(directory.listFiles())).toList().stream().filter(x -> x.getName().contains(codeUnit.getName())).findFirst();
                file.ifPresent(files::add);
            }
        }
        return files;
    }

    public List<File> getQuestionFilesByBlock(Block block) {
        // gets questions from repository
        List<File> files = new ArrayList<>(); // get list of files
        File dir = new File(DEFAULT_QUESTIONS_PATH + File.separator + block.inEnglish());
        if (dir.isDirectory()) {
            files = List.of(Objects.requireNonNull(dir.listFiles()));
        }
        return files;
    }

    // registration blocks to users

    public void registerBlocks(User user, List<Block> blocksToAdd) {
        // add all blocks user doesn't have to database and resources
        List<Block> userBlocks = user.getBlocks();
        for (Block block : blocksToAdd) {
            if (userBlocks.stream().noneMatch(x -> x.equals(block))) {
                register(user, block);
                log.info("Registered " + block + " chatId: " + user.getChatId());
            } else {
                log.warn(block + " is yet registered to the user");
            }
        }
        userRepository.save(user);
    }

    public void registerBlocks(User user, Block blockToAdd) {
        registerBlocks(user, List.of(blockToAdd));
    }

    private void register(User user, Block block) {
        Path transferTo = Path.of(USERS_CODE_FILES + File.separator + user.getChatId());
        if (!Files.exists(transferTo)) {
            try {
                Files.createDirectories(transferTo);
            } catch (IOException e) {
                log.error("Unable to create user folder: " + user.getChatId());
            }
        }
        for (CodeUnit codeUnit : block.getCodeUnits()) {
            if (codeUnit.isFabricate()) {
                Path original = codeUnit.getOriginal().toPath();
                Path pattern = Path.of(PATTERN_FILES + File.separator + codeUnit.getOriginal().getName());
                Path destination = Path.of(FORMATTED_FILES + File.separator + codeUnit.getName());
                try {
                    transferService.transferFabricFile(original, pattern, destination, transferTo);
                } catch (IOException e) {
                    log.error("Unable to transfer fabric file chatId: " + user.getChatId());
                }
            } else {
                try {
                    transferService.transferFile(codeUnit.getOriginal().toPath(), transferTo);
                } catch (IOException e) {
                    log.error("Unable to transfer file chatId: " + user.getChatId());
                }
            }

        }
        user.addBlock(block);
    }

}
