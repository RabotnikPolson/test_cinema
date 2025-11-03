// src/main/java/com/cinema/testcinema/repository/MerchProductRepository.java
package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MerchProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchProductRepository extends JpaRepository<MerchProduct, Long> {
    List<MerchProduct> findByActiveTrue();
}
