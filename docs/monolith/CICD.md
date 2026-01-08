# GitHub Actions CI/CD 자동화 배포

## 📋 목차
1. [CI/CD 개요](#cicd-개요)
2. [워크플로우 구조](#워크플로우-구조)
3. [자동화 배포 프로세스](#자동화-배포-프로세스)
4. [GitHub Secrets 설정](#github-secrets-설정)
5. [배포 파이프라인 상세](#배포-파이프라인-상세)
6. [트러블슈팅](#트러블슈팅)

---

## CI/CD 개요

### GitHub Actions란?
GitHub에서 제공하는 CI/CD 플랫폼으로, 코드 변경 시 자동으로 빌드, 테스트, 배포를 수행합니다.

### 우리 프로젝트의 CI/CD 전략

```
┌──────────────────────────────────────────────────────────┐
│              GitHub Repository                           │
│                                                          │
│  1. 개발자가 코드 Push                                      │
│     └─► deploy/monolith 브랜치                             │
└────────────────────┬─────────────────────────────────────┘
                     │ Trigger
                     ▼
┌──────────────────────────────────────────────────────────┐
│              GitHub Actions Workflow                     │
│                                                          │
│  ┌─────────────────────────────────────────────────┐     │
│  │  Job 1: Test (자동 테스트)                         │     │
│  │  • MySQL/Redis 시작                              │     │
│  │  • ./gradlew test 실행                           │     │
│  │  • 테스트 결과 발행                                 │     │
│  │  • 실패 시 Slack 알림                              │     │
│  └─────────────────┬───────────────────────────────┘     │
│                    │ 테스트 통과                            │
│                    ▼                                     │
│  ┌─────────────────────────────────────────────────┐     │
│  │  Job 2: Deploy (AWS 배포)                        │     │
│  │  • Gradle 빌드 (bootJar)                         │     │
│  │  • 배포 패키지 생성 (Procfile + jar → zip)          │     │
│  │  • AWS Elastic Beanstalk 배포                    │     │
│  │  • 성공/실패 Slack 알림                            │     │
│  └─────────────────┬───────────────────────────────┘     │
└────────────────────┼─────────────────────────────────────┘
                     │ 배포 완료
                     ▼
┌──────────────────────────────────────────────────────────┐
│        AWS Elastic Beanstalk (운영 환경)                   │
│                                                          │
│  • EC2 Instance (t3.small)                               │
│  • Java 21 (Amazon Corretto)                             │
│  • Application Running (Port 8080)                       │
└──────────────────────────────────────────────────────────┘
```

---

## 워크플로우 구조

### 파일 위치
```
.github/workflows/deploy-monolith.yml
```

### 트리거 조건

**자동 실행 조건**:
```yaml
on:
  push:
    branches:
      - deploy/monolith    # 이 브랜치에 push 시 자동 배포
  pull_request:
    branches:
      - deploy/monolith    # PR 생성 시 테스트만 실행
```

**실행 시나리오**:
1. **PR 생성 시**: 테스트만 실행 (배포 X)
2. **deploy/monolith에 Push 시**: 테스트 + 배포 자동 실행

---

## 자동화 배포 프로세스

### 전체 플로우

```
[1단계] 코드 Push
  ↓
[2단계] GitHub Actions 트리거
  ↓
[3단계] 테스트 Job 실행
  ├─ MySQL 시작
  ├─ Redis 시작
  ├─ Gradle 테스트 실행
  └─ 테스트 실패 시 → Slack 알림 → 중단
  ↓
[4단계] 배포 Job 실행 (테스트 통과 시)
  ├─ Gradle bootJar 빌드
  ├─ 배포 패키지 생성 (zip)
  ├─ AWS Elastic Beanstalk 배포
  └─ 배포 결과 Slack 알림
  ↓
[5단계] AWS에서 애플리케이션 실행
  ├─ 새 버전 배포
  ├─ Health Check
  └─ 이전 버전과 교체 (Rolling Update)
```

---

## GitHub Secrets 설정

### 필수 Secrets

GitHub 리포지토리 → Settings → Secrets and variables → Actions에서 설정:

```yaml
# AWS 배포용
AWS_ACCESS_KEY: AKIA...              # AWS IAM 액세스 키
AWS_SECRET_KEY: ...                  # AWS IAM 시크릿 키

# 애플리케이션 환경 변수
OPENAI_API_KEY: sk-proj-...          # OpenAI API 키
SLACK_API_TOKEN: xoxb-...            # Slack Bot 토큰
SLACK_LOG_CHANNEL: C...              # Slack 로그 채널 ID
SLACK_WEBHOOK_URL: https://...       # Slack Webhook URL (배포 알림용)

# DB 테스트용 (GitHub Actions 내에서만 사용)
# MySQL/Redis는 GitHub Actions 환경에서 자동 시작
```

### Secrets 사용 예시

```yaml
- name: Run tests
  run: ./gradlew test
  env:
    OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
    AWS_ACCESS_KEY: ${{ secrets.AWS_ACCESS_KEY }}
```

---

## 배포 파이프라인 상세

### Job 1: Test (자동 테스트)

**목적**: 코드 품질 검증 및 회귀 방지

**실행 단계**:

#### 1. Checkout 코드
```yaml
- name: Checkout code
  uses: actions/checkout@v4
```
→ GitHub 리포지토리의 최신 코드를 가져옵니다.

#### 2. JDK 21 설정
```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: 'gradle'  # Gradle 의존성 캐싱으로 빌드 속도 향상
```
→ Java 21 (Temurin OpenJDK) 설치 및 Gradle 캐시 활성화

#### 3. MySQL 시작
```yaml
- name: Start MySQL
  run: |
    sudo /etc/init.d/mysql start
    mysql -e 'CREATE DATABASE shopping;' -uroot -proot
```
→ GitHub Actions 환경의 MySQL 서비스 시작 및 DB 생성

#### 4. Redis 시작
```yaml
- name: Start Redis
  uses: supercharge/redis-github-action@1.8.0
  with:
    redis-version: 7
```
→ Redis 7.0 컨테이너 실행

#### 5. Gradle 테스트 실행
```yaml
- name: Run tests
  run: ./gradlew test --no-daemon
  env:
    SPRING_PROFILES_ACTIVE: test
    DB_HOST: localhost
    DB_USERNAME: root
    DB_PASSWORD: root
    OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
```
→ JUnit 테스트 실행 (통합 테스트 제외)

#### 6. 테스트 결과 발행
```yaml
- name: Publish test results
  uses: EnricoMi/publish-unit-test-result-action@v2
  if: always()  # 테스트 실패해도 결과 발행
  with:
    files: '**/build/test-results/**/*.xml'
```
→ 테스트 결과를 GitHub Actions UI에 표시

#### 7. 테스트 실패 시 Slack 알림
```yaml
- name: Send Slack notification (Test Failure)
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: custom
    custom_payload: |
      {
        "attachments": [{
          "color": "danger",
          "title": "❌ 모놀리식 테스트 실패",
          "text": "테스트가 실패하여 배포가 중단되었습니다.",
          ...
        }]
      }
    webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
```
→ 테스트 실패 시 Slack으로 즉시 알림 전송

---

### Job 2: Deploy (AWS 배포)

**조건**:
- Job 1 (Test) 성공 시에만 실행
- `push` 이벤트이고 `deploy/monolith` 브랜치일 때만 실행 (PR은 배포 X)

```yaml
deploy:
  needs: test  # test Job이 성공해야 실행
  if: github.event_name == 'push' && github.ref == 'refs/heads/deploy/monolith'
```

**실행 단계**:

#### 1. 환경 변수 설정
```yaml
- name: Set environment variables
  run: |
    echo "APP_NAME=kt-techup-shopping-monolith" >> $GITHUB_ENV
    echo "TIMESTAMP=$(date +'%Y%m%d-%H%M%S')" >> $GITHUB_ENV
```
→ 애플리케이션 이름과 타임스탬프 설정 (버전 관리용)
- 예: `kt-techup-shopping-monolith-20260108-143052`

#### 2. JDK 21 설정
```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: 'gradle'
```

#### 3. Gradle 빌드 (bootJar)
```yaml
- name: Build application
  run: ./gradlew bootJar --no-daemon -x test
```
→ 실행 가능한 JAR 파일 생성 (`build/libs/shopping.jar`)
- `-x test`: 테스트 스킵 (이미 Job 1에서 실행)
- `--no-daemon`: GitHub Actions 환경에서 안정성 향상

#### 4. 배포 패키지 준비
```yaml
- name: Prepare deployment package
  run: |
    mkdir -p deploy
    cp build/libs/*.jar deploy/application.jar
    echo "web: java -Dserver.port=5000 -jar application.jar" > deploy/Procfile
    cd deploy
    zip -r ${{ env.APP_NAME }}-${{ env.TIMESTAMP }}.zip .
    mv ${{ env.APP_NAME }}-${{ env.TIMESTAMP }}.zip ../
```

**생성되는 파일 구조**:
```
kt-techup-shopping-monolith-20260108-143052.zip
├── application.jar          # Spring Boot 실행 가능 JAR
└── Procfile                 # Elastic Beanstalk 실행 명령어
```

**Procfile 내용**:
```
web: java -Dserver.port=5000 -jar application.jar
```
→ Elastic Beanstalk는 포트 5000을 기본으로 사용하므로 명시적으로 설정

#### 5. AWS Elastic Beanstalk 배포
```yaml
- name: Deploy to AWS Elastic Beanstalk
  uses: einaregilsson/beanstalk-deploy@v22
  with:
    aws_access_key: ${{ secrets.AWS_ACCESS_KEY }}
    aws_secret_key: ${{ secrets.AWS_SECRET_KEY }}
    application_name: kt-techup-shopping-monolith
    environment_name: kt-techup-shopping-monolith-prod
    version_label: kt-techup-shopping-monolith-20260108-143052
    region: ap-northeast-2
    deployment_package: kt-techup-shopping-monolith-20260108-143052.zip
```

**배포 프로세스**:
1. S3에 배포 패키지 업로드
2. Elastic Beanstalk에 새 버전 등록
3. 환경에 배포 (Rolling Update)
4. Health Check 수행
5. 배포 완료

#### 6. 배포 성공 시 Slack 알림
```yaml
- name: Send Slack notification (Success)
  if: success()
  uses: 8398a7/action-slack@v3
  with:
    custom_payload: |
      {
        "attachments": [{
          "color": "good",
          "title": "✅ 모놀리식 배포 성공",
          "fields": [
            {
              "title": "Environment",
              "value": "kt-techup-shopping-monolith-prod"
            },
            {
              "title": "Version",
              "value": "20260108-143052"
            },
            ...
          ]
        }]
      }
```

#### 7. 배포 실패 시 Slack 알림
```yaml
- name: Send Slack notification (Failure)
  if: failure()
  # ... (실패 메시지)
```

---

## 배포 워크플로우 다이어그램

```
┌──────────────────────────────────────────────────────────┐
│  [개발자] 코드 Push → deploy/monolith                       │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│  GitHub Actions 트리거                                     │
│  • 워크플로우: deploy-monolith.yml                          │
└────────────────────┬─────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌───────────────┐         ┌───────────────┐
│  Job 1: Test  │         │  Job 2: Deploy│
│  (병렬 실행)    │         │   (순차 실행)   │
└───────┬───────┘         └───────┬───────┘
        │                         │
        ▼                         │
┌───────────────┐                 │
│ MySQL 시작     │                 │
│ Redis 시작     │                 │
│ Gradle 테스트   │                 │
└───────┬───────┘                 │
        │                         │
    ┌───┴───┐                     │
    │ 성공?  │                     │
    └───┬───┘                     │
        │                         │
   ┌────┼────┐                    │
   NO   │    YES                  │
   │    │    │                    │
   ▼    │    └────────────────────┤
[Slack │                         ▼
 알림]  │                  ┌───────────────┐
[중단]  │                  │ Gradle bootJar│
        │                 │   패키지 생성    │
        │                 │   AWS 배포     │
        │                 └───────┬───────┘
        │                         │
        │                     ┌───┴───┐
        │                     │ 성공?  │
        │                     └───┬───┘
        │                         │
        │                    ┌────┼────┐
        │                    │    │    │
        │                   YES   │    NO
        │                    │    │    │
        │                    ▼    │    ▼
        │              [Slack     │  [Slack
        │               성공       │   실패
        │               알림]      │   알림]
        │                         │
        └─────────────────────────┘
```

---

## Elastic Beanstalk 환경 설정

### AWS 콘솔에서 설정해야 할 항목

**1. 애플리케이션 생성**:
```
Application Name: kt-techup-shopping-monolith
```

**2. 환경 생성**:
```
Environment Name: kt-techup-shopping-monolith-prod
Platform: Java 21 (Amazon Corretto)
Instance Type: t3.small (2vCPU, 2GB RAM)
```

**3. 환경 변수 설정** (Configuration → Software → Environment properties):
```
SPRING_PROFILES_ACTIVE=prod
DB_HOST=your-mysql-endpoint.rds.amazonaws.com
DB_USERNAME=admin
DB_PASSWORD=your-password
OPENAI_API_KEY=sk-proj-...
AWS_ACCESS_KEY=AKIA...
AWS_SECRET_KEY=...
SLACK_API_TOKEN=xoxb-...
SLACK_LOG_CHANNEL=C...
redis.host=your-redis-endpoint.cache.amazonaws.com:6379
```

**4. 로드 밸런서 설정**:
```
Health check path: /actuator/health
Health check interval: 30 seconds
```

**5. Auto Scaling 설정** (선택 사항):
```
Min instances: 1
Max instances: 4
Scaling trigger: CPU > 70%
```

---

## 배포 모니터링

### 1. GitHub Actions 로그 확인

**경로**: GitHub 리포지토리 → Actions → 워크플로우 선택

**확인 사항**:
- 각 Step의 실행 시간
- 테스트 결과
- 빌드 로그
- 배포 상태

### 2. Elastic Beanstalk 콘솔

**AWS 콘솔** → Elastic Beanstalk → 환경 선택

**확인 사항**:
- **Health**: Green (정상), Yellow (경고), Red (오류)
- **Recent Events**: 배포 이벤트 로그
- **Logs**: 애플리케이션 로그 다운로드

### 3. Slack 알림

**알림 종류**:
- ✅ 테스트 성공
- ❌ 테스트 실패
- ✅ 배포 성공 (환경, 버전, 커밋 정보 포함)
- ❌ 배포 실패

---

## 트러블슈팅

### 문제 1: 테스트 실패

**증상**: GitHub Actions에서 테스트가 실패하고 배포가 중단됨

**해결 방법**:
```bash
# 로컬에서 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "ProductServiceTest"

# 테스트 로그 상세히 보기
./gradlew test --info
```

**원인**:
- 환경 변수 누락 (Secrets 확인)
- DB 연결 실패 (MySQL/Redis 상태 확인)
- 코드 오류 (로컬에서 먼저 테스트)

---

### 문제 2: 빌드 실패

**증상**: `./gradlew bootJar` 실패

**해결 방법**:
```bash
# 로컬에서 빌드 시도
./gradlew clean bootJar

# Gradle 캐시 삭제
./gradlew clean --refresh-dependencies
```

**원인**:
- 의존성 문제
- 컴파일 에러
- Gradle 버전 불일치

---

### 문제 3: AWS 배포 실패

**증상**: Elastic Beanstalk 배포가 실패하고 Red 상태

**해결 방법**:

**1. AWS 콘솔에서 로그 확인**:
```
Elastic Beanstalk → 환경 → Logs → Request Logs → Last 100 Lines
```

**2. 일반적인 원인**:

**A. Health Check 실패**:
```
해결: /actuator/health 엔드포인트가 정상 응답하는지 확인
```

**B. 환경 변수 누락**:
```
해결: Configuration → Software → Environment properties에서 모든 변수 확인
```

**C. 포트 불일치**:
```
해결: Procfile에서 -Dserver.port=5000 확인
```

**D. 메모리 부족**:
```
해결: Instance Type을 t3.medium으로 증가
```

---

### 문제 4: Slack 알림이 오지 않음

**원인**: SLACK_WEBHOOK_URL이 잘못되었거나 만료됨

**해결 방법**:
1. Slack에서 새 Webhook URL 생성
2. GitHub Secrets → SLACK_WEBHOOK_URL 업데이트
3. 워크플로우 재실행

---

## 배포 롤백

### 방법 1: AWS 콘솔에서 롤백

```
Elastic Beanstalk → 환경 → Application versions
→ 이전 버전 선택 → Deploy
```

### 방법 2: Git 커밋 롤백 후 재배포

```bash
# 로컬에서 이전 커밋으로 되돌리기
git revert HEAD

# deploy/monolith 브랜치에 Push
git push origin deploy/monolith
```
→ 자동으로 새 버전 배포

---

## 배포 주기

**권장 배포 주기**:
- **긴급 버그 수정**: 즉시 배포
- **기능 추가**: 1~2일마다 배포
- **대규모 변경**: 테스트 후 배포

**배포 전 체크리스트**:
- [ ] 로컬에서 모든 테스트 통과
- [ ] 코드 리뷰 완료
- [ ] Secrets 설정 확인
- [ ] 배포 시간대 확인 (사용자 적은 시간)
- [ ] 롤백 계획 수립

---

## 배포 성능 최적화

### 1. Gradle 캐싱

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    cache: 'gradle'  # 의존성 캐시로 빌드 시간 단축
```
→ 첫 빌드: 3~5분, 캐시 사용 시: 1~2분

### 2. 테스트 병렬 실행

```yaml
- name: Run tests
  run: ./gradlew test --parallel --max-workers=4
```

### 3. 배포 패키지 최적화

```yaml
# jar 파일 크기 줄이기 (build.gradle)
bootJar {
    archiveFileName = 'application.jar'
    excludes = ['**/logback-test.xml', '**/application-test.yml']
}
```

---

## 보안 고려사항

### 1. Secrets 관리
- ✅ 모든 민감 정보는 GitHub Secrets에 저장
- ❌ 코드에 직접 하드코딩 금지

### 2. IAM 권한 최소화
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:CreateApplicationVersion",
        "elasticbeanstalk:UpdateEnvironment",
        "s3:PutObject"
      ],
      "Resource": "*"
    }
  ]
}
```

### 3. 브랜치 보호 규칙
```
Settings → Branches → Add rule
- Require pull request reviews
- Require status checks to pass (test Job)
```

---

**문서 작성일**: 2026-01-08
**버전**: 1.0
