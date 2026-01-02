package com.kt.config;

import static org.mockito.Mockito.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.awspring.cloud.s3.S3Operations;

@TestConfiguration
public class TestS3Config {

	@Bean
	@Primary
	public S3Operations s3Operations() {
		return mock(S3Operations.class);
	}
}