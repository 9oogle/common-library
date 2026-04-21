# common-library

MSA 환경에서 공통으로 사용하는 기능을 제공하는 라이브러리입니다.

- 공통 예외 처리 / 응답 포맷
- Outbox / Inbox 패턴 (Kafka 기반 이벤트 발행 신뢰성 보장)
- 페이지네이션 (Offset / Cursor)
- MDC 기반 요청 추적

---

## 목차

1. [의존성 추가](#1-의존성-추가)
2. [소비자 서비스 설정](#2-소비자-서비스-설정)
3. [예외 처리](#3-예외-처리)
4. [공통 응답 포맷](#4-공통-응답-포맷)
5. [Outbox / Inbox 패턴](#5-outbox--inbox-패턴)
6. [페이지네이션](#6-페이지네이션)
7. [MDC 요청 추적](#7-mdc-요청-추적)
8. [버전 히스토리](#8-버전-히스토리)

---

## 1. 의존성 추가

### GitHub Personal Access Token 발급

GitHub Packages에서 패키지를 내려받으려면 인증 토큰이 필요합니다.

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)** 클릭
3. 권한에서 **`read:packages`** 체크
4. 토큰 생성 후 복사 (`ghp_xxxx...`)

### Gradle 인증 설정

발급받은 토큰을 로컬 Gradle 설정 파일에 저장합니다.

> ⚠️ 프로젝트 내부 파일에 절대 넣지 마세요. git에 올라갑니다.

`~/.gradle/gradle.properties`

```properties
GitHubPackagesUsername=본인_깃헙_아이디
GitHubPackagesPassword=ghp_xxxxxxxxxxxxxxxxxxxx
```

### build.gradle.kts 설정

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/9oogle/common-library")
        credentials {
            username = providers.gradleProperty("GitHubPackagesUsername").get()
            password = providers.gradleProperty("GitHubPackagesPassword").get()
        }
    }
}

dependencies {
    implementation("com.goggles:common-library:1.0.0")
}
```

---

## 2. 소비자 서비스 설정

### EventConfig Import

라이브러리의 모든 빈(Outbox/Inbox, Filter, ArgumentResolver 등)은 `EventConfig` 하나를 import하면 자동 등록됩니다.

```java
@Import(EventConfig.class)
@SpringBootApplication
@EnableJpaAuditing
public class MyServiceApplication { }
```

### JPA Auditing 설정

`BaseTime`(`createdAt`, `updatedAt`)은 `@EnableJpaAuditing`만으로 동작합니다.

`BaseAudit`(`createdBy`, `updatedBy`)을 사용하는 엔티티가 있다면 `AuditorAware<UUID>` 빈을 추가로 등록해야 합니다.

```java
@Bean
public AuditorAware<UUID> auditorAware() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()));
}
```

### 기반 엔티티 선택

| 클래스 | 제공 필드 | 사용 시점 |
|--------|-----------|-----------|
| `BaseTime` | `createdAt`, `updatedAt`, `deletedAt` | 작성자 정보가 필요 없을 때 |
| `BaseAudit` | `BaseTime` + `createdBy`, `updatedBy`, `deletedBy` | 작성자 정보까지 필요할 때 |

```java
// 작성자 추적이 필요한 엔티티
@Entity
public class Order extends BaseAudit { ... }

// 시각 정보만 필요한 엔티티
@Entity
public class Outbox extends BaseTime { ... }
```

---

## 3. 예외 처리

### 예외 계층 구조

```
RuntimeException
└── CustomException (status, field 포함)
    ├── BadRequestException        400
    ├── UnAuthorizedException      401
    ├── ForbiddenException         403
    ├── NotFoundException          404
    ├── ConflictException          409
    └── InternalServerException    500
```

### 도메인별 커스텀 예외 생성 (권장)

각 도메인 서비스에서 위 클래스를 상속받아 도메인에 맞는 예외를 직접 만들어 사용하는 것을 권장합니다. 에러 메시지를 한 곳에서 관리할 수 있습니다.

```java
// 주문 도메인 예외 예시
public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(String orderId) {
        super("주문을 찾을 수 없습니다. orderId=" + orderId);
    }
}

public class OrderAlreadyPaidException extends ConflictException {
    public OrderAlreadyPaidException(String orderId) {
        super("이미 결제된 주문입니다. orderId=" + orderId);
    }
}
```

```java
// 사용
Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
```

### GlobalExceptionAdvice 처리 목록

`GlobalExceptionAdvice`는 `@RestControllerAdvice`로 등록되며, 아래 예외들을 자동으로 처리합니다.

| 예외 | HTTP 상태 |
|------|-----------|
| `CustomException` (및 하위 클래스) | 예외에 설정된 상태 코드 |
| `MethodArgumentNotValidException` (`@Valid` 실패) | 400 |
| `ConstraintViolationException` (`@Validated` 실패) | 400 |
| `HttpMessageNotReadableException` (Body 누락/형식 오류) | 400 |
| `IllegalArgumentException` | 400 |
| `OptimisticLockException` | 409 |
| `Exception` (그 외 모든 예외) | 500 |

### 에러 응답 포맷

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "주문을 찾을 수 없습니다. orderId=abc123",
  "field": null,
  "traceId": "a3f2bc91",
  "timestamp": "2025-04-21T10:30:00"
}
```

---

## 4. 공통 응답 포맷

`CommonResponseAdvice`가 컨트롤러의 반환값을 자동으로 `ApiResponse`로 감쌉니다. 컨트롤러에서 별도 처리가 필요 없습니다.

```java
// 컨트롤러
@GetMapping("/orders/{id}")
public OrderResponse getOrder(@PathVariable String id) {
    return orderService.getOrder(id); // ApiResponse로 자동 래핑됨
}
```

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... },
  "traceId": "a3f2bc91"
}
```

메시지를 직접 지정하고 싶다면 `ApiResponse`를 직접 반환합니다.

```java
return ApiResponse.success("주문이 생성되었습니다.", orderResponse);
```

---

## 5. Outbox / Inbox 패턴

### 개념

Kafka 메시지 발행은 네트워크 오류, 브로커 장애 등으로 실패할 수 있습니다. 단순히 `kafkaTemplate.send()`를 호출하면 DB 저장은 성공했는데 메시지 발행이 누락되는 상황이 발생할 수 있습니다.

이 문제를 해결하기 위해 **Outbox/Inbox 패턴**을 사용합니다.

- **Outbox**: 발행할 메시지를 DB에 먼저 저장한 뒤 Kafka로 전송. 전송 실패 시 스케줄러가 재시도
- **Inbox**: 컨슈머 측에서 동일 메시지를 중복으로 처리하지 않도록 멱등성 보장

---

### Outbox 처리 흐름

```
도메인 서비스                   OutboxEventListener              OutboxRelayScheduler
─────────────────────────────────────────────────────────────────────────────────────
Events.trigger()
  │
  └─► Spring ApplicationEvent 발행
            │
            ▼
      recordOutbox()          ← @EventListener
      DB에 PENDING으로 저장
      (트랜잭션 참여)
            │
            ▼ (트랜잭션 커밋 후)
      publish()               ← @TransactionalEventListener(AFTER_COMMIT)
      Kafka 전송
            │
     ┌──────┴──────┐
   성공            실패
     │              │
  PROCESSED      FAILED + retryCount++
                    │
              retryCount >= 3
                    │
                  DLT 전송
                (topic.DLT)

                          스케줄러 (10초마다)
                          ─────────────────
                          PENDING/FAILED 조회
                          → PROCESSING으로 선점
                          → Kafka 재전송
                          → PROCESSED/FAILED 업데이트
                          (5분 이상 PROCESSING stuck → 자동 복구)
