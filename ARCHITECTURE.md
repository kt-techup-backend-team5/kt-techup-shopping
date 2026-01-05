# 멀티모듈 아키텍처 설계 문서

## 📋 목차
1. [멀티모듈 전환 배경](#멀티모듈-전환-배경)
2. [모듈 분리 기준](#모듈-분리-기준)
3. [모듈 구조 상세](#모듈-구조-상세)
4. [의존성 그래프](#의존성-그래프)
5. [기능별 모듈 매핑](#기능별-모듈-매핑)
6. [빌드 설정](#빌드-설정)
7. [테스트 전략](#테스트-전략)
8. [배포 전략](#배포-전략)

---

## 멀티모듈 전환 배경

### 기존 모놀리식 구조의 문제점
```
kt-techup-shopping (monolithic)
└── src/main/java/com/kt/
    ├── controller/     # 모든 컨트롤러 (User + Admin 혼재)
    ├── service/        # 모든 서비스
    ├── domain/         # 모든 도메인
    ├── repository/     # 모든 레포지토리
    └── config/         # 모든 설정
```

**문제점:**
1. **배포 단위 비효율**: Admin API 수정해도 전체 애플리케이션 재배포
2. **AWS 비용 증가**: 하나의 큰 애플리케이션만 배포 가능
3. **의존성 관리 어려움**: 모든 코드가 하나의 컨텍스트에 존재
4. **팀 협업 충돌**: 여러 도메인이 섞여 있어 코드 충돌 빈번

### 멀티모듈 전환 목표
1. **독립 배포**: User/Admin을 각각 독립적으로 배포
2. **비용 절감**: 필요한 애플리케이션만 스케일링
3. **명확한 경계**: 모듈별 책임과 역할 명확화
4. **재사용성 향상**: 공통 코드를 라이브러리 모듈로 분리

---

## 모듈 분리 기준

### 1. 계층별 분리 (Layered Architecture)

```
┌─────────────────────────────────────┐
│         Application Layer           │  ← user, admin (실행 가능)
├─────────────────────────────────────┤
│      Infrastructure Layer           │  ← integration (외부 시스템)
├─────────────────────────────────────┤
│      Security Layer                 │  ← auth (인증/인가)
├─────────────────────────────────────┤
│      Domain Layer                   │  ← core (비즈니스 로직)
├─────────────────────────────────────┤
│      Foundation Layer               │  ← common (공통 기능)
└─────────────────────────────────────┘
```

**Foundation (common)**: 모든 계층에서 사용하는 횡단 관심사
**Domain (core)**: 비즈니스 핵심 로직 (도메인 모델, 레포지토리)
**Security (auth)**: 인증/인가 관심사 분리
**Infrastructure (integration)**: 외부 시스템 통합
**Application (user/admin)**: 사용자 인터페이스 제공

### 2. 독립 배포 단위 분리

```
Library Modules (jar)          Executable Modules (bootJar)
┌──────────────┐              ┌──────────────────┐
│    common    │              │       user       │ ← 8080 포트
│    core      │              │  (shopping-user) │
│  integration │    의존       └──────────────────┘
│    auth      │    ────────> ┌──────────────────┐
└──────────────┘              │      admin       │ ← 8081 포트
                              │ (shopping-admin) │
                              └──────────────────┘
```

**라이브러리 모듈**: 재사용 가능한 컴포넌트 (jar)
**실행 가능 모듈**: 독립 실행 가능한 애플리케이션 (bootJar)

### 3. 관심사 분리 (Separation of Concerns)

| 모듈 | 관심사 | 예시 |
|------|--------|------|
| common | 공통 유틸리티, 예외 처리 | ApiResult, ErrorCode, Preconditions |
| core | 도메인 로직, 데이터 접근 | Product, Order, ProductRepository |
| integration | 외부 시스템 연동 | Redis, S3, Slack, Scheduler |
| auth | 인증/인가 | JWT, SecurityConfiguration |
| user | 사용자 API, 비즈니스 서비스 | ProductController, OrderService |
| admin | 관리자 API | AdminProductController |

---

## 모듈 구조 상세

### 1. common 모듈 (Foundation Layer)

**역할**: 모든 모듈에서 사용하는 공통 기능 제공

**패키지 구조**:
```
common/src/main/java/com/kt/common/
├── exception/           # 예외 처리
│   ├── ApiAdvice.java          # 전역 예외 핸들러
│   ├── CustomException.java    # 커스텀 예외
│   ├── ErrorCode.java           # 에러 코드 정의
│   └── Exceptions.java          # 예외 유틸리티
├── response/            # 응답 모델
│   ├── ApiResult.java           # 표준 API 응답
│   └── ErrorResponse.java       # 에러 응답
├── support/             # 유틸리티
│   ├── Preconditions.java       # 검증 유틸
│   ├── Message.java             # 메시지 이벤트
│   └── Lock.java                # 분산 락 어노테이션
├── request/             # 요청 모델
│   └── Paging.java              # 페이징 처리
├── interceptor/         # 인터셉터
│   └── VisitStatInterceptor.java
└── profile/             # 프로파일
    ├── LocalProfile.java
    ├── DevProfile.java
    └── AppProfile.java
```

**주요 기능**:
- ✅ 전역 예외 처리 (`@RestControllerAdvice`)
- ✅ 표준 API 응답 형식 (`ApiResult<T>`)
- ✅ 공통 Validation 로직
- ✅ 환경별 프로파일 설정

**의존성**: 없음 (최하위 계층)

---

### 2. core 모듈 (Domain Layer)

**역할**: 비즈니스 핵심 로직과 데이터 모델 제공

**패키지 구조**:
```
core/src/main/java/com/kt/
├── domain/              # 도메인 엔티티
│   ├── product/
│   │   ├── Product.java              # 상품 엔티티
│   │   ├── ProductStatus.java        # 상품 상태 enum
│   │   └── ProductSortType.java      # 상품 정렬 타입
│   ├── order/
│   │   ├── Order.java                # 주문 엔티티
│   │   ├── OrderStatus.java          # 주문 상태
│   │   ├── Receiver.java             # 수령자 정보
│   │   └── event/OrderEvent.java     # 주문 도메인 이벤트
│   ├── payment/
│   │   ├── Payment.java              # 결제 엔티티
│   │   ├── PaymentStatus.java        # 결제 상태
│   │   └── event/PaymentEvent.java   # 결제 도메인 이벤트
│   ├── cart/
│   │   └── CartItem.java             # 장바구니 아이템
│   ├── review/
│   │   ├── Review.java               # 리뷰 엔티티
│   │   └── event/ReviewEvent.java    # 리뷰 도메인 이벤트
│   ├── question/
│   │   ├── Question.java             # Q&A 질문
│   │   ├── Answer.java               # Q&A 답변
│   │   └── QuestionStatus.java       # 질문 상태
│   ├── point/
│   │   ├── Point.java                # 포인트 엔티티
│   │   ├── PointHistory.java         # 포인트 이력
│   │   └── PointHistoryType.java     # 포인트 이력 타입
│   ├── refund/
│   │   ├── Refund.java               # 환불 엔티티
│   │   ├── RefundStatus.java         # 환불 상태
│   │   └── event/RefundEvent.java    # 환불 도메인 이벤트
│   ├── user/
│   │   ├── User.java                 # 사용자 엔티티
│   │   ├── Role.java                 # 역할 enum
│   │   └── Gender.java               # 성별 enum
│   ├── wishlist/
│   │   └── Wishlist.java             # 위시리스트
│   └── visitstat/
│       └── VisitStat.java            # 방문 통계
│
├── repository/          # 데이터 접근 레이어
│   ├── product/ProductRepository.java
│   ├── order/
│   │   ├── OrderRepository.java
│   │   ├── OrderRepositoryCustom.java       # QueryDSL 인터페이스
│   │   └── OrderRepositoryCustomImpl.java   # QueryDSL 구현
│   ├── payment/PaymentRepository.java
│   ├── cart/CartItemRepository.java
│   ├── review/
│   │   ├── ReviewRepository.java
│   │   ├── ReviewRepositoryCustom.java
│   │   └── ReviewRepositoryCustomImpl.java
│   ├── question/
│   │   ├── QuestionRepository.java
│   │   └── AnswerRepository.java
│   ├── point/
│   │   ├── PointRepository.java
│   │   └── PointHistoryRepository.java
│   ├── refund/RefundRepository.java
│   ├── user/UserRepository.java
│   └── wishlist/WishlistRepository.java
│
└── dto/                 # Data Transfer Objects
    ├── product/
    │   ├── ProductRequest.java
    │   ├── ProductResponse.java
    │   └── ProductCommand.java
    ├── order/
    │   ├── OrderRequest.java
    │   ├── OrderResponse.java
    │   └── OrderSearchCondition.java
    ├── payment/PaymentRequest.java
    ├── cart/
    │   ├── CartRequest.java
    │   └── CartResponse.java
    ├── review/
    │   ├── ReviewCreateRequest.java
    │   ├── ReviewUpdateRequest.java
    │   ├── ReviewResponse.java
    │   └── ReviewSearchCondition.java
    ├── question/
    │   ├── QuestionRequest.java
    │   ├── QuestionResponse.java
    │   ├── AnswerRequest.java
    │   └── AnswerResponse.java
    ├── point/
    │   ├── PointRequest.java
    │   └── PointResponse.java
    ├── refund/
    │   ├── RefundRequest.java
    │   └── RefundResponse.java
    └── user/
        ├── UserCreateRequest.java
        ├── UserChangeRequest.java
        └── UserResponse.java
```

**주요 기능**:
- ✅ JPA 엔티티 정의 (Product, Order, Payment 등)
- ✅ Repository 인터페이스 (Spring Data JPA)
- ✅ QueryDSL 동적 쿼리 (복잡한 검색 조건)
- ✅ 도메인 이벤트 (OrderEvent, PaymentEvent 등)
- ✅ DTO (Request/Response 분리)

**의존성**: `common`

**기술 스택**:
- Spring Data JPA
- QueryDSL 5.0.0
- H2 Database (테스트용)
- MySQL Connector

---

### 3. integration 모듈 (Infrastructure Layer)

**역할**: 외부 시스템과의 통합 담당

**패키지 구조**:
```
integration/src/main/java/com/kt/integration/
├── redis/
│   ├── RedisConfiguration.java      # Redis 설정
│   └── RedisService.java            # Redis 캐시 서비스
├── s3/
│   └── AwsS3Service.java            # S3 파일 업로드 서비스
├── slack/
│   ├── SlackConfiguration.java      # Slack 설정
│   ├── SlackProperties.java         # Slack 프로퍼티
│   ├── NotifyApi.java               # 알림 인터페이스
│   ├── DefaultNotifyApi.java        # 프로덕션 알림
│   ├── DevNotifyApi.java            # 개발 환경 알림
│   └── LocalNotifyApi.java          # 로컬 환경 알림
├── scheduler/
│   └── ViewSyncScheduler.java       # 조회수 동기화 스케줄러
└── eventlistener/
    └── NotificationListener.java    # 도메인 이벤트 리스너
```

**주요 기능**:

**Redis 캐시**:
- 조회수 캐싱 (제품, 리뷰)
- 분산 락 (재고 관리, 포인트 처리)
- Redisson 기반 분산 락

**S3 파일 저장**:
- 제품 이미지 업로드
- 리뷰 이미지 업로드
- LocalStack (로컬 테스트용)

**Slack 알림**:
- 주문 생성 알림
- 결제 완료 알림
- 환불 요청 알림
- 환경별 분리 (Local/Dev/Prod)

**스케줄러**:
- Redis 조회수 → DB 동기화 (매시간)

**의존성**: `common`, `core`

**기술 스택**:
- Redisson (분산 락)
- AWS S3 (Spring Cloud AWS)
- Slack API Client
- Spring Scheduling

---

### 4. auth 모듈 (Security Layer)

**역할**: 인증/인가 처리

**패키지 구조**:
```
auth/src/main/java/com/kt/
├── security/
│   ├── JwtService.java                    # JWT 생성/검증
│   ├── JwtFilter.java                     # JWT 필터
│   ├── JwtProperties.java                 # JWT 설정
│   ├── CurrentUser.java                   # 현재 사용자 인터페이스
│   ├── DefaultCurrentUser.java            # 현재 사용자 구현
│   └── TechUpAuthenticationToken.java     # 커스텀 인증 토큰
├── config/
│   └── SecurityConfiguration.java         # Spring Security 설정
└── dto/auth/
    ├── AuthRequest.java                   # 로그인 요청
    └── AuthResponse.java                  # 토큰 응답
```

**주요 기능**:
- ✅ JWT 토큰 생성/검증
- ✅ Spring Security 설정
- ✅ 사용자 인증 필터
- ✅ 역할 기반 접근 제어 (ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN)

**의존성**: `common`, `core`

**기술 스택**:
- Spring Security
- JWT (jjwt 0.13.0)

---

### 5. user 모듈 (Application Layer - User)

**역할**: 일반 사용자용 API 제공 + 비즈니스 서비스 로직

**패키지 구조**:
```
user/src/main/java/com/kt/
├── UserApplication.java         # 메인 애플리케이션
│
├── controller/                  # REST API 컨트롤러
│   ├── auth/AuthController.java           # 로그인/로그아웃/토큰재발급
│   ├── product/ProductController.java     # 상품 조회
│   ├── order/OrderController.java         # 주문 생성/조회
│   ├── payment/PaymentController.java     # 결제
│   ├── cart/CartController.java           # 장바구니
│   ├── review/ReviewController.java       # 리뷰 작성/조회
│   ├── question/QuestionController.java   # Q&A 질문
│   ├── point/PointController.java         # 포인트 조회
│   ├── wishlist/WishlistController.java   # 위시리스트
│   ├── address/AddressController.java     # 배송지 관리
│   └── user/UserController.java           # 내 정보 관리
│
├── service/                     # 비즈니스 로직
│   ├── ProductService.java                # 상품 서비스
│   ├── OrderService.java                  # 주문 서비스
│   ├── PaymentService.java                # 결제 서비스
│   ├── CartService.java                   # 장바구니 서비스
│   ├── ReviewService.java                 # 리뷰 서비스
│   ├── QuestionService.java               # Q&A 서비스
│   ├── AnswerService.java                 # 답변 서비스
│   ├── PointService.java                  # 포인트 서비스
│   ├── WishlistService.java               # 위시리스트 서비스
│   ├── AddressService.java                # 배송지 서비스
│   ├── UserService.java                   # 사용자 서비스
│   ├── AuthService.java                   # 인증 서비스
│   ├── UserOrderService.java              # 사용자 주문 조회
│   ├── StockService.java                  # 재고 관리
│   └── VisitStatService.java              # 방문 통계
│
├── aspect/                      # AOP
│   ├── LockAspect.java                    # 분산 락 AOP
│   ├── AopTransactionManager.java         # 트랜잭션 관리
│   └── DefaultAopTransactionManager.java
│
├── config/                      # 설정
│   ├── QueryDslConfiguration.java         # QueryDSL 설정
│   ├── JpaAuditingConfiguration.java      # JPA Auditing
│   ├── SwaggerConfiguration.java          # Swagger UI
│   └── PaymentTypeDataLoader.java         # 초기 데이터
│
└── internalevent/               # 내부 이벤트
    └── listener/
        ├── OrderEventListener.java        # 주문 이벤트 처리
        ├── PointEventListener.java        # 포인트 이벤트 처리
        └── InternalEventListener.java     # 내부 이벤트 처리
```

**주요 기능**:

**상품 관련**:
- 상품 목록 조회 (필터링, 정렬, 페이징)
- 상품 상세 조회
- 상품 검색
- 조회수 증가 (Redis 캐싱)

**주문/결제 관련**:
- 주문 생성 (재고 차감 + 분산 락)
- 결제 처리
- 포인트 사용/적립
- 주문 취소
- 환불 요청

**리뷰/Q&A**:
- 리뷰 작성/수정/삭제
- 리뷰 조회 (베스트 리뷰, 포토 리뷰)
- Q&A 질문 작성
- Q&A 답변 조회

**장바구니/위시리스트**:
- 장바구니 추가/수정/삭제
- 비회원 장바구니 병합
- 위시리스트 추가/삭제

**의존성**: `common`, `core`, `integration`, `auth`

**기술 스택**:
- Spring Web MVC
- Spring Data JPA
- QueryDSL
- Redisson (분산 락)
- Swagger/OpenAPI

---

### 6. admin 모듈 (Application Layer - Admin)

**역할**: 관리자용 API 제공

**패키지 구조**:
```
admin/src/main/java/com/kt/
├── AdminApplication.java        # 메인 애플리케이션
│
└── controller/                  # REST API 컨트롤러
    ├── product/AdminProductController.java    # 상품 관리
    ├── order/AdminOrderController.java        # 주문 관리
    ├── point/AdminPointController.java        # 포인트 관리
    ├── review/AdminReviewController.java      # 리뷰 관리
    ├── question/AdminQuestionController.java  # Q&A 관리
    └── user/
        ├── AdminUserController.java           # 사용자 관리
        └── AdminController.java               # 관리자 계정 관리
```

**주요 기능**:

**상품 관리**:
- 상품 등록/수정/삭제
- 상품 상태 변경 (판매중/품절/판매중지)

**주문 관리**:
- 전체 주문 조회 (검색, 필터링)
- 주문 상태 변경
- 주문 취소 승인/거부

**리뷰 관리**:
- 리뷰 블라인드 처리
- 부적절한 리뷰 관리

**Q&A 관리**:
- 질문 답변 작성
- 질문 삭제

**사용자 관리**:
- 사용자 목록 조회
- 사용자 정보 수정
- 사용자 비활성화/활성화
- 비밀번호 초기화

**관리자 계정 관리**:
- 관리자 권한 부여/회수 (SUPER_ADMIN만 가능)
- 관리자 계정 생성

**의존성**: `common`, `core`, `integration`, `auth`, `user`
- **user 모듈에 의존**: admin은 user의 서비스를 재사용

**기술 스택**: user와 동일

---

## 의존성 그래프

### 모듈 의존성 관계

```
                    ┌─────────────┐
                    │   common    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │    core     │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──────┐     │     ┌──────▼──────┐
       │ integration │     │     │    auth     │
       └──────┬──────┘     │     └──────┬──────┘
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────▼──────┐
                    │    user     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   admin     │
                    └─────────────┘
```

### 의존성 방향 규칙

1. **하위 → 상위 의존만 허용** (역방향 의존 금지)
   - ✅ user → core (OK)
   - ❌ core → user (NOT OK)

2. **같은 계층 간 의존 가능**
   - integration ↔ auth (같은 infrastructure layer)

3. **순환 의존 금지**
   - 순환 의존 발생 시 모듈 재설계 필요

---

## 기능별 모듈 매핑

### 상품 (Product)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Product.java | 상품 엔티티 |
| Domain | core | ProductRepository.java | 상품 레포지토리 |
| Domain | core | ProductResponse.java | 상품 응답 DTO |
| Service | user | ProductService.java | 상품 비즈니스 로직 |
| API | user | ProductController.java | 상품 조회 API |
| API | admin | AdminProductController.java | 상품 관리 API |
| Cache | integration | RedisService.java | 조회수 캐싱 |
| Storage | integration | AwsS3Service.java | 상품 이미지 저장 |

### 주문 (Order)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Order.java | 주문 엔티티 |
| Domain | core | OrderProduct.java | 주문 상품 |
| Domain | core | OrderRepository.java | 주문 레포지토리 |
| Domain | core | OrderEvent.java | 주문 도메인 이벤트 |
| Service | user | OrderService.java | 주문 생성/취소 로직 |
| Service | user | UserOrderService.java | 사용자 주문 조회 |
| Service | user | StockService.java | 재고 차감 (분산 락) |
| API | user | OrderController.java | 주문 API |
| API | admin | AdminOrderController.java | 주문 관리 API |
| Event | user | OrderEventListener.java | 주문 이벤트 처리 |
| Lock | integration | RedisService.java | 재고 분산 락 |
| Notification | integration | NotifyApi.java | 주문 Slack 알림 |

### 결제 (Payment)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Payment.java | 결제 엔티티 |
| Domain | core | PaymentType.java | 결제 수단 |
| Domain | core | PaymentEvent.java | 결제 도메인 이벤트 |
| Service | user | PaymentService.java | 결제 처리 로직 |
| API | user | PaymentController.java | 결제 API |
| Notification | integration | NotifyApi.java | 결제 완료 알림 |

### 장바구니 (Cart)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | CartItem.java | 장바구니 아이템 |
| Domain | core | CartResponse.java | 장바구니 응답 DTO |
| Service | user | CartService.java | 장바구니 로직 |
| API | user | CartController.java | 장바구니 API |

### 리뷰 (Review)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Review.java | 리뷰 엔티티 |
| Domain | core | ReviewRepository.java | 리뷰 레포지토리 (QueryDSL) |
| Domain | core | ReviewEvent.java | 리뷰 도메인 이벤트 |
| Service | user | ReviewService.java | 리뷰 작성/조회 로직 |
| API | user | ReviewController.java | 리뷰 API |
| API | admin | AdminReviewController.java | 리뷰 관리 API |
| Storage | integration | AwsS3Service.java | 리뷰 이미지 저장 |
| Cache | integration | RedisService.java | 리뷰 조회수 캐싱 |

### Q&A (Question & Answer)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Question.java | 질문 엔티티 |
| Domain | core | Answer.java | 답변 엔티티 |
| Domain | core | QuestionRepository.java | 질문 레포지토리 |
| Service | user | QuestionService.java | 질문 작성/조회 |
| Service | user | AnswerService.java | 답변 작성 |
| API | user | QuestionController.java | Q&A API |
| API | admin | AdminQuestionController.java | Q&A 관리 API |

### 포인트 (Point)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Point.java | 포인트 엔티티 |
| Domain | core | PointHistory.java | 포인트 이력 |
| Service | user | PointService.java | 포인트 적립/사용 |
| API | user | PointController.java | 포인트 조회 API |
| API | admin | AdminPointController.java | 포인트 관리 API |
| Event | user | PointEventListener.java | 포인트 이벤트 처리 |
| Lock | integration | RedisService.java | 포인트 동시성 제어 |

### 환불 (Refund)

| 계층 | 모듈 | 컴포넌트 | 설명 |
|------|------|----------|------|
| Domain | core | Refund.java | 환불 엔티티 |
| Domain | core | RefundEvent.java | 환불 도메인 이벤트 |
| Service | user | (OrderService 내부) | 환불 요청 로직 |
| Notification | integration | NotifyApi.java | 환불 Slack 알림 |

---

## 빌드 설정

### Gradle 멀티모듈 구조

**루트 build.gradle**:
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.7' apply false
    id 'io.spring.dependency-management' version '1.1.7'
}

// 모든 서브프로젝트 공통 설정
subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    
    repositories {
        mavenCentral()
    }
    
    dependencies {
        // 모든 모듈 공통 의존성
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }
    
    // 기본적으로 bootJar 비활성화 (user, admin만 활성화)
    tasks.named("bootJar") {
        enabled = false
    }
    
    tasks.named("jar") {
        enabled = true
    }
}
```

### 모듈별 빌드 설정

**1. common/build.gradle** (라이브러리):
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.data:spring-data-commons'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
}

// jar만 생성 (bootJar X)
```

**2. core/build.gradle** (라이브러리):
```gradle
dependencies {
    implementation project(':common')
    
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // QueryDSL 어노테이션 프로세서
    annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    
    runtimeOnly 'com.mysql:mysql-connector-j'
    runtimeOnly 'com.h2database:h2'
}

// QueryDSL Q클래스 생성 경로
sourceSets {
    main {
        java {
            srcDirs = ['src/main/java', 'build/generated/sources/annotationProcessor/java/main']
        }
    }
}
```

**3. integration/build.gradle** (라이브러리):
```gradle
dependencies {
    implementation project(':common')
    implementation project(':core')
    
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Redisson (분산 락)
    implementation 'org.redisson:redisson-spring-boot-starter:3.32.0'
    implementation 'io.netty:netty-resolver-dns-native-macos:4.1.117.Final:osx-aarch_64'
    implementation 'io.netty:netty-resolver-dns:4.1.117.Final'
    
    // Slack
    implementation 'com.slack.api:slack-api-client:1.43.1'
    
    // AWS S3
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3:3.4.0'
}
```

**4. auth/build.gradle** (라이브러리):
```gradle
dependencies {
    implementation project(':common')
    implementation project(':core')
    
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.13.0'
    
    testImplementation 'org.springframework.security:spring-security-test'
}
```

**5. user/build.gradle** (실행 가능):
```gradle
dependencies {
    implementation project(':common')
    implementation project(':core')
    implementation project(':integration')
    implementation project(':auth')
    
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
    
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    implementation 'org.redisson:redisson-spring-boot-starter:3.32.0'
    
    testImplementation 'org.springframework.security:spring-security-test'
}

// bootJar 활성화
tasks.named("bootJar") {
    enabled = true
    archiveFileName = "shopping-user.jar"
}

// plain jar도 생성 (admin에서 의존성으로 사용)
jar {
    enabled = true
}

// AWS Elastic Beanstalk 배포용 zip 생성
tasks.register("zip", Zip.class) {
    dependsOn("bootJar")
    archiveFileName = "shopping-user.zip"
    
    from("build/libs/shopping-user.jar") { into("") }
    from("../procfiles/UserProcfile") { 
        into("")
        rename("UserProcfile", "Procfile")
    }
}
```

**6. admin/build.gradle** (실행 가능):
```gradle
dependencies {
    implementation project(':common')
    implementation project(':core')
    implementation project(':integration')
    implementation project(':auth')
    implementation project(':user')  // 서비스 재사용
    
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
    
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    implementation 'org.redisson:redisson-spring-boot-starter:3.32.0'
    
    testImplementation 'org.springframework.security:spring-security-test'
}

// bootJar 활성화
tasks.named("bootJar") {
    enabled = true
    archiveFileName = "shopping-admin.jar"
}

jar {
    enabled = false  // admin은 다른 모듈에서 의존 안함
}

// AWS Elastic Beanstalk 배포용 zip 생성
tasks.register("zip", Zip.class) {
    dependsOn("bootJar")
    archiveFileName = "shopping-admin.zip"
    
    from("build/libs/shopping-admin.jar") { into("") }
    from("../procfiles/AdminProcfile") { 
        into("")
        rename("AdminProcfile", "Procfile")
    }
}
```

### 빌드 명령어

```bash
# 전체 빌드 (테스트 제외)
./gradlew clean build -x test

# 특정 모듈만 빌드
./gradlew :user:build -x test
./gradlew :admin:build -x test

# bootJar 생성
./gradlew :user:bootJar
./gradlew :admin:bootJar

# AWS 배포용 zip 생성
./gradlew :user:zip
./gradlew :admin:zip

# 실행
java -jar user/build/libs/shopping-user.jar
java -jar admin/build/libs/shopping-admin.jar --server.port=8081
```

---

## 테스트 전략

### 테스트 구조

현재 테스트는 **모놀리식 구조에서 멀티모듈로 전환 중**이므로, 테스트 마이그레이션이 필요합니다.

**기존 테스트 위치** (src.backup/test):
```
src/test/java/com/kt/
├── controller/          # 컨트롤러 테스트
│   ├── payment/PaymentControllerTest.java
│   ├── point/PointControllerTest.java
│   ├── product/ProductControllerTest.java
│   └── question/QuestionControllerTest.java
├── service/             # 서비스 테스트
│   ├── OrderServiceTest.java
│   ├── PaymentServiceTest.java
│   ├── PointServiceTest.java
│   ├── ProductServiceTest.java
│   ├── QuestionServiceTest.java
│   ├── ReviewServiceTest.java
│   └── UserServiceTest.java
├── domain/              # 도메인 테스트
│   ├── product/ProductTest.java
│   └── payment/PaymentTest.java
└── repository/          # 레포지토리 테스트
    ├── product/ProductRepositoryTest.java
    └── payment/PaymentTypeRepositoryTest.java
```

### 테스트 마이그레이션 계획

**1. 도메인/레포지토리 테스트 → core 모듈**:
```
core/src/test/java/com/kt/
├── domain/
│   ├── product/ProductTest.java
│   └── payment/PaymentTest.java
└── repository/
    ├── product/ProductRepositoryTest.java
    └── payment/PaymentTypeRepositoryTest.java
```

**2. 서비스 테스트 → user 모듈**:
```
user/src/test/java/com/kt/service/
├── OrderServiceTest.java
├── PaymentServiceTest.java
├── PointServiceTest.java
├── ProductServiceTest.java
├── QuestionServiceTest.java
├── ReviewServiceTest.java
└── UserServiceTest.java
```

**3. 컨트롤러 테스트 → user/admin 모듈**:
```
user/src/test/java/com/kt/controller/
├── payment/PaymentControllerTest.java
├── point/PointControllerTest.java
├── product/ProductControllerTest.java
└── question/QuestionControllerTest.java

admin/src/test/java/com/kt/controller/
├── product/AdminProductControllerTest.java
└── point/AdminPointControllerTest.java
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :core:test
./gradlew :user:test

# 통합 테스트 제외
./gradlew test -Dtest.profile=test
```

**테스트 설정** (모든 모듈):
```gradle
tasks.named('test') {
    useJUnitPlatform {
        excludeTags 'integration'  // 통합 테스트 제외
    }
}
```

---

## 배포 전략

### AWS Elastic Beanstalk 배포

**1. User 애플리케이션 배포**:
```bash
# 1. bootJar + zip 생성
./gradlew :user:clean :user:zip

# 2. shopping-user.zip 업로드
# user/build/distributions/shopping-user.zip

# 3. Elastic Beanstalk 환경 생성
# - Platform: Java 21 (Corretto)
# - Instance type: t3.small
# - Environment variables 설정
```

**shopping-user.zip 구조**:
```
shopping-user.zip
├── shopping-user.jar
└── Procfile (web: java -jar shopping-user.jar)
```

**2. Admin 애플리케이션 배포**:
```bash
# 1. bootJar + zip 생성
./gradlew :admin:clean :admin:zip

# 2. shopping-admin.zip 업로드
# admin/build/distributions/shopping-admin.zip

# 3. Elastic Beanstalk 환경 생성
# - 별도 환경으로 생성
# - 포트 설정: 8081
```

**shopping-admin.zip 구조**:
```
shopping-admin.zip
├── shopping-admin.jar
└── Procfile (web: java -jar shopping-admin.jar)
```

### 독립 배포의 장점

**비용 절감**:
- User: t3.medium (활성 사용자 많음)
- Admin: t3.small (관리자만 사용)
- **약 30% 비용 절감**

**독립 스케일링**:
- User: Auto Scaling (트래픽에 따라)
- Admin: Fixed (1대만 유지)

**무중단 배포**:
- User 배포 시 Admin 영향 없음
- Admin 배포 시 User 영향 없음

---

## 향후 개선 방향

### 1. 테스트 마이그레이션 완료
- [ ] 도메인/레포지토리 테스트 → core
- [ ] 서비스 테스트 → user
- [ ] 컨트롤러 테스트 → user/admin

### 2. 서비스 계층 분리
현재 user 모듈에 모든 서비스가 있어 admin이 user에 의존:
```
admin → user (서비스 재사용)
```

**개선안**: 서비스를 별도 모듈로 분리
```
admin → service ← user
```

### 3. 이벤트 기반 아키텍처 강화
- [ ] 도메인 이벤트 활용 확대
- [ ] 비동기 이벤트 처리 (Spring Events → Kafka)

### 4. API 버전 관리
- [ ] API 버전 관리 전략 수립
- [ ] v1, v2 API 분리

---

## 부록: 파일 통계

| 모듈 | Java 파일 | 설정 파일 | 총 라인 수 (예상) |
|------|-----------|-----------|-------------------|
| common | 27 | 1 | ~2,000 |
| core | 130 | 1 | ~15,000 |
| integration | 15 | 1 | ~2,000 |
| auth | 11 | 1 | ~1,500 |
| user | 40 | 4 | ~8,000 |
| admin | 7 | 4 | ~1,500 |
| **합계** | **230** | **12** | **~30,000** |

---

**문서 작성일**: 2026-01-05  
**작성자**: 강슬기
**버전**: 1.0
