package com.kt.service;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.address.Address;
import com.kt.dto.address.AddressRequest;
import com.kt.repository.address.AddressRepository;
import com.kt.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AddressService {
    private static final String DEFAULT_ALIAS = "배송지";
    private static final int MAX_ADDRESS_COUNT = 10;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public Address create(Long userId, AddressRequest request) {
        var user = userRepository.findByIdOrThrow(userId);

        var addresses = addressRepository.findAllByUserIdForUpdate(userId);

        Preconditions.validate(addresses.size() < MAX_ADDRESS_COUNT, ErrorCode.ADDRESS_LIMIT_EXCEEDED);

        boolean isFirst = addresses.isEmpty();
        boolean makeDefault = isFirst || request.isDefault();

        if (makeDefault) {
            unsetDefault(addresses);
        } else {
            ensureDefaultExists(addresses);
        }

        var address = Address.create(
            user,
            normalizeAlias(request.alias()),
            request.name(),
            request.mobile(),
            request.address(),
            request.detailAddress(),
            request.zipcode(),
            makeDefault
        );

        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public List<Address> list(Long userId) {
        return addressRepository.findAllByUserId(userId);
    }

    public Address update(Long userId, Long addressId, AddressRequest request) {
        var addresses = addressRepository.findAllByUserIdForUpdate(userId);

        var target = addresses.stream()
            .filter(a -> a.getId().equals(addressId))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ADDRESS));

        boolean wasDefault = target.isDefault();
        boolean willBeDefault = request.isDefault();

        // 기본 배송지 해제 금지
        Preconditions.validate(!(wasDefault && !willBeDefault), ErrorCode.CANNOT_UNSET_DEFAULT_ADDRESS);

        if (willBeDefault) {
            unsetDefault(addresses);
        } else {
            ensureDefaultExists(addresses);
        }

        target.update(
            normalizeAlias(request.alias()),
            request.name(),
            request.mobile(),
            request.address(),
            request.detailAddress(),
            request.zipcode(),
            willBeDefault
        );

        return target;
    }

    public void delete(Long userId, Long addressId) {
        var addresses = addressRepository.findAllByUserIdForUpdate(userId);

        var target = addresses.stream()
            .filter(a -> a.getId().equals(addressId))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ADDRESS));

        boolean isDefault = target.isDefault();

        if (!isDefault) {
            addressRepository.delete(target);
            return;
        }

        var remaining = addresses.stream()
            .filter(a -> !a.getId().equals(addressId))
            .toList();

        Preconditions.validate(!remaining.isEmpty(), ErrorCode.CANNOT_DELETE_LAST_DEFAULT_ADDRESS);

        // 승격 대상 = 최신 1건
        var promote = remaining.stream()
            .max(Comparator.comparing(Address::getCreatedAt))
            .orElseThrow();

        unsetDefault(remaining);
        promote.setDefault();

        addressRepository.delete(target);
    }

    private String normalizeAlias(String alias) {
        return StringUtils.hasText(alias) ? alias.trim() : DEFAULT_ALIAS;
    }

    private void unsetDefault(List<Address> addresses) {
        addresses.stream()
            .filter(Address::isDefault)
            .forEach(Address::unsetDefault);
    }

    // 기본 배송지가 없으면 최신 배송지를 승격
    private void ensureDefaultExists(List<Address> addresses) {
        if (addresses.isEmpty()) return;

        boolean hasDefault = addresses.stream().anyMatch(Address::isDefault);
        if (hasDefault) return;

        var promote = addresses.stream()
            .max(Comparator.comparing(Address::getCreatedAt))
            .orElseThrow();

        promote.setDefault();
    }
}