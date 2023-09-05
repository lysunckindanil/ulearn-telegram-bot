package com.example.ulearn.telegram_bot.service;


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
import java.util.List;
import java.util.Optional;

@SuppressWarnings("SpringPropertySource")
@Slf4j
@Service
@PropertySource("telegram.properties")
public class UserService {
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FilesService filesService;
    @Value("${admin.chatId}")
    public long ADMIN_CHATID;

    public List<Block> getBlocks() {
        return blockRepository.findAll();
    }

    @Autowired
    public UserService(BlockRepository blockRepository, UserRepository userRepository, FilesService filesService) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.filesService = filesService;
    }

    // transfers data to user folder, adds files in database to user and saves it
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
        // transfer files from resources to user folder and adds information to database
        filesService.transferDataToUserFiles(user.getChatId(), block.getCodeUnits()); //transfer files from resources to user folder
        // adds strings to database decided whether user data is empty or not
        user.addBlock(block);
    }

    public Optional<File> getUserFileByCodeUnit(long chatId, CodeUnit codeUnit) {
        return filesService.getUserFileByShortName(chatId, codeUnit.getName());
    }
}
