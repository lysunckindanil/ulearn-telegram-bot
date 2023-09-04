package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.repo.BlockRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@Getter
public class BlockService {
    private final BlockRepository blockRepository;
    private final List<Block> blocks;

    @Autowired
    public BlockService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
        this.blocks = blockRepository.findAll();
    }

}
