# 멀티모듈 애플리케이션 실행 가이드

## 🎯 멀티모듈 구조 설명

이제 **user**와 **admin** 두 개의 독립적인 애플리케이션으로 분리되었습니다:

- **user** (shopping-user.jar): 일반 사용자용 API (회원가입, 로그인, 상품조회, 주문 등)
- **admin** (shopping-admin.jar): 관리자용 API (상품관리, 주문관리, 회원관리 등)

## 📋 실행 순서

### 1단계: ELK + 인프라 실행
```bash
# ELK, MySQL, Redis, LocalStack 모두 한번에 실행
docker-compose up -d

# 상태 확인
docker ps
```

**실행되는 서비스:**
- Elasticsearch (9200): 로그 저장소
- Logstash (5044): 로그 수집기
- Kibana (5601): 로그 UI
- MySQL (3306): 데이터베이스
- Redis (6379): 캐시
- LocalStack (4566): S3 (파일 저장)

### 2단계: 애플리케이션 실행

#### 방법 1: 개발 시 - IntelliJ에서 실행 (권장)

**User 애플리케이션:**
1. `user/src/main/java/com/kt/UserApplication.java` 우클릭
2. `Run 'UserApplication'`
3. http://localhost:8080 접속

**Admin 애플리케이션:**
1. `admin/src/main/java/com/kt/AdminApplication.java` 우클릭  
2. `Run 'AdminApplication'`
3. 포트 충돌 피하려면 `application.yml`에서 포트 변경:
   ```yaml
   server:
     port: 8081  # admin은 8081로 변경
   ```

#### 방법 2: JAR 파일 실행

**User 애플리케이션:**
```bash
java -jar user/build/libs/shopping-user.jar
```

**Admin 애플리케이션 (다른 터미널에서):**
```bash
# 포트 충돌 피하기
java -jar admin/build/libs/shopping-admin.jar --server.port=8081
```

#### 방법 3: Gradle로 실행

**User:**
```bash
JAVA_HOME=/Users/goorm/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home \
./gradlew :user:bootRun
```

**Admin:**
```bash
JAVA_HOME=/Users/goorm/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home \
./gradlew :admin:bootRun --args='--server.port=8081'
```

## 🔍 실행 확인

### User API (http://localhost:8080)
```bash
curl http://localhost:8080/actuator/health
```

### Admin API (http://localhost:8081)
```bash
curl http://localhost:8081/actuator/health
```

### Kibana 로그 확인 (http://localhost:5601)
1. 브라우저에서 http://localhost:5601 접속
2. Management > Stack Management > Index Patterns
3. `logstash-*` 패턴 생성
4. Discover에서 실시간 로그 확인

## 💡 개발 시 권장 사항

**로컬 개발 시:**
- 보통 **user만 실행**하면 됩니다 (일반 API 개발)
- admin 기능 개발할 때만 **admin 실행**

**둘 다 실행할 때:**
- user: 8080 포트
- admin: 8081 포트 (충돌 방지)

## 🛑 종료 방법

```bash
# 애플리케이션 종료: Ctrl+C

# ELK 스택 종료
docker-compose down

# 데이터까지 완전 삭제
docker-compose down -v
```

## ⚠️ Connection refused 에러 해결

`localhost:5044: connection failed` 에러가 나면:
```bash
# ELK 스택이 실행 중인지 확인
docker ps | grep -E "elasticsearch|logstash|kibana"

# 실행 안되어 있으면
docker-compose up -d

# 로그 확인
docker logs logstash
```

## 📦 AWS 배포 시

AWS에서는 두 애플리케이션을 **각각 Elastic Beanstalk에 배포**:
- shopping-user.zip → User API 서버
- shopping-admin.zip → Admin API 서버

이렇게 분리하면 **독립 확장** 가능!