```

---

### Kafka 파티션 키

모든 Outbox 메시지는 `correlationId`를 Kafka 메시지 **키**로 사용합니다.

Kafka는 같은 파티션 내에서만 순서를 보장합니다. 키가 없으면 메시지가 라운드로빈으로 파티션에 분산되어, 동일 엔티티에 대한 이벤트(`order-created` → `order-paid` → `order-cancelled`)가 서로 다른 파티션으로 흩어져 **순서가 역전**될 수 있습니다.

`correlationId`를 키로 사용하면 같은 엔티티의 이벤트가 항상 동일한 파티션으로 전송되어 순서가 보장됩니다. 최초 발행(`publish`)과 스케줄러 재전송(`relay`) 두 경로 모두 동일하게 적용되어 있습니다.

```
correlationId = "order-abc123"  →  hash("order-abc123") % 파티션 수  →  항상 파티션 2
```

> `correlationId`는 보통 도메인 엔티티 ID를 사용합니다. 같은 주문에 대한 모든 이벤트가 동일한 파티션으로 묶입니다.

---

### Outbox 사용법

**1. `Events.trigger()` 호출**

도메인 서비스에서 `Events`를 주입받아 이벤트를 발행합니다. 반드시 `@Transactional` 메서드 내에서 호출해야 합니다.

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final Events events;

    @Transactional
    public void createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(Order.from(request));

        // correlationId: 중복 방지용 고유 ID (보통 도메인 ID 활용)
        // domainType: 도메인 구분 문자열
        // eventType: Kafka 토픽명
        // payload: 전송할 데이터 객체
        events.trigger(
                order.getId().toString(),   // correlationId
                "ORDER",                    // domainType
                "order-created",            // eventType (= Kafka 토픽)
                new OrderCreatedPayload(order)
        );
    }
}
```

