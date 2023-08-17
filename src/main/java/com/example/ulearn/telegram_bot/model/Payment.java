package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Payment {
    @Id
    private Integer number_of_order;
    private String id;
    private Long chatId;
    private String blocks;
    private String status;
    private String server_url;
}