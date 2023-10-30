package com.example.ulearn.telegram_bot.model.repo;

import com.example.ulearn.telegram_bot.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
