# 이벤트 기반 설계 (Event-Driven Architecture)

## 📋 목차
1. [이벤트 기반 설계란?](#이벤트-기반-설계란)
2. [도입 배경 및 이유](#도입-배경-및-이유)
3. [시스템 구조](#시스템-구조)
4. [장단점 분석](#장단점-분석)
5. [실제 적용 사례](#실제-적용-사례)
6. [설계 결정 과정](#설계-결정-과정)
7. [트러블슈팅](#트러블슈팅)

---

## 이벤트 기반 설계란?

### 개념

**이벤트 기반 아키텍처 (Event-Driven Architecture)**는 **도메인 간의 직접적인 의존성을 제거**하고, **이벤트**를 통해 느슨하게 결합된 시스템을 구축하는 설계 방식입니다.

### 핵심 원리

```
[전통적인 방식 - 직접 호출]
PaymentService.pay()
  ↓ (직접 호출)
orderService.updateStatus()  ← Payment가 Order에 직접 의존
  ↓ (직접 호출)
pointService.refund()         ← Payment가 Point에 직접 의존
  ↓ (직접 호출)
slackService.notify()         ← Payment가 Slack에 직접 의존


[이벤트 기반 방식 - 이벤트 발행/구독]
PaymentService.pay()
  ↓ (이벤트 발행)
eventPublisher.publishEvent(PaymentEvent.Success)
  │
  ├─► OrderEventListener.onPaymentSuccess()    ← 독립적으로 동작
  ├─► PointEventListener.onPaymentSuccess()    ← 독립적으로 동작
  └─► SlackEventListener.onPaymentSuccess()    ← 독립적으로 동작
```

---

## 도입 배경 및 이유

### 1. 문제 상황

**초기 설계 (이벤트 도입 전)**:
```java
@Service
public class PaymentService {
    private final OrderService orderService;      // 의존성 1
    private final PointService pointService;      // 의존성 2
    private final SlackService slackService;      // 의존성 3
    private final RefundService refundService;    // 의존성 4

    public void pay(Long orderId) {
        // 결제 처리
        Payment payment = createPayment();

        // 여러 서비스를 직접 호출 (강한 결합)
        orderService.updateStatus(orderId, ORDER_ACCEPTED);
        slackService.notify("결제 완료: " + orderId);

        // 만약 결제가 실패하면?
        if (paymentFailed) {
            orderService.cancel(orderId);         // 순서 중요!
            pointService.refund(userId, amount);  // 순서 중요!
            slackService.notify("결제 실패");     // 순서 중요!
        }
    }
}
```

**문제점**:
1. **강한 결합** (Tight Coupling)
   - PaymentService가 Order, Point, Slack 등 모든 서비스에 직접 의존
   - 새로운 부가 기능 추가 시 PaymentService 수정 필요

2. **단일 책임 원칙 위반** (SRP Violation)
   - PaymentService가 결제 + 주문 상태 변경 + 포인트 환불 + 알림까지 담당

3. **순서 의존성**
   - 각 호출의 순서가 중요하고 복잡함
   - 순서가 바뀌면 버그 발생 가능

4. **확장성 부족**
   - 새로운 부가 작업 추가 시 코드 수정 필요
   - 예: 결제 완료 시 이메일 발송 추가 → PaymentService 수정

5. **테스트 어려움**
   - PaymentService 테스트 시 모든 의존 서비스 Mocking 필요

### 2. 해결 방안: 이벤트 기반 설계

**이벤트 도입 후**:
```java
@Service
public class PaymentService {
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행자만 의존

    public void pay(Long orderId) {
        // 결제 처리
        Payment payment = createPayment();
        payment.markAsSuccess();

        // 이벤트 발행 (관심사 분리)
        eventPublisher.publishEvent(
            new PaymentEvent.Success(payment.getId(), orderId)
        );
    }
}

// Order 도메인에서 독립적으로 처리
@Component
public class OrderEventListener {
    @EventListener(PaymentEvent.Success.class)
    public void onPaymentSuccess(PaymentEvent.Success event) {
        Order order = orderRepository.findById(event.orderId());
        order.acceptPayment(event.paymentId()); // Order 상태 변경
    }
}

// Point 도메인에서 독립적으로 처리
@Component
public class PointEventListener {
    @EventListener(PaymentEvent.Failed.class)
    public void onPaymentFailed(PaymentEvent.Failed event) {
        pointService.refund(event.orderId()); // 포인트 환불
    }
}
```

**장점**:
- ✅ PaymentService는 **결제 처리만** 담당 (단일 책임)
- ✅ 각 도메인이 **독립적으로** 이벤트에 반응
- ✅ 새로운 부가 기능 추가 시 **기존 코드 수정 불필요**

---

## 시스템 구조

### 전체 이벤트 플로우

```
┌──────────────────────────────────────────────────────────────┐
│                    Event Publisher                            │
│                 (이벤트 발행 주체)                              │
├──────────────────────────────────────────────────────────────┤
│  PaymentService                                               │
│  • publishEvent(PaymentEvent.Success)                        │
│  • publishEvent(PaymentEvent.Failed)                         │
│  • publishEvent(PaymentEvent.Cancelled)                      │
│                                                               │
│  OrderService                                                 │
│  • publishEvent(OrderEvent.Created)                          │
│  • publishEvent(OrderEvent.Cancelled)                        │
│                                                               │
│  RefundService                                                │
│  • publishEvent(RefundEvent.Requested)                       │
└────────────────────┬─────────────────────────────────────────┘
                     │ Spring ApplicationEventPublisher
                     │ (이벤트 브로커)
                     ▼
┌──────────────────────────────────────────────────────────────┐
│                   Event Listeners                             │
│                 (이벤트 구독자)                                 │
├──────────────────────────────────────────────────────────────┤
│  OrderEventListener                                           │
│  • @EventListener(PaymentEvent.Success)                      │
│    → Order 상태를 ORDER_ACCEPTED로 변경                       │
│  • @EventListener(PaymentEvent.Failed)                       │
│    → Order 상태를 ORDER_CANCELLED로 변경                      │
│                                                               │
│  PointEventListener                                           │
│  • @EventListener(OrderEvent.Delivered)                      │
│    → 배송 완료 7일 후 포인트 적립                              │
│  • @EventListener(PaymentEvent.Failed)                       │
│    → 결제 실패 시 사용한 포인트 복구                           │
│                                                               │
│  SlackEventListener (향후 구현 예정)                          │
│  • @EventListener(PaymentEvent.Success)                      │
│    → Slack으로 결제 완료 알림                                  │
└──────────────────────────────────────────────────────────────┘
```

### 도메인 이벤트 정의

**1. PaymentEvent** (결제 이벤트):
```java
public class PaymentEvent {
    // 결제 성공
    public record Success(Long paymentId, Long orderId) {}

    // 결제 실패
    public record Failed(Long paymentId, Long orderId, String reason) {}

    // 결제 취소
    public record Cancelled(Long paymentId, Long orderId) {}
}
```

**2. OrderEvent** (주문 이벤트):
```java
public class OrderEvent {
    // 주문 생성
    public record Created(Long orderId, Long userId) {}

    // 주문 취소
    public record Cancelled(Long orderId, Long userId) {}
}
```

**3. RefundEvent** (환불 이벤트):
```java
public class RefundEvent {
    // 환불 요청
    public record Requested(Long refundId, Long orderId) {}
}
```

---

## 장단점 분석

### ✅ 장점

#### 1. 느슨한 결합 (Loose Coupling)

**Before (강한 결합)**:
```java
@Service
public class PaymentService {
    private final OrderService orderService;        // 직접 의존
    private final PointService pointService;        // 직접 의존
    private final SlackService slackService;        // 직접 의존

    public void pay(Long orderId) {
        // PaymentService가 모든 서비스를 알아야 함
        orderService.updateStatus();
        pointService.refund();
        slackService.notify();
    }
}
```

**After (느슨한 결합)**:
```java
@Service
public class PaymentService {
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행자만 의존

    public void pay(Long orderId) {
        // PaymentService는 결제만 처리
        eventPublisher.publishEvent(new PaymentEvent.Success(...));
        // 누가 이벤트를 받는지 몰라도 됨!
    }
}
```

**효과**:
- PaymentService가 Order, Point, Slack 서비스를 **몰라도 됨**
- 각 도메인이 **독립적으로 발전** 가능

#### 2. 단일 책임 원칙 (SRP) 준수

**Before**:
```java
public void pay(Long orderId) {
    // 1. 결제 처리 (본 책임)
    Payment payment = createPayment();

    // 2. 주문 상태 변경 (부가 책임)
    orderService.updateStatus(orderId);

    // 3. 포인트 환불 (부가 책임)
    pointService.refund(userId);

    // 4. 알림 발송 (부가 책임)
    slackService.notify("결제 완료");
}
```

**After**:
```java
public void pay(Long orderId) {
    // 1. 결제 처리 (본 책임만)
    Payment payment = createPayment();
    payment.markAsSuccess();

    // 2. 이벤트 발행 (관심사 분리)
    eventPublisher.publishEvent(new PaymentEvent.Success(...));
}
```

**효과**:
- PaymentService는 **결제 처리만** 담당
- 부가 작업은 각 도메인의 리스너가 **독립적으로** 처리

#### 3. 확장성 (Scalability)

**새로운 부가 기능 추가 시**:

**Before (기존 코드 수정)**:
```java
public void pay(Long orderId) {
    // 기존 코드
    orderService.updateStatus();
    slackService.notify();

    // 새로운 기능 추가 → PaymentService 수정 필요 ❌
    emailService.send("결제 완료");        // 추가
    smsService.send("결제 완료");          // 추가
}
```

**After (새 리스너만 추가)**:
```java
// PaymentService는 수정 불필요 ✅
// 새로운 리스너만 추가
@Component
public class EmailEventListener {
    @EventListener(PaymentEvent.Success.class)
    public void onPaymentSuccess(PaymentEvent.Success event) {
        emailService.send("결제 완료");
    }
}

@Component
public class SmsEventListener {
    @EventListener(PaymentEvent.Success.class)
    public void onPaymentSuccess(PaymentEvent.Success event) {
        smsService.send("결제 완료");
    }
}
```

**효과**:
- 기존 코드 **수정 없이** 새 기능 추가 (Open-Closed Principle)
- 각 리스너가 **독립적으로** 배포 가능

#### 4. 비동기 처리 가능 (향후 확장)

**현재**:
```java
@EventListener(PaymentEvent.Success.class)
public void onPaymentSuccess(PaymentEvent.Success event) {
    // 동기 처리 (같은 트랜잭션)
    order.acceptPayment();
}
```

**향후 확장**:
```java
@Async  // 비동기 처리
@EventListener(PaymentEvent.Success.class)
public void onPaymentSuccess(PaymentEvent.Success event) {
    // 별도 스레드에서 처리 (성능 향상)
    slackService.notify("결제 완료");
}
```

**효과**:
- 시간이 오래 걸리는 작업(알림, 이메일 등)을 **비동기 처리** 가능
- 메인 트랜잭션 성능 향상

#### 5. 테스트 용이성

**Before**:
```java
@Test
void 결제_테스트() {
    // 모든 의존 서비스 Mocking 필요
    given(orderService.updateStatus(...)).willReturn(...);
    given(pointService.refund(...)).willReturn(...);
    given(slackService.notify(...)).willReturn(...);

    paymentService.pay(orderId);

    verify(orderService).updateStatus(...);
    verify(pointService).refund(...);
    verify(slackService).notify(...);
}
```

**After**:
```java
@Test
void 결제_테스트() {
    // PaymentService만 테스트 (이벤트 발행 검증)
    paymentService.pay(orderId);

    // Payment 상태만 검증
    assertThat(payment.getStatus()).isEqualTo(PAYMENT_SUCCESS);
}

@Test
void 이벤트_리스너_테스트() {
    // OrderEventListener만 독립적으로 테스트
    listener.onPaymentSuccess(new PaymentEvent.Success(...));

    assertThat(order.getStatus()).isEqualTo(ORDER_ACCEPTED);
}
```

**효과**:
- 각 컴포넌트를 **독립적으로** 테스트 가능
- Mock 객체 수 감소

---

### ❌ 단점

#### 1. 디버깅 어려움

**문제**:
- 이벤트 발행 → 리스너 호출이 **간접적**이라 추적 어려움
- IDE에서 "Find Usages"로 호출 추적 불가

**예시**:
```java
// PaymentService.java
eventPublisher.publishEvent(new PaymentEvent.Success(...));
// ← 누가 이 이벤트를 받는지 코드에서 직접 보이지 않음
```

**해결 방법**:
- 로그 추가:
  ```java
  @EventListener(PaymentEvent.Success.class)
  public void onPaymentSuccess(PaymentEvent.Success event) {
      log.info("결제 성공 이벤트 수신 - paymentId: {}", event.paymentId());
      // ...
  }
  ```
- 이벤트 문서화 (이 문서!)

#### 2. 트랜잭션 경계 불명확

**문제**:
- 기본적으로 이벤트 리스너는 **같은 트랜잭션**에서 실행
- 리스너에서 예외 발생 시 전체 롤백

**예시**:
```java
@Transactional
public void pay(Long orderId) {
    // 1. 결제 처리
    payment.markAsSuccess();

    // 2. 이벤트 발행
    eventPublisher.publishEvent(new PaymentEvent.Success(...));
} // ← 트랜잭션 커밋 전에 리스너 실행

@EventListener
@Transactional
public void onPaymentSuccess(PaymentEvent.Success event) {
    order.acceptPayment(); // 예외 발생 시 결제까지 롤백됨!
}
```

**해결 방법**:
- `@TransactionalEventListener` 사용:
  ```java
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPaymentSuccess(PaymentEvent.Success event) {
      // 트랜잭션 커밋 후 실행
  }
  ```

#### 3. 이벤트 순서 보장 안됨

**문제**:
- 여러 리스너가 있을 때 실행 순서가 **보장되지 않음**

**예시**:
```java
// 두 리스너의 실행 순서는?
@EventListener(PaymentEvent.Success.class)
public void listener1() { /* ... */ }

@EventListener(PaymentEvent.Success.class)
public void listener2() { /* ... */ }
```

**해결 방법**:
- `@Order` 어노테이션:
  ```java
  @Order(1)
  @EventListener(PaymentEvent.Success.class)
  public void listener1() { /* 먼저 실행 */ }

  @Order(2)
  @EventListener(PaymentEvent.Success.class)
  public void listener2() { /* 나중에 실행 */ }
  ```

#### 4. 학습 곡선

**문제**:
- 초보 개발자에게 이벤트 플로우 이해 어려움
- "누가 이 이벤트를 발행하지?", "어디서 처리되지?" 파악 필요

**해결 방법**:
- 명확한 문서화 (이 문서!)
- 이벤트 네이밍 규칙 준수
- 주석 작성

#### 5. 오버엔지니어링 위험

**문제**:
- 간단한 로직에도 이벤트를 사용하면 복잡도만 증가

**예시**:
```java
❌ Bad (불필요한 이벤트)
public void updateUserName(String newName) {
    user.setName(newName);
    eventPublisher.publishEvent(new UserNameChangedEvent(...)); // 불필요
}

✅ Good (직접 호출)
public void updateUserName(String newName) {
    user.setName(newName); // 단순 업데이트는 직접 처리
}
```

**원칙**:
- **도메인 간 결합을 끊어야 할 때**만 이벤트 사용
- 같은 도메인 내에서는 직접 호출

---

## 실제 적용 사례

### 사례 1: Payment → Order 상태 변경

**시나리오**: 결제 성공 시 주문 상태를 `ORDER_ACCEPTED`로 변경

**Before (직접 호출)**:
```java
@Service
public class PaymentService {
    private final OrderRepository orderRepository; // Order에 직접 의존

    public void pay(Long orderId) {
        // 결제 처리
        Payment payment = createPayment();

        // Order 상태 직접 변경 (강한 결합)
        Order order = orderRepository.findById(orderId);
        order.acceptPayment(payment.getId());
    }
}
```

**문제점**:
- PaymentService가 OrderRepository에 직접 의존
- Payment 도메인이 Order 도메인의 내부 로직(`acceptPayment`)을 알아야 함

**After (이벤트 기반)**:
```java
// PaymentService.java (발행자)
@Service
public class PaymentService {
    private final ApplicationEventPublisher eventPublisher;

    public void pay(Long orderId) {
        Payment payment = createPayment();
        payment.markAsSuccess();

        // 이벤트 발행 (Order 모름)
        eventPublisher.publishEvent(
            new PaymentEvent.Success(payment.getId(), orderId)
        );
    }
}

// OrderEventListener.java (구독자)
@Component
@Transactional
public class OrderEventListener {
    private final OrderRepository orderRepository;

    @EventListener(PaymentEvent.Success.class)
    public void onPaymentSuccess(PaymentEvent.Success event) {
        log.info("결제 성공 이벤트 수신 - orderId: {}", event.orderId());

        Order order = orderRepository.findById(event.orderId());
        order.acceptPayment(event.paymentId());

        log.info("주문 상태 변경 완료 - status: {}", order.getStatus());
    }
}
```

**효과**:
- ✅ PaymentService는 Order를 몰라도 됨
- ✅ Order 도메인이 자신의 로직을 독립적으로 관리

**테스트**:
```java
@Test
@DisplayName("결제 성공 시 PaymentEvent.Success 발행 → Order 상태 ORDER_ACCEPTED")
void 결제_성공_이벤트_플로우() {
    // when
    paymentService.pay(orderId, paymentType);

    // then - Order 상태 확인 (이벤트를 통해 변경됨)
    Order updatedOrder = orderRepository.findById(orderId).get();
    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.ORDER_ACCEPTED);
}
```

---

### 사례 2: Payment 실패 → Order 취소 + Point 환불

**시나리오**: 결제 실패 시 주문 취소 및 사용한 포인트 환불

**Before (직접 호출)**:
```java
public void pay(Long orderId) {
    try {
        Payment payment = processPayment(); // PG사 연동
    } catch (PaymentException e) {
        // 여러 서비스 직접 호출 (순서 중요!)
        orderService.cancel(orderId);           // 1. 주문 취소
        pointService.refund(userId, amount);    // 2. 포인트 환불
        slackService.notify("결제 실패");       // 3. 알림
    }
}
```

**After (이벤트 기반)**:
```java
// PaymentService.java
public void pay(Long orderId) {
    try {
        Payment payment = processPayment();
        eventPublisher.publishEvent(new PaymentEvent.Success(...));
    } catch (PaymentException e) {
        // 실패 이벤트만 발행
        eventPublisher.publishEvent(
            new PaymentEvent.Failed(paymentId, orderId, e.getMessage())
        );
    }
}

// OrderEventListener.java
@EventListener(PaymentEvent.Failed.class)
public void onPaymentFailed(PaymentEvent.Failed event) {
    log.info("결제 실패 이벤트 수신 - orderId: {}", event.orderId());

    Order order = orderRepository.findById(event.orderId());
    order.cancelByPaymentFailure();

    // 포인트 환불 (독립적으로 처리)
    pointService.refundPointsForPaymentFailure(order.getUserId(), event.orderId());
}
```

**효과**:
- ✅ PaymentService는 실패 처리 로직을 몰라도 됨
- ✅ 각 도메인이 자신의 복구 로직을 독립적으로 관리

---

### 사례 3: Order 배송 완료 → Point 적립

**시나리오**: 주문 배송 완료 7일 후 포인트 적립

**구현**:
```java
// OrderService.java (발행자)
public void markAsDelivered(Long orderId) {
    Order order = orderRepository.findById(orderId);
    order.markAsDelivered();

    // 배송 완료 이벤트 발행
    eventPublisher.publishEvent(
        new OrderEvent.Delivered(orderId, order.getUserId(), order.getTotalPrice())
    );
}

// PointEventListener.java (구독자)
@Component
public class PointEventListener {
    @EventListener(OrderEvent.Delivered.class)
    public void onOrderDelivered(OrderEvent.Delivered event) {
        // TODO: 배송 완료 7일 후 포인트 적립 (스케줄러 필요)
        Long earnPoints = (long)(event.totalPrice() * 0.01); // 1% 적립
        pointService.earnPoints(event.userId(), earnPoints, "주문 적립");
    }
}
```

**효과**:
- ✅ OrderService는 Point 적립 로직을 몰라도 됨
- ✅ Point 적립 정책 변경 시 OrderService 수정 불필요

---

## 설계 결정 과정

### 1단계: 문제 인식

**초기 상황**:
- PaymentService가 너무 많은 책임을 가짐
- 새로운 부가 기능 추가 시마다 PaymentService 수정

**질문**:
> "결제 서비스가 주문 상태 변경, 포인트 환불, 알림까지 알아야 하는가?"

**답변**: ❌ No
- PaymentService는 **결제 처리만** 책임져야 함
- 부가 작업은 각 도메인의 책임

### 2단계: 해결 방안 탐색

**대안 1: Service 계층 분리**
```java
public class PaymentOrchestrator {
    public void pay(Long orderId) {
        paymentService.pay();
        orderService.updateStatus();
        pointService.refund();
    }
}
```
- ❌ 여전히 강한 결합
- ❌ Orchestrator가 모든 서비스를 알아야 함

**대안 2: 이벤트 기반 설계** (채택)
```java
public class PaymentService {
    public void pay(Long orderId) {
        // 결제만 처리
        eventPublisher.publishEvent(new PaymentEvent.Success(...));
    }
}
```
- ✅ 느슨한 결합
- ✅ 확장 용이

### 3단계: 장단점 비교

| 항목 | 직접 호출 | 이벤트 기반 (채택) |
|------|-----------|-------------------|
| 결합도 | ❌ 강함 | ✅ 약함 |
| 확장성 | ❌ 낮음 (코드 수정 필요) | ✅ 높음 (리스너 추가만) |
| 디버깅 | ✅ 쉬움 (직접 추적) | ❌ 어려움 (간접 호출) |
| 테스트 | ❌ 어려움 (많은 Mock) | ✅ 쉬움 (독립적 테스트) |
| 학습 곡선 | ✅ 낮음 | ❌ 높음 |
| 비동기 처리 | ❌ 불가 | ✅ 가능 (향후 확장) |

**결론**: 장기적 확장성을 위해 **이벤트 기반 설계 채택**

### 4단계: 구현 및 검증

**구현**:
1. 도메인 이벤트 정의 (`PaymentEvent`, `OrderEvent`)
2. 이벤트 발행 (`ApplicationEventPublisher`)
3. 이벤트 리스너 구현 (`OrderEventListener`)

**검증**:
- 통합 테스트 작성 (`PaymentEventIntegrationTest`)
- 이벤트 플로우 정상 동작 확인

---

## 트러블슈팅

### 문제 1: 이벤트 리스너가 실행 안됨

**증상**:
```java
eventPublisher.publishEvent(new PaymentEvent.Success(...));
// 이벤트 발행했는데 리스너가 실행 안됨
```

**원인**:
- 리스너 클래스가 `@Component`로 등록 안됨
- 이벤트 타입이 일치하지 않음

**해결**:
```java
@Component  // ← 필수!
public class OrderEventListener {
    @EventListener(PaymentEvent.Success.class)  // 타입 정확히 일치
    public void onPaymentSuccess(PaymentEvent.Success event) {
        // ...
    }
}
```

---

### 문제 2: 트랜잭션 롤백 문제

**증상**:
- 리스너에서 예외 발생 시 전체 트랜잭션 롤백

**원인**:
- 기본적으로 리스너는 같은 트랜잭션에서 실행

**해결**:
```java
// 방법 1: 트랜잭션 커밋 후 실행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onPaymentSuccess(PaymentEvent.Success event) {
    // 트랜잭션 커밋 후 실행 (롤백 영향 없음)
}

// 방법 2: 비동기 처리
@Async
@EventListener(PaymentEvent.Success.class)
public void onPaymentSuccess(PaymentEvent.Success event) {
    // 별도 스레드에서 실행
}
```

---

### 문제 3: 이벤트 순서 보장

**증상**:
- 여러 리스너의 실행 순서가 랜덤

**해결**:
```java
@Order(1)  // 먼저 실행
@EventListener(PaymentEvent.Success.class)
public void updateOrder() { /* ... */ }

@Order(2)  // 나중에 실행
@EventListener(PaymentEvent.Success.class)
public void sendNotification() { /* ... */ }
```

---

## 결론

### 이벤트 기반 설계를 선택한 이유

1. **도메인 간 결합도 감소**
   - PaymentService가 Order, Point 서비스를 몰라도 됨

2. **확장성 향상**
   - 새로운 부가 기능 추가 시 기존 코드 수정 불필요

3. **단일 책임 원칙 준수**
   - 각 서비스가 자신의 책임만 담당

4. **비동기 처리 가능** (향후 확장)
   - 알림, 이메일 등을 비동기로 처리 가능

### 트레이드오프

**포기한 것**:
- 디버깅 용이성
- 학습 곡선

**얻은 것**:
- 확장 가능한 아키텍처
- 도메인 독립성
- 테스트 용이성

### 적용 기준

**이벤트 사용 ✅**:
- 도메인 간 의존성을 끊어야 할 때
- 부가 작업이 많을 때 (알림, 로깅 등)
- 비동기 처리가 필요할 때

**이벤트 사용 ❌**:
- 같은 도메인 내부 로직
- 단순한 CRUD
- 순서가 매우 중요한 로직

---

**문서 작성일**: 2026-01-08
**버전**: 1.0
