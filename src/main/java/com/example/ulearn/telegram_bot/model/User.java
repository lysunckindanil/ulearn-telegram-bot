package com.example.ulearn.telegram_bot.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    private Long chatId;
    private String userName;
    @Column(length = 8192)
    private String files;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Block> blocks = new ArrayList<>();

    public void addBlock(Block block) {
        blocks.add(block);
        Collections.sort(blocks);
    }
}
