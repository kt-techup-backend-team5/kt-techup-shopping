package com.kt.domain.wishlist;

import com.kt.common.support.BaseEntity;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "wishlist",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_wishlist_user_product",
            columnNames = {"user_id", "product_id"}
        )
    }
)
public class Wishlist extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public static Wishlist create(User user, Product product) {
        var wishlist = new Wishlist();
        wishlist.user = user;
        wishlist.product = product;
        return wishlist;
    }
}