package com.example.ulearn.telegram_bot.service;

import com.example.ulearn.generator.units.CodeUnit;
import com.example.ulearn.generator.units.FormattedCodeUnit;
import com.example.ulearn.telegram_bot.model.BlockEntity;
import com.example.ulearn.telegram_bot.model.BlockRepository;
import com.example.ulearn.telegram_bot.model.CodeUnitEntity;
import com.example.ulearn.telegram_bot.service.source.Block;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Getter
public class BlockService {
    private final BlockRepository blockRepository;

    @Autowired
    public BlockService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    private List<Block> blocks = new ArrayList<>();

    private static CodeUnitEntity getDefFCU(String name) {
        String file_name = name + ".txt";
        String src = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData";
        File original = new File(src + File.separator + "CodeOriginalFiles" + File.separator + file_name);
        File pattern = new File(src + File.separator + "CodePatternFiles" + File.separator + file_name);
        File destination = new File(src + File.separator + "CodeFormattedFiles" + File.separator);
        return new CodeUnitEntity(original, pattern, destination);
    }

    private static CodeUnitEntity getDefCU(String name) {
        String file_name = name + ".txt";
        String src = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData";
        File file = new File(src + File.separator + "CodeOriginalFiles" + File.separator + file_name);

        return new CodeUnitEntity(file);
    }

    public void loadToRepo() {
        List<BlockEntity> blocks = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            blocks.add(new BlockEntity(i));
        }
        blocks.get(1).addAllCodeUnits(List.of(getDefFCU("Calculator"), getDefCU("check"), getDefCU("calculate"), getDefCU("getRevertString"))); //2
        blocks.get(2).addAllCodeUnits(List.of(getDefFCU("Hospital"), getDefFCU("TodoList"), getDefCU("getTwoDimensionalArray"))); //3
        blocks.get(3).addAllCodeUnits(List.of(getDefFCU("School"), getDefFCU("PhoneBook"), getDefCU("Line"), getDefCU("Client"))); //4
        blocks.get(4).addAllCodeUnits(List.of(getDefFCU("AbstractLogger"), getDefCU("TimeUnit"), getDefCU("Animal"))); //5
        blocks.get(5).addAllCodeUnits(List.of(getDefFCU("Customers"), getDefCU("Handler"))); //6
        blocks.get(6).addAllCodeUnits(List.of(getDefFCU("Utils"), getDefCU("FileUtils"), getDefCU("ImageResizer"))); //7
        blocks.get(7).addAllCodeUnits(List.of(getDefFCU("Parser"), getDefCU("Buffer"))); //8
        blocks.get(8).addAllCodeUnits(List.of(getDefFCU("Employees"))); //9
        blocks.get(9).addAllCodeUnits(List.of(getDefFCU("Airport"))); //10
        blockRepository.saveAll(blocks);

    }

    public void loadFromRepo() {
        for (BlockEntity blockEntity : blockRepository.findAll()) {
            Block block = new Block(blockEntity.getNumber());
            List<CodeUnit> codeUnits = new ArrayList<>();
            List<CodeUnitEntity> codeUnitEntities = blockEntity.getCodeUnits();
            for (CodeUnitEntity codeUnitEntity : codeUnitEntities) {
                CodeUnit codeUnit;
                if (codeUnitEntity.getPattern() == null) {
                    codeUnit = new CodeUnit(codeUnitEntity.getOriginal());
                } else {
                    codeUnit = new FormattedCodeUnit(codeUnitEntity.getOriginal(), codeUnitEntity.getPattern(), codeUnitEntity.getDestination());
                }
                codeUnits.add(codeUnit);
            }
            block.addAllCodeUnits(codeUnits);
            blocks.add(block);
        }
    }
}
