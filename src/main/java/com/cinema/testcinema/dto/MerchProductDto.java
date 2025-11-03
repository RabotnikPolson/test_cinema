// src/main/java/com/cinema/testcinema/dto/MerchProductDto.java
package com.cinema.testcinema.dto;

public class MerchProductDto {
    private Long id;
    private String slug;
    private String title;
    private Integer priceCents;
    private String currency;
    private String imageUrl;
    private Integer stock;

    public MerchProductDto() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
