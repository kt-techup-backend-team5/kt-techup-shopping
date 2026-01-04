package com.kt.repository.address;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.address.Address;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * 사용자 배송지 목록 조회
     * - 기본 배송지 우선
     * - 최신순(createdAt desc)
     */
    @NotNull
    @Query("""
            SELECT a FROM Address a
            WHERE a.user.id = :userId
            ORDER BY CASE WHEN a.isDefault = true THEN 0 ELSE 1 END ASC,
                     a.createdAt DESC
        """)
    List<Address> findAllByUserId(@Param("userId") Long userId);

    /**
     * 동시성 제어: 사용자 배송지 row-lock
     * - create/update/delete에서 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM Address a
            WHERE a.user.id = :userId
        """)
    List<Address> findAllByUserIdForUpdate(@Param("userId") Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userid);

    default Address findByIdAndUserIdOrThrow(Long id, Long userId) {
        return findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ADDRESS));
    }
}
