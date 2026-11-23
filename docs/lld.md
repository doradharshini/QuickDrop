# Low-Level Design (LLD)

Describes the code as it exists today, in package `com.quickdrop`.

## 1. Package layout

```
com.quickdrop
├── QuickdropApplication                  Spring Boot entry point
└── order
    ├── OrderStatus                       enum + state machine rules
    ├── FoodOrder                         JPA entity
    ├── FoodOrderRepository               Spring Data JPA repository
    ├── OrderService                      business logic, transactions
    ├── OrderController                   REST endpoints
    ├── CreateOrderRequest                request record (validated)
    ├── StatusChangeRequest               request record (validated)
    ├── OrderNotFoundException            maps to 404
    ├── IllegalOrderTransitionException   maps to 409
    └── ApiExceptionHandler               @RestControllerAdvice
```

## 2. Class diagram

```mermaid
classDiagram
    class OrderController {
        -OrderService service
        +create(CreateOrderRequest) FoodOrder
        +list(OrderStatus status) List~FoodOrder~
        +get(Long id) FoodOrder
        +changeStatus(Long id, StatusChangeRequest) FoodOrder
    }

    class OrderService {
        -FoodOrderRepository repository
        +create(CreateOrderRequest) FoodOrder
        +get(Long id) FoodOrder
        +list(OrderStatus status) List~FoodOrder~
        +transition(Long id, OrderStatus target) FoodOrder
    }

    class FoodOrderRepository {
        <<interface>>
        +findByStatus(OrderStatus status) List~FoodOrder~
    }

    class FoodOrder {
        <<Entity>>
        -Long id
        -String customerName
        -String restaurantName
        -String deliveryAddress
        -BigDecimal totalAmount
        -OrderStatus status
        -Instant createdAt
        -Instant updatedAt
        ~setStatus(OrderStatus status)
    }

    class OrderStatus {
        <<enumeration>>
        PLACED
        ACCEPTED
        PREPARING
        READY
        PICKED_UP
        DELIVERED
        CANCELLED
        +allowedNext() Set~OrderStatus~
        +canTransitionTo(OrderStatus target) boolean
        +isTerminal() boolean
    }

    class CreateOrderRequest {
        <<record>>
        +String customerName
        +String restaurantName
        +String deliveryAddress
        +BigDecimal totalAmount
    }

    class StatusChangeRequest {
        <<record>>
        +OrderStatus status
    }

    class OrderNotFoundException {
        <<RuntimeException>>
    }

    class IllegalOrderTransitionException {
        <<RuntimeException>>
    }

    class ApiExceptionHandler {
        <<RestControllerAdvice>>
        +notFound(OrderNotFoundException) ResponseEntity
        +conflict(IllegalOrderTransitionException) ResponseEntity
    }

    OrderController --> OrderService
    OrderController ..> CreateOrderRequest
    OrderController ..> StatusChangeRequest
    OrderService --> FoodOrderRepository
    OrderService ..> OrderStatus : checks canTransitionTo
    OrderService ..> OrderNotFoundException : throws
    OrderService ..> IllegalOrderTransitionException : throws
    FoodOrderRepository ..> FoodOrder : manages
    FoodOrder --> OrderStatus
    StatusChangeRequest --> OrderStatus
    ApiExceptionHandler ..> OrderNotFoundException : handles
    ApiExceptionHandler ..> IllegalOrderTransitionException : handles
```

## 3. Sequence diagrams

### 3.1 Place an order

```mermaid
sequenceDiagram
    actor Client
    participant C as OrderController
    participant S as OrderService
    participant R as FoodOrderRepository
    participant DB as H2

    Client->>C: POST /api/orders (JSON)
    C->>C: validate CreateOrderRequest
    alt invalid (blank field or amount below 0.01)
        C-->>Client: 400 Bad Request
    else valid
        C->>S: create(request)
        S->>S: build FoodOrder (status = PLACED)
        S->>R: save(order)
        R->>DB: INSERT food_order
        Note right of DB: createdAt and updatedAt set by @PrePersist
        DB-->>R: generated id
        R-->>S: saved order
        S-->>C: order
        C-->>Client: 201 Created + JSON
    end
```

### 3.2 Change status (success)