**2. DB 마이그레이션**

소비자 서비스의 DB에 `p_outbox` 테이블이 필요합니다.

```sql
CREATE TABLE p_outbox (
    id              UUID PRIMARY KEY,
    correlation_id  VARCHAR(64)  NOT NULL UNIQUE,
    domain_type     VARCHAR(50)  NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_outbox_status ON p_outbox (status);
```

---

### Inbox 처리 흐름

```
Kafka Consumer
──────────────
메시지 수신
  │
  ▼
@IdempotentConsumer     ← AOP가 가로챔
  │
  ├─► p_inbox에 (messageId, messageGroup) INSERT 시도
  │         │
  │    이미 존재   → 중복 메시지 → 처리 건너뜀
  │         │
  │    신규 메시지 → 비즈니스 로직 실행
  │                      │
  │                   예외 발생 → 트랜잭션 롤백 (Inbox 저장도 취소)
  │                              → 다음 소비 시 재처리됨
  │
  └─► InboxCleanupScheduler: 7일 경과 Inbox 레코드 매일 새벽 3시 삭제
```

---

### Inbox 사용법

**1. `@IdempotentConsumer` 적용**

Kafka `@KafkaListener` 메서드에 어노테이션을 붙입니다. `value`에는 토픽명 또는 컨슈머 그룹명을 입력합니다.

```java
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    @IdempotentConsumer("order-created")   // value = 메시지 그룹 구분자
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        // message_id 헤더가 있으면 자동으로 중복 체크
        // 이미 처리된 메시지면 이 메서드는 실행되지 않음
        paymentService.processPayment(record.value());
    }
}
```

> `message_id` 헤더는 Outbox 패턴으로 발행된 메시지에 자동으로 포함됩니다.
> 외부에서 수신하는 메시지도 헤더에 `message_id`를 포함하면 동일하게 멱등성이 보장됩니다.

**2. DB 마이그레이션**

소비자 서비스의 DB에 `p_inbox` 테이블이 필요합니다.

```sql
CREATE TABLE p_inbox (
    id              UUID PRIMARY KEY,
    message_id      UUID         NOT NULL,
    message_group   VARCHAR(100),
    processed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_inbox_message_id_group UNIQUE (message_id, message_group)
);

CREATE INDEX idx_inbox_message_group ON p_inbox (message_group);
CREATE INDEX idx_inbox_processed_at  ON p_inbox (processed_at);
```

