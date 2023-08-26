package com.example.ulearn.telegram_bot.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long chatId;
    private String userName;
    @Column(length = 8096)
    private String files;
    @Column(length = 8096)
    private String blocks;

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", userName='" + userName + '\'' +
                '}';
    }
}
