package com.kt.dto.address;

import com.kt.domain.address.Address;

import java.time.LocalDateTime;

public record AddressResponse(
    Long id,
    String alias,
    String receiverName,
    String receiverMobile,
    String receiverAddress,
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
            a.getReceiverName(),
            a.getReceiverMobile(),
            a.getReceiverAddress(),
            a.getDetailAddress(),
            a.getZipcode(),
            a.isDefault(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }
}