# User Flow

How an order moves through QuickDrop, and who acts at each step. The order API (step 1) is built. The driver and live-tracking parts are planned and marked as such.

## 1. End-to-end order flow

```mermaid
flowchart TD
    A([Customer opens app]) --> B[Choose restaurant and items]
    B --> C["POST /api/orders<br/>customer, restaurant, address, total"]
    C --> D{Valid request?}
    D -- No --> E["400 Bad Request"]
    E --> C
    D -- Yes --> F["201 Created<br/>status = PLACED"]
    F --> G[Restaurant reviews the order]
    G --> H{Restaurant decision}
    H -- Reject --> X([CANCELLED])
    H -- Accept --> I["POST /status ACCEPTED"]
    I --> J["POST /status PREPARING<br/>kitchen starts cooking"]
    J --> K["POST /status READY<br/>food is packed"]
    K --> L["Driver arrives, POST /status PICKED_UP"]
    L --> M["Driver hands over,<br/>POST /status DELIVERED"]
    M --> Z([DELIVERED])
    I -. customer cancels .-> X
    J -. customer cancels .-> X
    G -. customer cancels .-> X
```

Cancelling is allowed only until the food is `READY`. After that the food has been made, so the API refuses it.

## 2. Order status lifecycle

```mermaid
stateDiagram-v2
    [*] --> PLACED: order created
    PLACED --> ACCEPTED
    PLACED --> CANCELLED
    ACCEPTED --> PREPARING
    ACCEPTED --> CANCELLED
    PREPARING --> READY
    PREPARING --> CANCELLED
    READY --> PICKED_UP
    PICKED_UP --> DELIVERED
    DELIVERED --> [*]
    CANCELLED --> [*]
```

Any move not shown above, such as `PLACED` to `DELIVERED`, returns `409 Conflict`. The error message lists the statuses that are allowed from the current one.

## 3. What happens on an illegal move

```mermaid
flowchart LR
    A["POST /api/orders/7/status<br/>status = DELIVERED"] --> B{"Order 7 exists?"}
    B -- No --> C["404 Not Found"]
    B -- Yes --> D{"Current status allows DELIVERED?"}
    D -- Yes --> E["200 OK<br/>order updated"]
    D -- No --> F["409 Conflict<br/>lists allowed next statuses"]
```

## 4. Planned: driver and live-tracking flow (steps 2 to 4)

Not built yet.

```mermaid
flowchart TD
    A([Order becomes READY]) --> B["Find nearest free drivers<br/>(Redis GEO)"]
    B --> C[Offer the order to the closest driver]
    C --> D{Driver accepts<br/>before timeout?}
    D -- Yes --> E[Driver assigned]
    D -- No --> F[Offer to next closest driver]
    F --> D
    E --> G[Driver heads to restaurant]
    G --> H[Driver sends location updates]
    H --> I[Customer sees the driver<br/>move live over WebSocket]
    I --> J([Delivered])
```
