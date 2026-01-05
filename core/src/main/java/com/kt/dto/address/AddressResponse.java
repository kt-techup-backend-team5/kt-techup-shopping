package com.kt.dto.address;

import com.kt.domain.address.Address;

import java.time.LocalDateTime;

public record AddressResponse(
    Long id,
    String alias,
    String name,
    String mobile,
    String address,
    String detailAddress,
    String zipcode,
    boolean isDefault,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
            a.getId(),
            a.getAlias(),
            a.getName(),
            a.getMobile(),
            a.getAddress(),
            a.getDetailAddress(),
            a.getZipcode(),
            a.isDefault(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }
}