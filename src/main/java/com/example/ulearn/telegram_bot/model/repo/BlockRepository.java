package com.example.ulearn.telegram_bot.model.repo;

import com.example.ulearn.telegram_bot.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<Block, Integer> {
}
