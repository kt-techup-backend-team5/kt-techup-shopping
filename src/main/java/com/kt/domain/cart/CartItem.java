package com.kt.domain.cart;

import com.kt.domain.cart.exception.InvalidCartQuantityException;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "cart_item",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cart_item_user_product",
            columnNames = {"user_id", "product_id"}
        )
    },
    indexes = {
        @Index(name = "idx_cart_item_user_updated_at", columnList = "user_id, updated_at")
    }
)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CartItem create(User user, Product product, Long quantity) {
        validateQuantity(quantity);

        var cartItem = new CartItem();
        cartItem.user = user;
        cartItem.product = product;
        cartItem.quantity = quantity;

        var now = LocalDateTime.now();
        cartItem.createdAt = now;
        cartItem.updatedAt = now;

        return cartItem;
    }

    public void changeQuantity(Long quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
        touch();
    }

    private static void validateQuantity(Long quantity) {
        if (quantity < 1) {
            throw new InvalidCartQuantityException(quantity);
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}