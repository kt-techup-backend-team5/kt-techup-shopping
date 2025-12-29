package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.kt.domain.user.CreatedAtSortType;
import com.kt.domain.user.Role;
import com.kt.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceSearchTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 탈퇴_회원_목록_조회_기본정렬() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        when(userRepository.findDeletedUsersDesc(any(), any(), any())).thenReturn(Page.empty());

        // when
        userService.searchCustomers(pageable, null, null, true);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findDeletedUsersDesc(
            argThat(roles -> roles.size() == 1 && roles.contains(Role.CUSTOMER)),
            isNull(),
            captor.capture()
        );
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void 탈퇴_회원_목록_조회_오래된순() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findDeletedUsersAsc(any(), any(), any())).thenReturn(Page.empty());

        // when
        userService.searchCustomers(pageable, null, CreatedAtSortType.OLDEST, true);

        // then
        verify(userRepository).findDeletedUsersAsc(
            argThat(roles -> roles.size() == 1 && roles.contains(Role.CUSTOMER)),
            isNull(),
            any(Pageable.class)
        );
    }
}