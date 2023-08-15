package com.example.ulearn.generator;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Block implements Comparable<Block> {
    private final int position;
    private ArrayList<CodeUnit> codeUnits;

    public Block(int position, ArrayList<CodeUnit> codeUnits) {
        this.position = position;
        this.codeUnits = codeUnits;
    }

    public String inRussian() {
        return position + " блок";
    }

    @Override
    public String toString() {
        return "block" + position;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Block && ((Block) obj).position == this.position;
    }


    @Override
    public int compareTo(Block block) {
        return this.position - block.getPosition();
    }
}
