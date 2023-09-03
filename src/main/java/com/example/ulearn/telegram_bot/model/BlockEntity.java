package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "blocks")
public class BlockEntity {

    @Id
    private int number;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "block")
    private List<CodeUnitEntity> codeUnits = new ArrayList<>();

    public BlockEntity(int number) {
        this.number = number;
    }

    public void addAllCodeUnits(List<CodeUnitEntity> codeUnitsList) {
        codeUnitsList.forEach(x -> x.setBlock(this));
        codeUnits.addAll(codeUnitsList);
    }

}
