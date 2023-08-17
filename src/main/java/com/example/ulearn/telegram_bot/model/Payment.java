package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Payment {
    @Id
    private String id;
    private Long chatId;
    private String url;

    private String block;
    private String status;
}
