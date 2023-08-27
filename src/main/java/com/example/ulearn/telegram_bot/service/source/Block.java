package com.example.ulearn.telegram_bot.service.source;

import com.example.ulearn.generator.units.CodeUnit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Block implements Comparable<Block> {
    private final int number;
    private List<CodeUnit> codeUnits = new ArrayList<>();

    public Block(int number) {
        this.number = number;
    }

    public void addAllCodeUnits(List<CodeUnit> codeUnitsList) {
        codeUnits.addAll(codeUnitsList);
    }

    public String inRussian() {
        return number + " блок";
    }

    @Override
    public String toString() {
        return "block" + number;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Block && ((Block) obj).number == this.number;
    }


    @Override
    public int compareTo(Block block) {
        return this.number - block.getNumber();
    }
}
