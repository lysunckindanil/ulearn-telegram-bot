package com.example.ulearn.telegram_bot.model.repo;

import com.example.ulearn.telegram_bot.model.Payment;
import org.springframework.data.repository.CrudRepository;

public interface PaymentRepository extends CrudRepository<Payment, Integer> {
}
