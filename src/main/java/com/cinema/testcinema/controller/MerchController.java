// src/main/java/com/cinema/testcinema/controller/MerchController.java
package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.MerchProductDto;
import com.cinema.testcinema.model.MerchOrder;
import com.cinema.testcinema.model.MerchProduct;
import com.cinema.testcinema.service.MerchService;
import com.cinema.testcinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merch")
public class MerchController {

    private final MerchService merchService;
    private final UserService userService;

    public MerchController(MerchService merchService, UserService userService) {
        this.merchService = merchService;
        this.userService = userService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<MerchProductDto>> listProducts() {
        List<MerchProduct> list = merchService.listActiveProducts();
        List<MerchProductDto> dtos = list.stream().map(p -> {
            MerchProductDto d = new MerchProductDto();
            d.setId(p.getId());
            d.setSlug(p.getSlug());
            d.setTitle(p.getTitle());
            d.setPriceCents(p.getPriceCents());
            d.setCurrency(p.getCurrency());
            d.setImageUrl(p.getImageUrl());
            d.setStock(p.getStock());
            return d;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/orders")
    public ResponseEntity<MerchOrder> createOrder(
            @RequestParam String username,
            @RequestBody CreateOrderRequest req
    ) {
        Long uid = userService.findIdByUsername(username);
        MerchOrder order = merchService.createOrder(uid, req.getProductId(), req.getQty());
        return ResponseEntity.ok(order);
    }

    public static class CreateOrderRequest {
        private long productId;
        private int qty;

        public CreateOrderRequest() { }

        public long getProductId() { return productId; }
        public void setProductId(long productId) { this.productId = productId; }

        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
    }
}
