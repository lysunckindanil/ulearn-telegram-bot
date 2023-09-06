package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment implements Serializable {
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

    @Override
    public String toString() {
        return "Payment{" +
                "id='" + id + '\'' +
                ", chatId=" + chatId +
                ", blocks='" + blocks + '\'' +
                ", date=" + date +
                '}';
    }
}