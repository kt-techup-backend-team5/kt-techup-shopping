# kt-techup-shopping
KT Cloud TECH-UP Backend 1st Cohort Team 5’s e-commerce backend project. Implements RESTful APIs for shopping with Spring Boot.

# 🛒 KT Cloud TECH-UP 백엔드 1기 5조(JavaChip Frappuccino) - 쇼핑 프로젝트

## 📋 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [API 문서](#-api-문서)
- [시작하기](#-시작하기)
- [상세 문서](#-상세-문서)
- [팀원 소개](#-팀원-소개)

---

## 📘 프로젝트 소개
KT Cloud TECH-UP 백엔드 1기 **5조**의 전자상거래 백엔드 프로젝트입니다.
Spring Boot 기반으로 **전자상거래(쇼핑)** 의 기능을 제공하는 RESTful API를 설계하고 구현합니다.
확장성과 유지보수성을 고려한 백엔드 아키텍처를 구축하여, 실제 배포 가능한 형태로 완성하는 것을 목표로 합니다.

### 🎯 주요 특징
- **확장 가능한 아키텍처**: 마이크로서비스 전환을 고려한 모듈식 설계
- **AI 기능 통합**: OpenAI GPT-4o-mini, Qdrant Vector Store를 활용한 AI 추천/챗봇
- **보안 강화**: JWT 기반 인증, BCrypt 암호화, Spring Security 적용
- **성능 최적화**: Redis 캐싱, 분산 락, QueryDSL을 통한 동적 쿼리 최적화
- **실시간 모니터링**: ELK Stack, Slack 연동 알림 시스템
- **이벤트 기반 아키텍처**: Spring Events를 활용한 느슨한 결합 구조
- **멀티모듈 아키텍처(deploy/msa 브랜치)**: User/Admin 독립 배포를 위한 모듈 분리 (common, core, integration, auth, user, admin)

### 🪜 하위 목표
- **프로젝트 : 전자상거래 백엔드**
    - 상품 등록 및 조회 API
    - 장바구니 및 주문 처리 API
    - 결제 연동 및 보안 관리
    - 리뷰 및 평점 시스템 구현
    - 관리자용 상품 관리 기능
    - 재고 관리 시스템
---
## ✨ 주요 기능

### 👤 회원 관리
- 회원가입 및 로그인 (JWT 토큰 기반)
- 이메일 인증 (인증번호 발송 및 확인)
- 아이디/비밀번호 찾기
- 회원 정보 수정 및 탈퇴 (소프트 삭제)
- 관리자 권한 관리 (ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN)

### 🛍️ 상품 관리
- 상품 CRUD (등록, 조회, 수정, 삭제)
- 상품 검색 및 정렬 (최신순, 인기순, 가격순)
- 상품 상태 관리 (활성화, 비활성화, 품절, 삭제)
- 실시간 조회수 추적 (Redis 기반)
- AI 기반 상품 분석 (타겟 성별/연령대/추천 이유)

### 📦 주문 및 결제
- **주문 생성**: 장바구니 기반 주문 생성
- **주문 상태 관리**: ORDER_CREATED → ORDER_ACCEPTED → ORDER_PREPARING → ORDER_SHIPPING → ORDER_DELIVERED → ORDER_CONFIRMED
- **주문 취소**: 사용자 취소 요청 및 관리자 승인/거절
- **환불 처리**: 환불 요청 및 관리자 승인/거절
- **수령인 정보 관리**: 주문별 수령인 정보 수정 가능
- **재고 관리**: Redisson 분산 락을 통한 동시성 제어
- **결제 시스템**: 다중 결제 수단 지원 (현금, 카드, 간편결제)
- **배송비 계산**: 주문 금액 기반 자동 계산

### 💰 포인트 시스템
- **포인트 적립**: 구매 확정 시 자동 적립 (구매 금액의 1%)
- **포인트 사용**: 주문 시 포인트 차감 (분산 락 적용)
- **포인트 조회**: 잔액 조회 및 이력 조회 (최대 6개월)
- **포인트 이력**: 적립/사용/만료 내역 추적

### 🛒 장바구니 및 찜
- **장바구니**: 상품 담기/수량 변경/삭제/전체 비우기
- **비회원 장바구니 병합**: 로그인 시 LocalStorage 장바구니 자동 병합
- **찜(위시리스트)**: 상품 찜/해제/조회 (페이징)

### ⭐ 리뷰 시스템
- 구매 확정 후 리뷰 작성 (이미지 첨부 가능)
- 리뷰 수정 및 삭제 (소프트 삭제)
- 상품별 리뷰 조회 (페이징, 정렬: 최신순/평점순)
- 베스트 리뷰, 포토 리뷰 필터링
- 관리자 리뷰 관리 (검색, 블라인드 처리)
- **AI 리뷰 요약**: 최근 30개 리뷰를 AI가 3문장으로 요약 (Redis 캐싱 24시간)

### 💬 Q&A (상품 문의)
- **문의 작성**: 상품별 문의 작성 (공개/비공개 설정)
- **문의 조회**: 상품별 공개 문의 조회, 내 문의 조회
- **문의 수정/삭제**: 답변 전 수정/삭제 가능
- **답변 작성**: 관리자 답변 작성
- **공개 여부 변경**: 문의 공개/비공개 전환

### 📍 배송지 관리
- **배송지 등록**: 여러 배송지 등록 가능
- **기본 배송지**: 첫 배송지 자동 기본 설정, 기본 배송지 변경
- **배송지 수정/삭제**: 전체 교체 방식 수정, 소프트 삭제

### 🔐 보안
- JWT Access/Refresh 토큰 인증
- BCrypt 비밀번호 암호화
- Spring Security 기반 접근 제어
- 비밀번호 정책 적용 (대소문자, 숫자, 특수문자 포함)
- 이메일 인증 (Redis 기반, 5분 유효)

### 📊 통계 및 모니터링
- 방문자 통계 수집
- 상품 조회수 추적 (Redis 캐싱 → 매시간 DB 동기화)
- Slack 실시간 알림 시스템 (주문/결제/환불)
- 관리자 대시보드용 통계 API
- ELK Stack (Elasticsearch, Logstash, Kibana)을 통한 로그 수집 및 모니터링

### 🤖 AI 기능
- **AI 상품 추천**: 사용자 질문 기반 상품 추천 (Qdrant Vector Store 유사도 검색)
- **AI 챗봇**: OpenAI GPT-4o-mini를 활용한 고객 문의 자동 응답 (주문 내역 컨텍스트 자동 주입)
- **상품 분석**: AI 기반 타겟 고객 분석 (성별/연령대/추천 이유)
- **리뷰 요약**: 최근 30개 리뷰를 AI가 자동 요약 (Redis 캐싱)

---
## 🛠 기술 스택

### Backend
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.5.7
- **ORM**: Spring Data JPA, QueryDSL
- **보안**: Spring Security, JWT
- **데이터베이스**: MySQL (RDB)
- **캐싱**: Redis, Redisson (분산 락)
- **빌드 도구**: Gradle

### AI & ML
- **AI**: OpenAI GPT-4o-mini
- **Vector DB**: Qdrant Vector Store
- **AI Framework**: Spring AI

### Infrastructure & DevOps
- **문서화**: Swagger (OpenAPI 3.0)
- **모니터링**: ELK Stack (Elasticsearch, Logstash, Kibana), Slack API
- **파일 저장**: AWS S3 / LocalStack (개발 환경)
- **형상 관리**: Git, GitHub
- **CI/CD**: GitHub Actions
- **배포**: AWS Elastic Beanstalk

### 디자인 패턴 및 아키텍처
- Layered Architecture (Controller - Service - Repository)
- Multi-Module Architecture (common, core, integration, auth, user, admin)
- AOP (Aspect-Oriented Programming)
- 이벤트 기반 아키텍처 (Spring Events)
- DTO 패턴
- Repository 패턴

---
## 🏗 시스템 아키텍처
```
┌─────────────────┐
│   API Gateway   │
│   (Future)      │
└────────┬────────┘
         │
┌────────▼────────────────────────────────────────┐
│           Spring Boot Application               │
│  ┌──────────────────────────────────────────┐   │
│  │         Controller Layer                 │   │
│  │  (REST API Endpoints)                    │   │
│  └──────────────┬───────────────────────────┘   │
│                 │                               │
│  ┌──────────────▼───────────────────────────┐   │
│  │         Service Layer                    │   │
│  │  (Business Logic)                        │   │
│  └──────────────┬───────────────────────────┘   │
│                 │                               │
│  ┌──────────────▼───────────────────────────┐   │
│  │         Repository Layer                 │   │
│  │  (Data Access)                           │   │
│  └──────────────┬───────────────────────────┘   │
└─────────────────┼───────────────────────────────┘
                  │
     ┌────────────┼────────────┐
     │            │            │
┌────▼────┐  ┌───▼────┐  ┌───▼────┐
│  MySQL  │  │ Redis  │  │ Slack  │
│   DB    │  │ Cache  │  │  API   │
└─────────┘  └────────┘  └────────┘
```
---

## 📚 API 문서

### Swagger UI
프로젝트 실행 후 아래 URL로 접속하여 전체 API 문서를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui/index.html
```

### 주요 API 엔드포인트

#### 인증 (Authentication)
```
POST   /auth/signup          # 회원가입
POST   /auth/login           # 로그인
POST   /auth/logout          # 로그아웃
POST   /auth/reissue         # 토큰 재발급
```

#### 이메일 인증 (Mail)
```
POST   /mail/send            # 이메일 인증번호 발송
POST   /mail/check           # 이메일 인증번호 확인
```

#### 회원 (Users)
```
GET    /users/duplicate-login-id   # 아이디 중복 확인
POST   /users/find-login-id        # 아이디 찾기
GET    /users/my-info              # 내 정보 조회
PUT    /users/my-info              # 내 정보 수정
PUT    /users/{id}/change-password # 비밀번호 변경
DELETE /users/withdrawal           # 회원 탈퇴
```

#### 포인트 (Points)
```
GET    /users/me/points            # 포인트 잔액 조회
GET    /users/me/points/history    # 포인트 이력 조회
```

#### 상품 (Products)
```
GET    /products                   # 상품 목록 조회
GET    /products/{id}              # 상품 상세 조회
GET    /products/{id}/reviews      # 상품 리뷰 목록
```

#### 관리자 - 상품 (Admin Products)
```
GET    /admin/products             # 상품 관리 목록
POST   /admin/products             # 상품 등록
PUT    /admin/products/{id}        # 상품 수정
DELETE /admin/products/{id}        # 상품 삭제
POST   /admin/products/{id}/activate        # 상품 활성화
POST   /admin/products/{id}/in-activate     # 상품 비활성화
POST   /admin/products/{id}/toggle-sold-out # 상품 품절 처리
```

#### 장바구니 (Cart)
```
GET    /cart                       # 장바구니 조회
POST   /cart                       # 장바구니 담기
PUT    /cart/{productId}           # 수량 변경
DELETE /cart/{productId}           # 장바구니 항목 삭제
DELETE /cart                       # 전체 비우기
POST   /cart/merge                 # 비회원 장바구니 병합
```

#### 찜 (Wishlist)
```
GET    /wishlist                   # 찜 목록 조회
POST   /wishlist/{productId}       # 찜 등록
DELETE /wishlist/{productId}       # 찜 해제
```

#### 주문 (Orders)
```
POST   /orders                     # 주문 생성
GET    /orders                     # 내 주문 목록
GET    /orders/{id}                # 주문 상세 조회
PUT    /orders/{id}                # 주문 수정
POST   /orders/{id}/cancel         # 주문 취소 요청
POST   /orders/{id}/pay            # 주문 결제
```

#### 관리자 - 주문 (Admin Orders)
```
GET    /admin/orders               # 주문 목록 조회
GET    /admin/orders/{id}          # 주문 상세 조회
GET    /admin/orders/cancel        # 취소 요청 목록
POST   /admin/orders/{id}/cancel   # 취소 승인/거절
POST   /admin/orders/{id}/change-status  # 주문 상태 변경
```

#### 결제 (Payment)
```
POST   /payments                   # 결제 처리
GET    /payments/{id}              # 결제 정보 조회
```

#### 배송지 (Addresses)
```
GET    /addresses                  # 배송지 목록 조회
POST   /addresses                  # 배송지 등록
PUT    /addresses/{id}             # 배송지 수정
DELETE /addresses/{id}             # 배송지 삭제
```

#### 리뷰 (Reviews)
```
POST   /reviews                    # 리뷰 작성
GET    /reviews                    # 리뷰 목록 조회
PUT    /reviews/{id}               # 리뷰 수정
DELETE /reviews/{id}               # 리뷰 삭제
GET    /reviews/summary/{productId} # AI 리뷰 요약
```

#### 관리자 - 리뷰 (Admin Reviews)
```
GET    /admin/reviews              # 리뷰 관리 목록
DELETE /admin/reviews/{id}         # 리뷰 삭제
```

#### 상품 문의 (Questions & Answers)
```
POST   /questions                  # 문의 작성
GET    /questions?productId={id}   # 상품별 문의 조회
GET    /questions/my               # 내 문의 목록
GET    /questions/{id}             # 문의 상세 조회
PUT    /questions/{id}             # 문의 수정
PATCH  /questions/{id}/public      # 공개 여부 변경
DELETE /questions/{id}             # 문의 삭제
```

#### 관리자 - 문의/답변 (Admin Q&A)
```
GET    /admin/questions            # 문의 목록 조회
POST   /admin/questions/{id}/answers # 답변 작성
PUT    /admin/questions/{id}/answers/{answerId} # 답변 수정
DELETE /admin/questions/{id}/answers/{answerId} # 답변 삭제
```

#### AI 챗봇 (Chatbot)
```
POST   /api/chatbot/chat           # AI 챗봇 대화
```
---
## 🚀 시작하기

### 🗒️ 사전 요구사항
프로젝트를 실행하기 전에 다음 소프트웨어가 설치되어 있어야 합니다.
| 소프트웨어 | 버전 | 다운로드 링크 | 비고 |
|----------|------|------------|------|
| **Java** | 21 이상 | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 또는 [OpenJDK](https://adoptium.net/) | JDK 21 필수 |
| **MySQL** | 8.0 이상 | [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) | 데이터베이스 서버 |
| **Redis** | 6.0 이상 | [Redis](https://redis.io/download) | 캐싱 및 분산 락 |
| **Git** | 최신 버전 | [Git](https://git-scm.com/downloads) | 소스 코드 관리 |

#### 선택 사항
- **Docker Desktop**: MySQL과 Redis를 Docker로 실행할 경우
- **IntelliJ IDEA** : IDE
- **Postman**: API 테스트용

### 설치 및 실행

1. **레포지토리 클론**
```bash
git clone https://github.com/kt-techup-backend-team5/kt-techup-shopping.git
cd kt-techup-shopping
```

2. **데이터베이스 설정**
```sql
CREATE DATABASE shopping;
```

3. **application.yml 설정**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shopping
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
        jdbc:
          time_zone: Asia/Seoul
        show_sql: true
  
  data:
    redis:
      host: ${redis.host:localhost}
      port: ${redis.port:6379}

jwt:
  secret: your-secret-key-here
  access-token-expiration: 3600000    # 1시간
  refresh-token-expiration: 43200000  # 12시간

slack:
  bot-token: your-slack-bot-token
  log-channel: your-channel-id
```

4. **프로젝트 빌드 및 실행**
```bash
./gradlew clean build
./gradlew bootRun
```

5. **API 테스트**
```
브라우저에서 http://localhost:8080/swagger-ui/index.html 접속
```

### 환경별 프로파일

- **local**: 로컬 개발 환경
- **dev**: 개발 서버 환경
- **prod**: 운영 서버 환경
```bash
# 프로파일 지정 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```
---

## 🔑 주요 구현 기능

### 1. 분산 락을 통한 동시성 제어
Redisson을 활용한 분산 락으로 재고 관리의 동시성 문제를 해결했습니다.
```java
@Lock(key = Lock.Key.STOCK, index = 1)
public void create(Long userId, Long productId, ...) {
    // 재고 확인 및 차감 로직
}
```

### 2. QueryDSL 동적 쿼리
복잡한 검색 조건을 QueryDSL로 구현하여 유연한 조회 기능을 제공합니다.

### 3. 이벤트 기반 아키텍처 (Event-Driven Design)
Spring Events를 활용한 느슨한 결합의 비즈니스 로직 구현:

**주문 관련 이벤트**:
- `OrderCreatedEvent`: 주문 생성 → 재고 차감, Slack 알림
- `OrderPaidEvent`: 결제 완료 → 주문 상태 변경
- `OrderCancelledEvent`: 주문 취소 → 재고 복구, 포인트 환불
- `OrderConfirmedEvent`: 구매 확정 → 포인트 적립

**결제 관련 이벤트**:
- `PaymentCompletedEvent`: 결제 완료 → 주문 상태 업데이트, Slack 알림

**환불 관련 이벤트**:
- `RefundApprovedEvent`: 환불 승인 → 재고 복구, 포인트 환불
- `RefundRejectedEvent`: 환불 거절 → 주문 상태 복원

**리뷰 관련 이벤트**:
- `ReviewCreatedEvent`: 리뷰 작성 → 상품 평점 업데이트
- `ReviewDeletedEvent`: 리뷰 삭제 → 상품 평점 재계산

**기타 이벤트**:
- 상품 조회 이벤트 → 조회수 증가 (Redis)
- 방문 이벤트 → 통계 수집
- 시스템 이벤트 → Slack 알림

자세한 내용은 [이벤트 드리븐 디자인 문서](EVENT_DRIVEN_DESIGN.md)를 참조하세요.

### 4. 소프트 삭제 (Soft Delete)
사용자와 리뷰 데이터는 물리적 삭제 대신 논리적 삭제를 적용했습니다.
```java
@SQLDelete(sql = "UPDATE user SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
public class User extends BaseEntity { ... }
```

---

## 📊 ERD
<img width="3752" height="1986" alt="Image" src="https://github.com/user-attachments/assets/45725255-9eb4-4bda-873e-cb6c62a3927e" />

### 주요 엔티티

- **User**: 회원 정보 (일반/관리자, 소프트 삭제)
- **Product**: 상품 정보 (상태 관리, AI 분석 정보 포함)
- **Order**: 주문 정보 (주문 상태 관리)
- **OrderProduct**: 주문-상품 매핑 (다대다 해소)
- **Payment**: 결제 정보 (결제 수단, 결제 상태)
- **Point**: 포인트 잔액
- **PointHistory**: 포인트 이력 (적립/사용/만료)
- **Review**: 리뷰 정보 (별점, 이미지, 소프트 삭제)
- **CartItem**: 장바구니 항목
- **Wishlist**: 찜 목록
- **Question**: 상품 문의 (공개/비공개)
- **Answer**: 문의 답변
- **Address**: 배송지 정보 (기본 배송지 관리)
- **Refund**: 환불 정보
- **VisitStat**: 방문자 통계

---
## 🌿 브랜치 전략

- **기본 브랜치**
  - **main**: 항상 배포 가능한 상태를 유지하는 브랜치

- **Jira 기반 기능 브랜치 (Jira Branch)**
  - **네이밍 규칙**: `KAN-이슈번호`
  - 예시: `KAN-12`, `KAN-21`
  - Jira 이슈와 1:1 매핑을 권장

- **버그/핫픽스 브랜치 (Hotfix Branch)**
  - **네이밍 규칙**: `hotfix/짧은-설명`
  - 예시: `hotfix/login-error`

- **브랜치 운영 규칙**
  - `main` 에 직접 커밋 **금지**, 반드시 PR 통해 머지
  - 브랜치 생성 시 항상 최신 `main`기준으로 분기
  - 작업 완료 후 **PR + 코드리뷰 + 승인** 절차를 거친 후 머지

---

## 📝 커밋 메세지 규칙 (Commit Message Convention)


### ✅ 7가지 기본 규칙

1. **타입(type)은 소문자로 작성**
2. 제목과 본문은 **빈 줄(엔터 1줄)** 로 구분
3. 제목은 **50자 이내 한글로 작성**
4. 제목 끝에는 **마침표 금지**
5. 제목은 **명령문 형태**, **과거형 금지**
6. 본문 각 행은 **72자 이내**로 작성
7. **무엇과 왜**를 설명 (어떻게는 PR에 기술)

### 🔤 타입 분류

| 타입 | 설명 |
|------|------|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| build | 빌드 관련 변경 (모듈 설치/삭제 등) |
| chore | 자잘한 변경 (코드 영향 없음) |
| ci | CI/CD 관련 설정 변경 |
| docs | 문서 수정 |
| style | 포맷팅, 세미콜론 등 비기능적 수정 |
| refactor | 코드 리팩터링 |
| test | 테스트 코드 추가/수정 |
| perf | 성능 개선 |

### 🧱 구조

- **Header (필수)**  
  - `type(scope): subject`
- **Body (선택)**  
  - 변경 이유, 상세 내용
- **Footer (선택)**  
  - 이슈 번호, 연관 작업 등

- **스코프(scope) 예시**
  - `auth`, `order`, `product`, `user`, `build`, `deps` 등 (선택)
- **Footer 예시**
  - `fixes: #42`, `resolves: #1137` 등

```bash
git commit -m "feat(order): 주문 생성 API 구현

- 주문 요청 DTO 생성
- 주문 생성 시 재고 차감 로직 추가

fixes: #42"
```

---

## 🔄 PR 작성 규칙 (Pull Request Rules)

### 💬 PR 생성 규칙

- **대상 브랜치**
  - 기능 브랜치: `KAN-XX` → `main`
- **리뷰**
  - 최소 **1인 이상 리뷰 승인 필수**
  - 본인 PR은 **셀프 머지 금지** (브랜치 보호 규칙으로 막혀 있음)

### 🧩 PR 제목 포맷

- 형식: **`[type](scope): subject`**
- 예시: `feat(product): 상품 등록 API 구현`

### 📄 PR 본문 템플릿

```markdown
[type](scope): subject

### 🔧 구현 내용
- 무엇을 어떻게 왜 개발했는지
- 주요 변경 사항 요약

### 📌 관련 Jira Issue
- KAN-XX

### 🧪 테스트 방법
- [엔드포인트] /api/v1/...
- [파라미터] 예: ...
- [체크 포인트] 응답 코드/바디, DB 변경사항 등

### ❗ 기타 참고 사항
- 추가로 리뷰가 필요한 부분
- 브레이킹 체인지 여부 등
```

팀 합의에 따라 PR 템플릿은 GitHub 리포지토리 `.github/pull_request_template.md`로도 관리할 수 있습니다.

## 📐 코딩 컨벤션

본 프로젝트는 **Naver 코딩 컨벤션(Naver Hackday Java Convention)**을 따릅니다.

### Checkstyle 설정

프로젝트에는 코드 품질 관리를 위한 Checkstyle이 적용되어 있습니다:

- **컨벤션**: Naver Coding Convention
- **설정 파일**: `naver-checkstyle-suppressions.xml`
- **적용 범위**: Java 소스 코드 전체

---

## 📚 상세 문서

프로젝트의 상세한 아키텍처 및 배포 가이드는 다음 문서들을 참고하세요.

### 모놀로식 아키텍처 (deploy/monolith 브랜치)
- [모놀로식 아키텍처 설계 문서](docs/monolith/ARCHITECTURE.md)
  - 시스템 개요 및 전체 아키텍처
  - AI 기능 아키텍처 (OpenAI, Qdrant, 챗봇, 상품 추천)
  - 외부 시스템 통합 (AWS S3, Redis, Slack, ELK Stack)
  - 주요 기능별 상세 설명

- [CI/CD 자동화 배포 가이드](docs/monolith/CICD.md)
  - GitHub Actions 워크플로우 구조
  - 자동 테스트 및 배포 프로세스
  - AWS Elastic Beanstalk 배포 전략
  - 트러블슈팅 가이드

### 멀티모듈 아키텍처 (deploy/msa 브랜치)
- [멀티모듈 아키텍처 설계 문서](docs/msa/ARCHITECTURE.md)
  - 멀티모듈 전환 배경 및 목표
  - 모듈 분리 기준 및 구조 (common, core, integration, auth, user, admin)
  - 의존성 그래프 및 빌드 설정
  - 기능별 모듈 매핑

- [AWS 멀티모듈 배포 가이드](docs/msa/AWS-DEPLOYMENT.md)
  - User/Admin 독립 배포 아키텍처
  - 공유 리소스 전략 (RDS, Redis, S3)
  - Elastic Beanstalk 환경 설정
  - 비용 산정 및 최적화

- [멀티모듈 실행 가이드](docs/msa/RUN.md)
  - 로컬 개발 환경 설정
  - User/Admin 애플리케이션 실행 방법
  - ELK Stack 설정 및 로그 확인

- [배포 핵심 요약](docs/msa/DEPLOYMENT-SUMMARY.md)
  - 30초 만에 이해하는 배포 구조
  - 자주 묻는 질문 (FAQ)

---

## 👥 팀원 소개
| 이름 | 역할 | GitHub |
|------|------|--------|
| **강슬기** | 팀장 / 백엔드 개발 | [SeulGi0117](https://github.com/SeulGi0117) |
| **이신영** | 백엔드 개발 | [youngyii](https://github.com/youngyii/kt-techup-shopping) |
| **김예은** | 백엔드 개발 | [YeKim1](https://github.com/YeKim1/kt-techup-shopping) |
| **양승희** | 백엔드 개발 | [seungh22](https://github.com/seungh22/kt_cloud_study.git) |

## 팀원 자기소개
**[강슬기]**
>안녕하세요, KT Cloud TECH-UP 백엔드 1기에서 학습 중인 강슬기입니다.
현재 5조 전자상거래 백엔드 프로젝트의 팀 리더로, 팀원들과 함께 Spring Boot 기반의 전자상거래 시스템을 설계·개발할 예정입니다.
백엔드 아키텍처와 클라우드 네이티브 개발에 집중하고 있으며, 특히 AI 모델 서빙과 시스템 아키텍처 설계에 관심을 가지고 있습니다.

**[김예은]**
> 안녕하세요. 앞으로 팀 프로젝트 서로 도와가며 즐겁게 완수했으면 좋겠습니다. 잘부탁드려요!

**[이신영]**
>안녕하세요!  
TECH UP에서 백엔드 개발을 공부 중인 이신영입니다.  
이번 프로젝트가 기대되네요. 최선을 다하겠습니다!

**[양승희]**
>안녕하세요! 양승희입니다!!! 반갑습니다

---
## 📜 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조.

## 🙏 감사의 말

KT Cloud TECH-UP 프로그램과 강사님들께 감사드립니다.
---
