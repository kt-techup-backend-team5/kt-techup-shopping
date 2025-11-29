package com.kt.dto.review;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewSearchCondition {
    private String productName;
    private String userName;
    private Integer rating;
}
