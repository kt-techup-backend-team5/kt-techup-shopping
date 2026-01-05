# ELK 스택 실행 가이드

## 필요한 파일 구조
```
kt-techup-javachip-team5/
├── docker-compose.yml          # ELK + MySQL + Redis + LocalStack 설정
├── init-s3.sh                  # LocalStack S3 초기화 스크립트
├── logstash/
│   └── pipeline/
│       └── logstash.conf       # Logstash 파이프라인 설정
└── elasticsearch/              # Elasticsearch 데이터 저장소 (자동 생성)
```

## 실행 방법

### 1. ELK 스택 시작
```bash
docker-compose up -d
```

### 2. 서비스 확인
- **Elasticsearch**: http://localhost:9200
- **Kibana**: http://localhost:5601
- **Logstash**: localhost:5044 (TCP)
- **MySQL**: localhost:3306
- **Redis**: localhost:6379
- **LocalStack S3**: localhost:4566

### 3. 애플리케이션 실행
```bash
# User 애플리케이션
java -jar user/build/libs/shopping-user.jar

# Admin 애플리케이션
java -jar admin/build/libs/shopping-admin.jar
```

### 4. Kibana에서 로그 확인
1. http://localhost:5601 접속
2. Management > Stack Management > Index Patterns
3. `logstash-*` 패턴 생성
4. Discover 메뉴에서 로그 확인

## 로그 전송 설정

애플리케이션의 `logback-spring.xml`에 이미 Logstash appender가 설정되어 있습니다:
- 모든 로그가 `localhost:5044`로 JSON 형식으로 전송됩니다
- Elasticsearch에 `logstash-YYYY.MM.dd` 인덱스로 저장됩니다

## 중지 방법
```bash
docker-compose down

# 데이터까지 삭제
docker-compose down -v
```
