// src/main/java/com/cinema/testcinema/repository/SubscriptionRepository.java
package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
}
