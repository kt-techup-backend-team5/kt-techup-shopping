# AWS 배포 핵심 요약 (30초 이해)

## 🎯 핵심 구조

```
Elastic Beanstalk Application: "kt-techup-shopping" (1개)
├── Environment: user   (EC2 1대)  ← shopping-user.zip
└── Environment: admin  (EC2 1대)  ← shopping-admin.zip

공유 리소스 (1개씩):
├── RDS (MySQL)      ← user, admin 둘 다 접속
├── Redis            ← user, admin 둘 다 접속
└── S3               ← user, admin 둘 다 사용
```

## ✅ 답변

### Q1: 애플리케이션 2개 만들어야 하나?
**A**: ❌ **아니요!** Application은 1개, Environment를 2개 만듭니다.
- Application: `kt-techup-shopping` (1개) ← 논리적 그룹
- Environment: `user`, `admin` (2개) ← 실제 배포

### Q2: 환경 변수를 2개로 하면 EC2 2개 뜨나?
**A**: ✅ **맞습니다!** Environment 2개 = EC2 2대
- USER Environment → EC2 #1 (user.jar 실행)
- ADMIN Environment → EC2 #2 (admin.jar 실행)

### Q3: User와 Admin이 어떻게 통신하나?
**A**: 🚫 **통신 안 합니다!** 완전히 독립적인 API 서버입니다.
- User API ← 일반 사용자가 호출
- Admin API ← 관리자가 호출
- 둘 다 **같은 RDS**에 접속해서 데이터 공유

## 📦 배포 명령어

```bash
# User 배포
./gradlew :user:clean :user:zip
# → user/build/distributions/shopping-user.zip 업로드

# Admin 배포
./gradlew :admin:clean :admin:zip
# → admin/build/distributions/shopping-admin.zip 업로드
```

## 💰 비용

- **EC2**: user (t3.small) + admin (t3.micro) = $22/월
- **RDS**: db.t3.micro = $15/월
- **Redis**: cache.t3.micro = $12/월
- **Load Balancer**: $32/월 (2개)
- **총**: **약 $90~120/월**

## 🔑 중요!

**JWT_SECRET_KEY는 user와 admin이 반드시 같아야 합니다!**
- user에서 발급한 토큰을 admin도 검증할 수 있어야 함

상세 내용은 **AWS-DEPLOYMENT.md** 참고