---

## 6. 페이지네이션

두 가지 방식을 지원합니다. `EventConfig`를 import하면 `ArgumentResolver`가 자동 등록되어 컨트롤러 파라미터로 바로 사용할 수 있습니다.

### Offset 기반 페이지네이션

전체 데이터 수와 페이지 번호가 필요한 일반적인 목록 조회에 사용합니다.

**허용 사이즈:** `10`, `30`, `50` (그 외 값은 기본값 `10`으로 대체)

```java
// Controller
@GetMapping("/orders")
public ApiResponse<CommonPageResponse<OrderResponse>> getOrders(CommonPageRequest pageRequest) {
    Page<Order> page = orderRepository.findAll(pageRequest.toPageable());
    return ApiResponse.success(CommonPageResponse.of(page, OrderResponse::from));
}
```

```
GET /orders?page=0&size=10
GET /orders?page=1&size=30&sort=createdAt,desc   // 정렬 포함
```

**응답 포맷**

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 253,
  "totalPages": 26,
  "first": true,
  "last": false
}
```

**정렬을 포함한 Pageable 생성**

```java
Pageable pageable = pageRequest.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
```

---

### Cursor 기반 페이지네이션

무한 스크롤, 피드처럼 전체 데이터 수가 필요 없고 성능이 중요한 경우에 사용합니다. `cursor`가 없으면 첫 페이지입니다.

```java
// Controller
@GetMapping("/feeds")
public ApiResponse<CommonCursorResponse<FeedResponse>> getFeeds(CommonCursorRequest cursorRequest) {
    // size + 1개를 조회해서 다음 페이지 존재 여부를 판단
    List<Feed> feeds = feedRepository.findByCursor(
            cursorRequest.getCursor(),
            cursorRequest.getSize() + 1
    );
    return ApiResponse.success(
            CommonCursorResponse.of(feeds, cursorRequest.getSize(), FeedResponse::from, Feed::getCursorKey)
    );
}
```

```
GET /feeds?size=10              // 첫 페이지
GET /feeds?cursor=xxx&size=10   // 다음 페이지
```

**응답 포맷**

```json
{
  "content": [...],
  "nextCursor": "eyJpZCI6MTIzfQ",
  "hasNext": true
}
```

`nextCursor`가 `null`이면 마지막 페이지입니다.

---

### 페이지네이션 방식 선택 기준

| | Offset | Cursor |
|---|---|---|
| 전체 페이지 수 표시 | 가능 | 불가 |
| 특정 페이지 이동 | 가능 | 불가 |
| 대용량 데이터 성능 | 느려짐 (OFFSET 비용) | 일정 (인덱스 활용) |
| 적합한 UX | 페이지네이션 버튼 | 무한 스크롤 |

---

## 7. MDC 요청 추적

`MdcLoggingFilter`가 자동 등록됩니다. 모든 요청에 `traceId`, `uri`, `method`가 MDC에 주입되어 로그에서 요청 단위로 추적할 수 있습니다.

**logback 설정 예시**

```xml
<pattern>[%X{traceId}] [%X{method} %X{uri}] %d{HH:mm:ss} %-5level %logger - %msg%n</pattern>
```

**traceId 전달**

외부에서 `X-Trace-Id` 헤더를 넘기면 해당 값을 traceId로 사용합니다. 없으면 자동 생성됩니다.

```
X-Trace-Id: a3f2bc91-7e4d
```

**비동기 처리**

`@Async` 메서드에서도 traceId가 유지됩니다. `EventConfig`에 등록된 스레드 풀이 `MdcTaskDecorator`를 통해 부모 스레드의 MDC 컨텍스트를 자식 스레드에 복사합니다.

---

## 8. 버전 히스토리

| 버전 | 변경 내용 |
|------|-----------|
| 1.0.0 | 최초 릴리즈: 예외 처리, 공통 응답, Outbox/Inbox, 페이지네이션, MDC |