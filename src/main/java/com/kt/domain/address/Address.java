package com.kt.domain.address;

import com.kt.common.support.BaseEntity;
import com.kt.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "addresses")
@SQLDelete(sql = "UPDATE addresses SET deleted = true, deleted_at = NOW(), is_default = false WHERE id = ?")
@SQLRestriction("deleted = false")
@NoArgsConstructor
public class Address extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String alias;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String mobile;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(length = 200)
    private String detailAddress;

    @Column(nullable = false, length = 20)
    private String zipcode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    private Address(User user, String alias, String name, String mobile,
					String address, String detailAddress, String zipcode,
					boolean isDefault) {
        this.user = user;
        this.alias = alias;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.detailAddress = detailAddress;
        this.zipcode = zipcode;
        this.isDefault = isDefault;
    }

    public static Address create(User user, String alias, String name, String mobile,
                                 String address, String detailAddress, String zipcode,
                                 boolean isDefault) {
        return new Address(user, alias, name, mobile, address, detailAddress, zipcode, isDefault);
    }

    public void update(String alias, String name, String mobile,
                       String address, String detailAddress, String zipcode,
                       boolean isDefault) {
        this.alias = alias;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.detailAddress = detailAddress;
        this.zipcode = zipcode;
        this.isDefault = isDefault;
    }

    public void setDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

    public void deleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.isDefault = false;
    }
}
