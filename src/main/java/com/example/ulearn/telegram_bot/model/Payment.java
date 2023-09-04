package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

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
    private Date date;
    private String status;
    @Column(length = 16384)
    private String message;
}