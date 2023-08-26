package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private Integer number_of_order;
    private String id;
    private Long chatId;
    private String blocks;
    private String server_url;
    private String date;
    private String status;
}