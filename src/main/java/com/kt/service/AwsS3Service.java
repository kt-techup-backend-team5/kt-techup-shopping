package com.kt.service;

import static com.kt.common.exception.ErrorCode.*;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kt.common.exception.CustomException;
import com.kt.common.support.Preconditions;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AwsS3Service {
	private final S3Operations s3Operations;
	@Value("${s3.bucket}")
	private String bucketName;

	public String upload(MultipartFile file) {
		Preconditions.validate(!file.isEmpty(), INVALID_FILE_ERROR);
		Preconditions.validate(Strings.isNotBlank(file.getOriginalFilename()), INVALID_FILE_ERROR);

		String key = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

		try {
			var resource = s3Operations.upload(bucketName, key, file.getInputStream(),
					ObjectMetadata.builder().contentType(file.getContentType()).build());

			return resource.getURL().toString();

		} catch (IOException e) {
			throw new CustomException(FAIL_UPLOAD_FILE);
		}
	}

	public void delete(String imageUrl) {
		s3Operations.deleteObject(imageUrl);
	}
}
