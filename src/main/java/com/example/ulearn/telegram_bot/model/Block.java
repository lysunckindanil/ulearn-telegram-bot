package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "blocks")
public class Block implements Comparable<Block> {
    public Block(int number) {
        this.number = number;
    }

    @Id
    private int number;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "block")
    private List<CodeUnit> codeUnits = new ArrayList<>();

    public String inRussian() {
        return number + " блок";
    }

    public String inEnglish() {
        return "block" + number;
    }

    public void addAllCodeUnits(List<CodeUnit> codeUnitsList) {
        codeUnitsList.forEach(x -> x.setBlock(this));
        codeUnits.addAll(codeUnitsList);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Block block = (Block) object;
        return number == block.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public int compareTo(Block o) {
        return this.number - o.number;
    }

}
