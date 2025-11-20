package com.kt.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Receiver {
	// receiver_name
	@Column(name = "receiver_name")
	private String name;
	// receiver_address
	@Column(name = "receiver_address")
	private String address;
	// receiver_mobile
	@Column(name = "receiver_mobile")
	private String mobile;
}
