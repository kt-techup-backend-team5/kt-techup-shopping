package com.kt.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductSortType;
import com.kt.domain.product.ProductStatus;
import com.kt.dto.product.ProductCommand;
import com.kt.repository.product.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {
	private final static List<ProductStatus> PUBLIC_VIEWABLE_STATUS = List.of(
			ProductStatus.ACTIVATED,
			ProductStatus.SOLD_OUT);
	private final static List<ProductStatus> NON_DELETED_STATUS = Arrays.stream(ProductStatus.values())
			.filter(status -> !status.equals(ProductStatus.DELETED))
			.toList();

	private final ProductRepository productRepository;
	private final AwsS3Service awsS3Service;

	public void create(ProductCommand.Create command) {
		String thumbnailImgUrl = uploadIfPresent(command.thumbnail());
		String detailImgUrl = uploadIfPresent(command.detail());

		productRepository.save(command.toEntity(thumbnailImgUrl, detailImgUrl));
	}

	public Page<Product> searchPublicStatus(String keyword, ProductSortType sortType, Pageable pageable) {
		String searchKeyword = StringUtils.hasText(keyword) ? keyword : "";
		Pageable sortedPageable = createSortedPageable(pageable, sortType);

		return productRepository.findAllByKeywordAndStatuses(
				searchKeyword,
				PUBLIC_VIEWABLE_STATUS,
				sortedPageable
		);
	}

	public Page<Product> searchNonDeletedStatus(String keyword, ProductSortType sortType, Pageable pageable) {
		String searchKeyword = StringUtils.hasText(keyword) ? keyword : "";
		Pageable sortedPageable = createSortedPageable(pageable, sortType);

		return productRepository.findAllByKeywordAndStatuses(
				searchKeyword,
				NON_DELETED_STATUS,
				sortedPageable
		);
	}

	private Pageable createSortedPageable(Pageable pageable, ProductSortType sortType) {
		return (sortType != null) ?
				PageRequest.of(
						pageable.getPageNumber(),
						pageable.getPageSize(),
						Sort.by(sortType.getDirection(), sortType.getFieldName())
				) : pageable;
	}

	public Product detail(Long id) {
		return productRepository.findByIdOrThrow(id);
	}

	public void update(ProductCommand.Update command) {
		var product = productRepository.findByIdOrThrow(command.id());

		product.update(
				command.data().getName(),
				command.data().getPrice(),
				command.data().getQuantity(),
				command.data().getDescription(),
				updateImage(command.thumbnail(), product.getThumbnailImgUrl()),
				updateImage(command.detail(), product.getDetailImgUrl())
		);
	}

	public void soldOut(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.soldOut();
	}

	public void inActivate(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.inActivate();
	}

	public void activate(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.activate();
	}

	public void delete(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.delete();
	}

	public void decreaseStock(Long id, Long quantity) {
		var product = productRepository.findByIdOrThrow(id);

		product.decreaseStock(quantity);
	}

	public void increaseStock(Long id, Long quantity) {
		var product = productRepository.findByIdOrThrow(id);

		product.increaseStock(quantity);
	}

	public Page<Product> searchLowStock(Long threshold, Pageable pageable) {
		return productRepository.findAllByLowStock(threshold, NON_DELETED_STATUS, pageable);
	}

	private String uploadIfPresent(MultipartFile file) {
		return (file != null && !file.isEmpty()) ? awsS3Service.upload(file) : null;
	}

	private String updateImage(MultipartFile newFile, String currentUrl) {
		if (newFile == null || newFile.isEmpty()) {
			return currentUrl;
		}

		if (StringUtils.hasText(currentUrl)) {
			awsS3Service.delete(currentUrl);
		}

		return awsS3Service.upload(newFile);
	}
}
