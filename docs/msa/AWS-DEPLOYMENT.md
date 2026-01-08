# AWS 멀티모듈 배포 가이드

## 📋 목차
1. [배포 아키텍처 개요](#배포-아키텍처-개요)
2. [Elastic Beanstalk 구조](#elastic-beanstalk-구조)
3. [리소스 공유 전략](#리소스-공유-전략)
4. [배포 단계별 가이드](#배포-단계별-가이드)
5. [환경 변수 설정](#환경-변수-설정)
6. [비용 산정](#비용-산정)
7. [통신 구조](#통신-구조)

---

## 배포 아키텍처 개요

### 전체 구조도

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Cloud (ap-northeast-2)               │
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Elastic Beanstalk Application                  │ │
│  │         "kt-techup-shopping"                           │ │
│  │                                                        │ │
│  │  ┌─────────────────────┐    ┌─────────────────────┐    │ │
│  │  │ Environment: USER   │    │ Environment: ADMIN  │    │ │
│  │  │ (shopping-user)     │    │ (shopping-admin)    │    │ │
│  │  │                     │    │                     │    │ │
│  │  │  ┌──────────────┐   │    │  ┌──────────────┐   │    │ │
│  │  │  │   EC2 #1     │   │    │  │   EC2 #2     │   │    │ │
│  │  │  │  :8080       │   │    │  │  :8080       │   │    │ │
│  │  │  │ user.jar     │   │    │  │ admin.jar    │   │    │ │
│  │  │  └──────────────┘   │    │  └──────────────┘   │    │ │
│  │  │                     │    │                     │    │ │
│  │  │  Auto Scaling:      │    │  Auto Scaling:      │    │ │
│  │  │  Min: 1, Max: 3     │    │  Min: 1, Max: 1     │    │ │
│  │  └─────────────────────┘    └─────────────────────┘    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              공유 리소스 (Shared Resources)            │    │
│  │                                                     │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐   │    │
│  │  │     RDS      │  │ ElastiCache  │  │   S3     │   │    │
│  │  │   (MySQL)    │  │   (Redis)    │  │ Bucket   │   │    │
│  │  │              │  │              │  │          │   │    │
│  │  │  :3306       │  │  :6379       │  │          │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────┘   │    │
│  │                                                     │    │
│  │  user와 admin이 모두 접근                              │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              보안 그룹 (Security Groups)              │    │
│  │                                                     │    │
│  │  SG-User:   0.0.0.0/0 → :80 (ALB)                   │    │
│  │  SG-Admin:  0.0.0.0/0 → :80 (ALB)                   │    │
│  │  SG-RDS:    SG-User, SG-Admin → :3306               │    │
│  │  SG-Redis:  SG-User, SG-Admin → :6379               │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘

외부 사용자
    │
    ├─→ https://user.kt-techup.com    → USER Environment  (EC2 #1)
    │                                      ↓
    │                                   user.jar (8080)
    │
    └─→ https://admin.kt-techup.com   → ADMIN Environment (EC2 #2)
                                          ↓
                                       admin.jar (8080)

둘 다 같은 RDS, Redis, S3 사용
```

---

## Elastic Beanstalk 구조

### Application vs Environment 구분

**중요**: Elastic Beanstalk에는 **Application**과 **Environment**라는 개념이 있습니다.

#### Application (애플리케이션)
- **논리적 컨테이너** 역할
- 여러 Environment를 그룹화
- **1개만 만들면 됩니다**: `kt-techup-shopping`

#### Environment (환경)
- **실제 배포 단위**
- 각 Environment마다 독립적인 EC2, Load Balancer 생성
- **2개 만들어야 합니다**:
  - `kt-techup-shopping-user` (shopping-user.zip 배포)
  - `kt-techup-shopping-admin` (shopping-admin.zip 배포)

### 구조 예시

```
AWS Console
└── Elastic Beanstalk
    └── Application: kt-techup-shopping  ← 1개 (논리적 그룹)
        ├── Environment: kt-techup-shopping-user   ← 실제 배포 #1
        │   ├── EC2 Instance(s)
        │   ├── Load Balancer
        │   ├── Auto Scaling Group
        │   └── shopping-user.zip 실행
        │
        └── Environment: kt-techup-shopping-admin  ← 실제 배포 #2
            ├── EC2 Instance(s)
            ├── Load Balancer
            ├── Auto Scaling Group
            └── shopping-admin.zip 실행
```

### EC2 인스턴스 개수

**각 Environment마다 최소 1대의 EC2 인스턴스가 생성됩니다.**

| Environment | EC2 인스턴스 | Auto Scaling | 포트 |
|-------------|-------------|--------------|------|
| USER | 최소 1대 (권장: 1~3대) | 활성화 | 8080 |
| ADMIN | 1대 고정 | 비활성화 | 8080 |

**총 EC2 인스턴스**: 최소 2대 (user 1대 + admin 1대)

---

## 리소스 공유 전략

### 공유 리소스 (Shared Resources)

user와 admin은 **완전히 독립적인 애플리케이션**이지만, **인프라는 공유**합니다.

```
┌──────────┐       ┌──────────┐
│   USER   │       │  ADMIN   │
│ (EC2 #1) │       │ (EC2 #2) │
└────┬─────┘       └────┬─────┘
     │                  │
     └──────┬───────────┘
            │
    ┌───────▼────────┐
    │  공유 리소스      │
    │                │
    │  • RDS (1개)   │  ← 같은 데이터베이스
    │  • Redis (1개) │  ← 같은 캐시
    │  • S3 (1개)    │  ← 같은 파일 스토리지
    └────────────────┘
```

#### 1. RDS (MySQL) - 1개 인스턴스
- **엔드포인트**: `kt-techup-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306`
- **user와 admin 둘 다 같은 RDS 접속**
- 비용: 1대 요금만 발생

**환경 변수 (user, admin 동일)**:
```properties
spring.datasource.url=jdbc:mysql://kt-techup-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/shopping
spring.datasource.username=admin
spring.datasource.password=your-password
```

#### 2. ElastiCache (Redis) - 1개 클러스터
- **엔드포인트**: `kt-techup-redis.xxxxx.cache.amazonaws.com:6379`
- **user와 admin 둘 다 같은 Redis 접속**
- 캐시, 분산 락, 세션 공유
- 비용: 1대 요금만 발생

**환경 변수 (user, admin 동일)**:
```properties
spring.data.redis.host=kt-techup-redis.xxxxx.cache.amazonaws.com
spring.data.redis.port=6379
```

#### 3. S3 - 1개 버킷
- **버킷 이름**: `kt-techup-shopping-media`
- **user와 admin 둘 다 같은 S3 사용**
- 상품 이미지, 리뷰 이미지 저장
- 비용: 저장 용량 + 요청 횟수

**환경 변수 (user, admin 동일)**:
```properties
cloud.aws.s3.bucket=kt-techup-shopping-media
cloud.aws.region.static=ap-northeast-2
```

### 왜 리소스를 공유하나?

**비용 절감**:
- RDS 2개 → 1개: **약 50% 절감**
- Redis 2개 → 1개: **약 50% 절감**
- 데이터 일관성 보장 (같은 DB 사용)

**데이터 공유 필요**:
- user가 작성한 주문을 admin이 조회해야 함
- user가 올린 상품을 admin이 관리해야 함
- 같은 데이터베이스를 써야 합니다!

---

## 배포 단계별 가이드

### Phase 1: 공유 리소스 생성 (1회만 실행)

#### 1-1. RDS (MySQL) 생성

```bash
# AWS Console → RDS → Create database

Engine: MySQL 8.0
Template: Free tier (또는 Production)
DB instance identifier: kt-techup-db
Master username: admin
Master password: [설정]

DB instance class: db.t3.micro (Free tier) 또는 db.t3.small
Storage: 20GB (General Purpose SSD)
VPC: Default VPC
Public access: No
VPC security group: Create new (kt-techup-db-sg)
  - Inbound: MySQL/Aurora (3306) from Beanstalk Security Groups
```

**생성 후 엔드포인트 복사**:
```
kt-techup-db.c9a8x7y6z5w4.ap-northeast-2.rds.amazonaws.com
```

#### 1-2. ElastiCache (Redis) 생성

```bash
# AWS Console → ElastiCache → Create

Cluster engine: Redis
Name: kt-techup-redis
Node type: cache.t3.micro (또는 cache.t3.small)
Number of replicas: 0 (개발용) 또는 1 (프로덕션)

Subnet group: Create new
VPC: Default VPC
Security group: Create new (kt-techup-redis-sg)
  - Inbound: Custom TCP (6379) from Beanstalk Security Groups
```

**생성 후 엔드포인트 복사**:
```
kt-techup-redis.xxxxx.cache.amazonaws.com
```

#### 1-3. S3 버킷 생성

```bash
# AWS Console → S3 → Create bucket

Bucket name: kt-techup-shopping-media
Region: ap-northeast-2 (Seoul)
Block all public access: OFF (이미지는 public)

# CORS 설정
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
    "AllowedOrigins": ["*"],
    "ExposeHeaders": []
  }
]
```

---

### Phase 2: Elastic Beanstalk 애플리케이션 생성

#### 2-1. Application 생성 (1회만)

```bash
# AWS Console → Elastic Beanstalk → Create application

Application name: kt-techup-shopping
Platform: Java
Platform branch: Corretto 21
Platform version: (latest)

# 아직 Environment는 만들지 않음!
```

---

### Phase 3: User Environment 배포

#### 3-1. User JAR 빌드

```bash
# 로컬에서 실행
cd /Users/goorm/Desktop/Project/kt-techup-javachip-team5

# Java 21 설정
export JAVA_HOME=/Users/goorm/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home

# User 빌드
./gradlew :user:clean :user:zip

# 생성된 파일 확인
ls -lh user/build/distributions/shopping-user.zip
```

**shopping-user.zip 구조**:
```
shopping-user.zip
├── shopping-user.jar  (102MB)
└── Procfile           (web: java -jar shopping-user.jar)
```

#### 3-2. User Environment 생성

```bash
# AWS Console → Elastic Beanstalk → kt-techup-shopping → Create environment

Environment name: kt-techup-shopping-user
Domain: kt-techup-shopping-user (또는 원하는 이름)

Platform: Java
Platform branch: Corretto 21

Application code: Upload your code
  - 파일 선택: user/build/distributions/shopping-user.zip

# Presets: Single instance (Free tier) 또는 High availability

Configure more options 클릭
```

#### 3-3. User Environment 설정

**Capacity (용량)**:
```
Environment type: Load balanced
Instance type: t3.small (또는 t3.medium)

Auto Scaling group:
  Min instances: 1
  Max instances: 3
```

**Software (환경 변수)**:
```properties
# 서버 설정
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# 데이터베이스 (RDS 엔드포인트)
SPRING_DATASOURCE_URL=jdbc:mysql://kt-techup-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/shopping
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=your-password

# Redis (ElastiCache 엔드포인트)
SPRING_DATA_REDIS_HOST=kt-techup-redis.xxxxx.cache.amazonaws.com
SPRING_DATA_REDIS_PORT=6379

# S3
CLOUD_AWS_S3_BUCKET=kt-techup-shopping-media
CLOUD_AWS_REGION_STATIC=ap-northeast-2

# JWT
JWT_SECRET_KEY=your-secret-key-min-32-characters-long

# Slack (선택사항)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

**Network (보안 그룹)**:
```
VPC: Default VPC
Load balancer visibility: Public
Instance subnets: 모든 subnet 선택

EC2 security groups:
  - kt-techup-user-sg (생성 필요)
    Inbound:
      - HTTP (80) from 0.0.0.0/0 (Load Balancer에서)
      - HTTPS (443) from 0.0.0.0/0 (선택사항)

# RDS Security Group에 kt-techup-user-sg 추가 필요
# Redis Security Group에 kt-techup-user-sg 추가 필요
```

#### 3-4. User 배포 확인

```bash
# 배포 완료까지 5~10분 소요

# Health 확인
curl http://kt-techup-shopping-user.ap-northeast-2.elasticbeanstalk.com/actuator/health

# Response:
{"status":"UP"}

# Swagger UI 확인
http://kt-techup-shopping-user.ap-northeast-2.elasticbeanstalk.com/swagger-ui.html
```

---

### Phase 4: Admin Environment 배포

#### 4-1. Admin JAR 빌드

```bash
# Admin 빌드
./gradlew :admin:clean :admin:zip

# 생성된 파일 확인
ls -lh admin/build/distributions/shopping-admin.zip
```

#### 4-2. Admin Environment 생성

```bash
# AWS Console → Elastic Beanstalk → kt-techup-shopping → Create environment

Environment name: kt-techup-shopping-admin
Domain: kt-techup-shopping-admin

Platform: Java (Corretto 21)

Application code: Upload your code
  - 파일 선택: admin/build/distributions/shopping-admin.zip
```

#### 4-3. Admin Environment 설정

**Capacity**:
```
Environment type: Single instance (또는 Load balanced)
Instance type: t3.micro (또는 t3.small)

Auto Scaling group:
  Min instances: 1
  Max instances: 1  ← Admin은 1대만!
```

**Software (환경 변수)**:
```properties
# User와 동일한 환경 변수 사용!
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# RDS (User와 동일)
SPRING_DATASOURCE_URL=jdbc:mysql://kt-techup-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/shopping
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=your-password

# Redis (User와 동일)
SPRING_DATA_REDIS_HOST=kt-techup-redis.xxxxx.cache.amazonaws.com
SPRING_DATA_REDIS_PORT=6379

# S3 (User와 동일)
CLOUD_AWS_S3_BUCKET=kt-techup-shopping-media
CLOUD_AWS_REGION_STATIC=ap-northeast-2

# JWT (User와 동일 - 중요!)
JWT_SECRET_KEY=your-secret-key-min-32-characters-long

# Slack (User와 동일)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

**Network**:
```
EC2 security groups:
  - kt-techup-admin-sg (생성 필요)
    Inbound:
      - HTTP (80) from 0.0.0.0/0
      - HTTPS (443) from 0.0.0.0/0 (선택사항)

# RDS Security Group에 kt-techup-admin-sg 추가
# Redis Security Group에 kt-techup-admin-sg 추가
```

#### 4-4. Admin 배포 확인

```bash
curl http://kt-techup-shopping-admin.ap-northeast-2.elasticbeanstalk.com/actuator/health

# Swagger UI
http://kt-techup-shopping-admin.ap-northeast-2.elasticbeanstalk.com/swagger-ui.html
```

---

## 환경 변수 설정

### 중요: JWT Secret Key는 반드시 동일해야 함!

**user와 admin의 `JWT_SECRET_KEY`는 반드시 같은 값을 사용해야 합니다.**

이유:
- user에서 발급한 JWT 토큰을 admin에서도 검증해야 할 수 있음
- 관리자가 user API를 호출할 수도 있음

### 환경별 설정

**개발(로컬)**:
```yaml
# user/src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  data:
    redis:
      host: localhost
      port: 6379
```

**프로덕션(AWS)**:
```properties
# Elastic Beanstalk 환경 변수로 설정
SPRING_DATASOURCE_URL=jdbc:mysql://RDS-ENDPOINT:3306/shopping
SPRING_DATA_REDIS_HOST=REDIS-ENDPOINT
```

---

## 비용 산정

### 월별 예상 비용 (Seoul Region)

| 리소스 | 사양 | 수량 | 월 비용 (USD) | 비고 |
|--------|------|------|---------------|------|
| **EC2 (User)** | t3.small | 1~3대 | $15~45 | Auto Scaling |
| **EC2 (Admin)** | t3.micro | 1대 | $7 | 고정 |
| **RDS** | db.t3.micro | 1대 | $15 | 공유 |
| **ElastiCache** | cache.t3.micro | 1대 | $12 | 공유 |
| **Load Balancer** | ALB | 2개 | $32 | 각 Environment |
| **S3** | - | 1개 | $1~5 | 용량에 따라 |
| **데이터 전송** | - | - | $5~10 | 트래픽에 따라 |
| **총 비용** | - | - | **$87~126** | 월 예상 |

### 비용 절감 팁

1. **Free Tier 활용** (12개월):
   - EC2 t2.micro 750시간/월 무료
   - RDS db.t2.micro 750시간/월 무료
   - S3 5GB 무료

2. **개발 환경 최소화**:
   - User: t3.small 1대 고정
   - Admin: t3.micro 1대 고정
   - Auto Scaling 비활성화

3. **야간 자동 종료**:
   - CloudWatch Events로 22:00~09:00 자동 종료
   - **약 50% 비용 절감**

---

## 통신 구조

### User와 Admin은 서로 통신하지 않습니다!

**중요**: user와 admin은 **완전히 독립적인 API 서버**입니다.

```
┌─────────────────────────────────────────────┐
│             클라이언트 (브라우저/앱)              │
└─────────────────────────────────────────────┘
         │                        │
         │                        │
    ┌────▼─────┐           ┌──────▼────┐
    │   USER   │           │   ADMIN   │
    │  API     │           │   API     │
    └────┬─────┘           └──────┬────┘
         │                        │
         └────────┬───────────────┘
                  │
         ┌────────▼────────┐
         │   공유 리소스      │
         │  RDS / Redis    │
         └─────────────────┘
```

### 통신 흐름

**일반 사용자 플로우**:
1. 브라우저 → USER API (상품 조회, 주문)
2. USER API → RDS (데이터 저장)
3. USER API → Redis (캐시, 분산 락)
4. USER API → S3 (이미지 업로드)

**관리자 플로우**:
1. 관리자 페이지 → ADMIN API (상품 등록, 주문 관리)
2. ADMIN API → RDS (데이터 수정)
3. ADMIN API → Redis (캐시 무효화)
4. ADMIN API → S3 (이미지 업로드)

**데이터 동기화**:
- user가 RDS에 저장한 주문을 admin이 RDS에서 조회
- **같은 데이터베이스를 공유하므로 실시간 동기화**

### 프론트엔드 통신

**사용자 웹사이트**:
```javascript
// User API 호출
fetch('https://api.kt-techup.com/products')
fetch('https://api.kt-techup.com/orders')
```

**관리자 대시보드**:
```javascript
// Admin API 호출
fetch('https://admin-api.kt-techup.com/admin/products')
fetch('https://admin-api.kt-techup.com/admin/orders')
```

**서로 다른 도메인, 서로 다른 API 서버!**

---

## 배포 업데이트

### User 업데이트

```bash
# 1. 로컬에서 빌드
./gradlew :user:clean :user:zip

# 2. AWS Console → Elastic Beanstalk → kt-techup-shopping-user
# 3. Upload and deploy
# 4. shopping-user.zip 업로드

# Blue/Green 배포로 무중단 업데이트 가능
```

### Admin 업데이트

```bash
# 1. 로컬에서 빌드
./gradlew :admin:clean :admin:zip

# 2. AWS Console → Elastic Beanstalk → kt-techup-shopping-admin
# 3. Upload and deploy
# 4. shopping-admin.zip 업로드

# Admin 업데이트는 User에 영향 없음!
```

---

## 보안 그룹 설정 요약

### RDS Security Group (kt-techup-db-sg)

```
Inbound Rules:
  Type: MySQL/Aurora (3306)
  Source: kt-techup-user-sg
  
  Type: MySQL/Aurora (3306)
  Source: kt-techup-admin-sg
```

### Redis Security Group (kt-techup-redis-sg)

```
Inbound Rules:
  Type: Custom TCP (6379)
  Source: kt-techup-user-sg
  
  Type: Custom TCP (6379)
  Source: kt-techup-admin-sg
```

### User EC2 Security Group (kt-techup-user-sg)

```
Inbound Rules:
  Type: HTTP (80)
  Source: 0.0.0.0/0 (Load Balancer에서)
```

### Admin EC2 Security Group (kt-techup-admin-sg)

```
Inbound Rules:
  Type: HTTP (80)
  Source: 0.0.0.0/0 (Load Balancer에서)
  
  또는 특정 IP만 허용 (보안 강화):
  Source: 1.2.3.4/32 (회사 IP)
```

---

## FAQ

### Q1: Application 1개 vs 2개?
**A**: **1개만 만들면 됩니다.**
- Application: `kt-techup-shopping` (1개)
- Environment: `user`, `admin` (2개)

### Q2: EC2가 몇 대 뜨나요?
**A**: **최소 2대입니다.**
- USER Environment: 1~3대 (Auto Scaling)
- ADMIN Environment: 1대 고정

### Q3: user와 admin이 서로 통신하나요?
**A**: **아니요, 통신하지 않습니다.**
- 둘 다 독립적인 API 서버
- 같은 RDS/Redis를 공유할 뿐

### Q4: 데이터는 어떻게 공유되나요?
**A**: **같은 RDS를 사용합니다.**
- user가 주문 생성 → RDS에 저장
- admin이 주문 조회 → 같은 RDS에서 조회

### Q5: 환경 변수가 왜 같나요?
**A**: **같은 리소스를 공유하기 때문입니다.**
- RDS 엔드포인트: 동일
- Redis 엔드포인트: 동일
- S3 버킷: 동일
- JWT Secret: 동일 (중요!)

### Q6: 포트가 둘 다 8080인데 충돌 안 나나요?
**A**: **충돌하지 않습니다.**
- 각각 다른 EC2 인스턴스에서 실행
- Load Balancer를 통해 외부에서는 80포트로 접근

---

**작성일**: 2026-01-05  
**버전**: 1.0
