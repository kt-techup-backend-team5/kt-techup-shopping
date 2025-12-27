package com.kt.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @Size(max = 20)
    String alias,

    @NotBlank
    @Size(max = 50)
    String receiverName,

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^(0\\d{1,2})-(\\d{3,4})-(\\d{4})$")
    String receiverMobile,

    @NotBlank
    @Size(max = 200)
    String receiverAddress,

    @Size(max = 200)
    String detailAddress,

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^\\d{5}$")
    String zipcode,

    boolean isDefault
) {
}
