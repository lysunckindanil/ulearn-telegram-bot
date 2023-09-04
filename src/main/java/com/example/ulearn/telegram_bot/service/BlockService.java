package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.telegram_bot.model.Block;
import com.example.ulearn.telegram_bot.model.BlockRepository;
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


//    public void loadToRepo() {
//        List<Block> blocks = new ArrayList<>();
//        for (int i = 1; i < 11; i++) {
//            blocks.add(new Block(i));
//        }
//        blocks.get(1).addAllCodeUnits(List.of(new CodeUnit("Calculator", true), new CodeUnit("check"), new CodeUnit("calculate"), new CodeUnit("getRevertString"))); //2
//        blocks.get(2).addAllCodeUnits(List.of(new CodeUnit("Hospital", true), new CodeUnit("TodoList", true), new CodeUnit("getTwoDimensionalArray"))); //3
//        blocks.get(3).addAllCodeUnits(List.of(new CodeUnit("School", true), new CodeUnit("PhoneBook", true), new CodeUnit("Line"), new CodeUnit("Client"))); //4
//        blocks.get(4).addAllCodeUnits(List.of(new CodeUnit("AbstractLogger", true), new CodeUnit("TimeUnit"), new CodeUnit("Animal"))); //5
//        blocks.get(5).addAllCodeUnits(List.of(new CodeUnit("Customers", true), new CodeUnit("Handler"))); //6
//        blocks.get(6).addAllCodeUnits(List.of(new CodeUnit("Utils", true), new CodeUnit("FileUtils"), new CodeUnit("ImageResizer"))); //7
//        blocks.get(7).addAllCodeUnits(List.of(new CodeUnit("Parser", true), new CodeUnit("Buffer"))); //8
//        blocks.get(8).addAllCodeUnits(List.of(new CodeUnit("Employees", true))); //9
//        blocks.get(9).addAllCodeUnits(List.of(new CodeUnit("Airport", true))); //10
//        blockRepository.saveAll(blocks);
//
//    }

}
