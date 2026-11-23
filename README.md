# QuickDrop

A food-delivery backend built in stages: orders with an enforced lifecycle first, then live driver tracking, nearest-driver matching and real-time updates.

## Status

| Step | Feature | State |
| --- | --- | --- |
| 1 | Orders with an enforced state machine | Done |
| 2 | Driver locations in Redis GEO, nearest-driver query | Planned |
| 3 | Matching: offer a ready order to the closest free driver, with timeout and re-offer | Planned |
| 4 | WebSocket (STOMP) feed for order status and driver location | Planned |
| 5 | Kafka order events, Docker Compose with Postgres, Redis and Kafka | Planned |

## Stack

Java 21, Spring Boot 3.5, Spring Data JPA, H2 (file mode) for now. Planned: PostgreSQL, Redis GEO, Kafka, WebSockets.

## Order lifecycle

```
PLACED -> ACCEPTED -> PREPARING -> READY -> PICKED_UP -> DELIVERED
   \          \           \
    +----------+-----------+--> CANCELLED     (allowed until the food is READY)
```

`DELIVERED` and `CANCELLED` are terminal. Any other move returns `409 Conflict` with the list of allowed next statuses. The rules live in one place, [`OrderStatus`](src/main/java/com/quickdrop/order/OrderStatus.java).

## Run

```bash
mvn spring-boot:run
```

Requires JDK 21 or newer on `JAVA_HOME`.

## API

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/orders` | Place an order (starts as `PLACED`) |
| GET | `/api/orders` | List orders, optional `?status=READY` |
| GET | `/api/orders/{id}` | Get one order |
| POST | `/api/orders/{id}/status` | Move to a new status, body `{"status":"ACCEPTED"}` |

```bash
curl -X POST localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"customerName":"Asha","restaurantName":"Spice Hub","deliveryAddress":"12 MG Road","totalAmount":349.50}'

curl -X POST localhost:8080/api/orders/1/status -H "Content-Type: application/json" \
  -d '{"status":"ACCEPTED"}'
```

## Test

```bash
mvn test
```

The tests cover every possible status pair, and the full order flow through the web layer: the happy path, skipped steps, terminal states, validation, and 404s.
