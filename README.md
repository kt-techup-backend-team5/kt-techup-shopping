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
- [팀원 소개](#-팀원-소개)

---

## 📘 프로젝트 소개
KT Cloud TECH-UP 백엔드 1기 **5조**의 전자상거래 백엔드 프로젝트입니다.
Spring Boot 기반으로 **전자상거래(쇼핑)** 의 기능을 제공하는 RESTful API를 설계하고 구현합니다.  
확장성과 유지보수성을 고려한 백엔드 아키텍처를 구축하여, 실제 배포 가능한 형태로 완성하는 것을 목표로 합니다.
- **확장 가능한 아키텍처**: 마이크로서비스 전환을 고려한 모듈식 설계
- **보안 강화**: JWT 기반 인증, BCrypt 암호화, Spring Security 적용
- **성능 최적화**: Redis 캐싱, 분산 락, QueryDSL을 통한 동적 쿼리 최적화
- **실시간 모니터링**: Slack 연동 알림 시스템

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
- 아이디/비밀번호 찾기
- 회원 정보 수정 및 탈퇴
- 관리자 권한 관리

### 🛍️ 상품 관리
- 상품 CRUD (등록, 조회, 수정, 삭제)
- 상품 검색 및 정렬 (최신순, 인기순)
- 상품 상태 관리 (활성화, 비활성화, 품절, 삭제)
- 실시간 조회수 추적 (Redis 기반)

### 📦 주문 관리
- 주문 생성 및 조회
- 주문 상태 관리 (결제대기, 결제완료, 배송중, 배송완료, 구매확정)
- 주문 취소 요청 및 관리자 승인/거절
- 주문 환불/반품 요청 및 관리자 승인/거절
- 수령인 정보 수정
- 분산 락을 통한 재고 관리

### 💳 결제 시스템
- 다중 결제 수단 지원 (현금, 카드, 간편결제)
- 결제 정보 관리
- 배송비 계산

### ⭐ 리뷰 시스템
- 구매 확정 후 리뷰 작성
- 리뷰 수정 및 삭제
- 상품별 리뷰 조회 (페이징, 정렬)
- 관리자 리뷰 관리 (검색, 삭제)

### 🔐 보안
- JWT Access/Refresh 토큰 인증
- BCrypt 비밀번호 암호화
- Spring Security 기반 접근 제어
- 비밀번호 정책 적용 (대소문자, 숫자, 특수문자 포함)

### 📊 통계 및 모니터링
- 방문자 통계 수집
- 상품 조회수 추적
- Slack 실시간 알림 시스템
- 관리자 대시보드용 통계 API

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

### Infrastructure
- **문서화**: Swagger (OpenAPI 3.0)
- **모니터링**: Slack API
- **형상 관리**: Git, GitHub

### 디자인 패턴 및 아키텍처
- Layered Architecture (Controller - Service - Repository)
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

#### 회원 (Users)
```
GET    /users/duplicate-login-id   # 아이디 중복 확인
POST   /users/find-login-id        # 아이디 찾기
GET    /users/my-info              # 내 정보 조회
PUT    /users/my-info              # 내 정보 수정
PUT    /users/{id}/change-password # 비밀번호 변경
DELETE /users/withdrawal           # 회원 탈퇴
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

#### 리뷰 (Reviews)
```
POST   /reviews                    # 리뷰 작성
GET    /reviews                    # 리뷰 목록 조회
PUT    /reviews/{id}               # 리뷰 수정
DELETE /reviews/{id}               # 리뷰 삭제
```

#### 관리자 - 리뷰 (Admin Reviews)
```
GET    /admin/reviews              # 리뷰 관리 목록
DELETE /admin/reviews/{id}         # 리뷰 삭제
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

### 3. 이벤트 기반 아키텍처
Spring Events를 활용한 느슨한 결합의 비즈니스 로직 구현:
- 상품 조회 이벤트 → 조회수 증가
- 방문 이벤트 → 통계 수집
- 시스템 이벤트 → Slack 알림

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

- **User**: 회원 정보 (일반/관리자)
- **Product**: 상품 정보
- **Order**: 주문 정보
- **OrderProduct**: 주문-상품 매핑 (다대다 해소)
- **Payment**: 결제 정보
- **Review**: 리뷰 정보

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

## 🙏 감사의 말

kt cloud TECH-UP 프로그램과 강사님들께 감사드립니다.
---


## 👥 팀원 소개
| 이름 | 역할 | GitHub |
|------|------|--------|
| **강슬기** | 팀장 / 백엔드 개발 | [SeulGi0117](https://github.com/SeulGi0117) |
| **이신영** | 백엔드 개발 | [youngyii](https://github.com/youngyii/kt-techup-shopping) |
| **김예은** | 백엔드 개발 | [YeKim1](https://github.com/YeKim1/kt-techup-shopping) |
| **양승희** | 백엔드 개발 | [seungh22](https://github.com/seungh22/kt_cloud_study.git) |
| **이동현** | 백엔드 개발 | [donghyeon95](https://github.com/donghyeon95/kt-techup-donghyoen) |

## 팀원 자기소개
**[강슬기]**
>안녕하세요, KT Cloud TECH-UP 백엔드 1기에서 학습 중인 강슬기입니다.
현재 5조 전자상거래 백엔드 프로젝트의 팀 리더로, 팀원들과 함께 Spring Boot 기반의 전자상거래 시스템을 설계·개발할 예정입니다.
백엔드 아키텍처와 클라우드 네이티브 개발에 집중하고 있으며, 특히 AI 모델 서빙과 시스템 아키텍처 설계에 관심을 가지고 있습니다.

**[김예은]**
> 안녕하세요. 앞으로 팀 프로젝트 서로 도와가며 즐겁게 완수했으면 좋겠습니다. 잘부탁드려요!

**[이동현]**
>안녕하세요~~

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

KT Cloud TECH-UP 프로그램과 멘토님들께 감사드립니다.
---
