// src/main/java/com/cinema/testcinema/service/MerchService.java
package com.cinema.testcinema.service;

import com.cinema.testcinema.model.MerchOrder;
import com.cinema.testcinema.model.MerchProduct;
import com.cinema.testcinema.repository.MerchOrderRepository;
import com.cinema.testcinema.repository.MerchProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MerchService {
    private final MerchProductRepository products;
    private final MerchOrderRepository orders;

    public MerchService(MerchProductRepository products, MerchOrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    public List<MerchProduct> listActiveProducts() {
        return products.findByActiveTrue();
    }

    public MerchProduct getProductOrThrow(Long id) {
        return products.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    @Transactional
    public MerchOrder createOrder(Long userId, long productId, int qty) {
        MerchProduct p = getProductOrThrow(productId);
        if (p.getStock() < qty) {
            throw new IllegalStateException("Not enough stock");
        }
        p.setStock(p.getStock() - qty);
        products.save(p);

        MerchOrder o = new MerchOrder();
        o.setUserId(userId);
        o.setProductId(Math.toIntExact(productId));
        o.setQty(qty);
        o.setAmountCents(p.getPriceCents() * qty);
        o.setCurrency(p.getCurrency());
        o.setStatus("created");
        o.setCreatedAt(Instant.now());
        return orders.save(o);
    }
}
