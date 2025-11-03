// src/main/java/com/cinema/testcinema/repository/MerchOrderRepository.java
package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MerchOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchOrderRepository extends JpaRepository<MerchOrder, Long> { }
