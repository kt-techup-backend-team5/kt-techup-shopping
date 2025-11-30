package com.kt.domain.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kt.common.support.BaseEntity;
import com.kt.domain.order.Order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

// 1. domain과 entity를 분리해야
// 2. 굳이? 같이쓰지뭐
@Getter
@Entity
@SQLDelete(sql = "UPDATE user SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
@NoArgsConstructor
public class User extends BaseEntity {
    private String loginId;
    private String password;
    private String name;
    private String email;
    private String mobile;
    // ordinal : enum의 순서를 DB에 저장 => 절대 사용하지마세욤
    // string : enum의 값 DB에 저장
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user")
    private List<Order> orders = new ArrayList<>();

    public User(String loginId, String password, String name, String email, String mobile, Gender gender,
                LocalDate birthday, LocalDateTime createdAt, LocalDateTime updatedAt, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.gender = gender;
        this.birthday = birthday;
        this.updatedAt = updatedAt;
        this.role = role;
    }

    public static User normalUser(String loginId, String password, String name, String email, String mobile,
                                  Gender gender,
                                  LocalDate birthday, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(
                loginId,
                password,
                name,
                email,
                mobile,
                gender,
                birthday,
                createdAt,
                updatedAt,
                Role.USER
        );
    }

    public static User admin(String loginId, String password, String name, String email, String mobile, Gender gender,
                             LocalDate birthday, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(
                loginId,
                password,
                name,
                email,
                mobile,
                gender,
                birthday,
                createdAt,
                updatedAt,
                Role.USER
        );
    }

    public void grantAdminRole() {
        this.role = Role.ADMIN;
        this.updatedAt = LocalDateTime.now();
    }

    public void revokeAdminRole() {
        if (this.role == Role.ADMIN) {
            this.role = Role.USER;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void update(String name, String email, String mobile) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
    }

    @Column(nullable = false)
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    public void deleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void activate() {
        this.deleted = false;
        this.deletedAt = null;
    }
}
