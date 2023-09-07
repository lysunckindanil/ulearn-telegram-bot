package com.example.ulearn.telegram_bot.service;


import com.example.ulearn.telegram_bot.exceptions.BlockRegistrationException;
import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.CodeUnit;
import com.example.ulearn.telegram_bot.model.User;
import com.example.ulearn.telegram_bot.model.repo.BlockRepository;
import com.example.ulearn.telegram_bot.model.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.example.ulearn.telegram_bot.source.Resources.*;


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

    public List<File> getUserFilesByBlock(long chatId, Block block) throws FileNotFoundException {
        // This method is needed to return list of files, which sendUserFilesByBlock method will send to user
        // This method gets the files from user's repository
        // If any of unit code's files wasn't found in user's directory, it throws exception
        // It means that if block declares that there should code unit file,
        // but it doesn't exist in user's directory, that means something went wrong
        List<File> files = new ArrayList<>();
        File directory = new File(USERS_CODE_FILES + File.separator + chatId);
        if (!block.getCodeUnits().isEmpty()) {
            for (CodeUnit codeUnit : block.getCodeUnits()) {
                File[] UserFiles = Objects.requireNonNull(directory.listFiles());
                Optional<File> file = Arrays.stream(UserFiles).toList().stream().filter(x -> x.getName().contains(codeUnit.getName())).findFirst();
                if (file.isPresent()) {
                    files.add(file.get());
                } else {
                    throw new FileNotFoundException();
                }
            }
        }
        return files;
    }

    public List<File> getQuestionFilesByBlock(Block block) throws FileNotFoundException {
        // This method is needed to get question files from repository and return them to sendQuestionsByBlock method
        // List of files is empty only once if directory of questions doesn't exist, it means block doesn't have questions
        // Otherwise it means that something went wrong and throws exception
        List<File> files = new ArrayList<>(); // get list of files
        File dir = new File(DEFAULT_QUESTIONS_PATH + File.separator + block.inEnglish());
        if (dir.isDirectory()) {
            files = List.of(Objects.requireNonNull(dir.listFiles()));
            if (files.isEmpty()) throw new FileNotFoundException();
        }
        return files;
    }

    // registration blocks to users

    public void registerBlocks(User user, List<Block> blocksToAdd) throws BlockRegistrationException {
        // This method defines which blocks user doesn't have.
        // Then calls register method to transfer code units of the block
        // All exceptions go to methods where this method will be called from,
        // so it will allow to track errors
        List<Block> userBlocks = user.getBlocks();
        for (Block block : blocksToAdd) {
            if (userBlocks.stream().noneMatch(x -> x.equals(block)))
                register(user, block);
            else
                log.error(block + " is yet registered to the user chatId: " + user.getChatId());
        }
        userRepository.save(user);
    }

    public void registerBlocks(User user, Block blockToAdd) throws BlockRegistrationException {
        registerBlocks(user, List.of(blockToAdd));
    }

    private void register(User user, Block block) throws BlockRegistrationException {
        // This method moves files to user's folder
        // The method throws BlockRegistrationException if something went wrong with transferring files
        Path transferTo = Path.of(USERS_CODE_FILES + File.separator + user.getChatId());
        if (!Files.exists(transferTo)) {
            try {
                Files.createDirectories(transferTo);
            } catch (IOException e) {
                log.error("Unable to create user folder chatId: " + user.getChatId());
                throw new BlockRegistrationException();
            }
        }
        for (CodeUnit codeUnit : block.getCodeUnits()) {
            if (codeUnit.isFabricate()) {
                // path to original file
                Path original = codeUnit.getOriginal().toPath();
                // path to pattern to generate files
                Path pattern = Path.of(PATTERN_FILES + File.separator + codeUnit.getOriginal().getName());
                // path to where files will be generated, folder name is name of code unit
                Path destination = Path.of(FORMATTED_FILES + File.separator + codeUnit.getName());
                try {
                    // adds block to user if everything went right only
                    transferService.transferFabricFile(original, pattern, destination, transferTo);
                    log.info("Registered " + block.inEnglish() + " chatId: " + user.getChatId());
                } catch (NullPointerException e) {
                    log.error("Register error Unable to get fabric file chatId: " + user.getChatId() + " block: " + block.inEnglish());
                    throw new BlockRegistrationException();
                } catch (IOException e) {
                    log.error("Register error unable to move fabric file chatId: " + user.getChatId() + " block: " + block.inEnglish());
                    throw new BlockRegistrationException();
                }
            } else {
                try {
                    // adds block to user if everything went right only
                    transferService.transferFile(codeUnit.getOriginal().toPath(), transferTo);
                } catch (IOException e) {
                    log.error("Unable to copy not fabric file chatId: " + user.getChatId() + " block: " + block.inEnglish());
                    throw new BlockRegistrationException();
                }
            }
        }
        user.addBlock(block);
    }

}
