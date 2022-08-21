package com.example.stock.domain;

import javax.persistence.Version;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long quantity;

    public Stock() {}

    public Stock(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    @Version
    private Long version;

    public Long getQuantity() {
        return quantity;
    }

    public Long decrease(Long quantity) {
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("Not enough stock");
        }

        this.quantity -= quantity;
        return this.quantity;
    }
}