```mermaid
sequenceDiagram
    actor Client
    participant C as OrderController
    participant S as OrderService
    participant R as FoodOrderRepository
    participant O as OrderStatus

    Client->>C: POST /api/orders/7/status {"status":"ACCEPTED"}
    C->>S: transition(7, ACCEPTED)
    S->>R: findById(7)
    R-->>S: order (PLACED)
    S->>O: PLACED.canTransitionTo(ACCEPTED)
    O-->>S: true
    S->>S: order.setStatus(ACCEPTED)
    S->>R: save(order)
    Note right of R: updatedAt set by @PreUpdate
    R-->>S: saved order
    S-->>C: order
    C-->>Client: 200 OK + JSON
```

### 3.3 Change status (rejected)

```mermaid
sequenceDiagram
    actor Client
    participant C as OrderController
    participant S as OrderService
    participant R as FoodOrderRepository
    participant O as OrderStatus
    participant H as ApiExceptionHandler

    Client->>C: POST /api/orders/7/status {"status":"DELIVERED"}
    C->>S: transition(7, DELIVERED)
    S->>R: findById(7)
    R-->>S: order (PLACED)
    S->>O: PLACED.canTransitionTo(DELIVERED)
    O-->>S: false
    S-->>H: throw IllegalOrderTransitionException
    H-->>Client: 409 Conflict (lists allowed statuses)
    Note over S,R: nothing is saved, status stays PLACED
```

An unknown id behaves the same way, except `findById` returns empty and the service throws `OrderNotFoundException`, which the handler turns into `404 Not Found`.

## 4. State machine table

| From | Allowed next |
| --- | --- |
| `PLACED` | `ACCEPTED`, `CANCELLED` |
| `ACCEPTED` | `PREPARING`, `CANCELLED` |
| `PREPARING` | `READY`, `CANCELLED` |
| `READY` | `PICKED_UP` |
| `PICKED_UP` | `DELIVERED` |
| `DELIVERED` | none (terminal) |
| `CANCELLED` | none (terminal) |

## 5. Data model

```mermaid
erDiagram
    FOOD_ORDER {
        bigint id PK "auto-generated identity"
        varchar customer_name "NOT NULL"
        varchar restaurant_name "NOT NULL"
        varchar delivery_address "NOT NULL"
        decimal total_amount "NOT NULL, precision 10 scale 2"
        varchar status "NOT NULL, enum stored as text"
        timestamp created_at "NOT NULL, not updatable"
        timestamp updated_at "NOT NULL"
    }
```

The schema is generated by Hibernate (`ddl-auto=update`). Flyway migrations are planned with the move to PostgreSQL.

## 6. Validation rules

| Request | Field | Rule |
| --- | --- | --- |
| Create order | `customerName`, `restaurantName`, `deliveryAddress` | required, not blank |
| Create order | `totalAmount` | required, at least 0.01 |
| Change status | `status` | required, must be a valid `OrderStatus` |

## 7. Error handling

| Situation | HTTP status | Source |
| --- | --- | --- |
| Validation failure, bad JSON, unknown status value | 400 | Spring default handling |
| Order id not found | 404 | `OrderNotFoundException` |
| Transition not allowed from current status | 409 | `IllegalOrderTransitionException` |

Custom error bodies (404 and 409) use the shape `{status, error, message}`.

## 8. Planned additions to the design (steps 2 to 5)

Not built yet.

```mermaid
classDiagram
    class Driver {
        Long id
        String name
        DriverStatus status
    }
    class DriverLocationService {
        +updateLocation(driverId, lat, lon)
        +findNearestFree(lat, lon, radius, limit) List~Driver~
    }
    class MatchingService {
        +offerOrder(orderId)
        +onDriverAccept(orderId, driverId)
        +onOfferTimeout(orderId)
    }
    class DriverStatus {
        <<enumeration>>
        OFFLINE
        FREE
        OFFERED
        BUSY
    }
    class OrderEventPublisher {
        +publish(OrderEvent)
    }
    MatchingService --> DriverLocationService
    MatchingService --> OrderService : existing
    DriverLocationService ..> Driver
    Driver --> DriverStatus
    OrderService ..> OrderEventPublisher
```

## 9. Known limitations

- No authentication, so anyone can move any order to any allowed status. Roles (customer, restaurant, driver) should restrict who may trigger which transition.
- Orders have no items, only a total amount.
- No pagination on `GET /api/orders`.
- Concurrent status changes to the same order are not guarded. A version field (optimistic locking) is a sensible fix.
