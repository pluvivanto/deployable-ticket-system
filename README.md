# Deployable Ticket System

A ticket reservation API built with Spring Boot.
Uses Redis to handle high traffic and Postgres for persistency.

## Request flow

1. **Request:** A user asks for a ticket.
2. **Redis:** The app checks stock in Redis. If there is a ticket, it takes one and responds immediately.
3. **Database:** Scheduled async job takes the request from a Redis queue and saves it to Postgres.

## Architecture Diagram

```mermaid
graph TD
    User((User)) -->|HTTP| ALB[Load Balancer]
    ALB --> APP[Spring Boot App]

    subgraph Initialization [One-time Tasks]
        Startup[Startup Runner]
    end

    Startup -.->|Read| DB
    Startup -.->|Set| RedisStock

    subgraph Background [Background Processing]
        Worker[Async Worker]
    end

    subgraph DataLayer [Data Layer]
        direction LR
        RedisStock[(Redis: Stock)]
        RedisQueue[(Redis: Queue)]
        DB[(PostgreSQL)]
    end

    subgraph Monitoring [Monitoring Stack]
        Prom[Prometheus] --> Graf[Grafana]
    end

    APP -->|Decr| RedisStock
    APP -->|Enqueue| RedisQueue
    RedisQueue -->|Poll| Worker
    Worker -->|Save| DB

    APP -.->|Scrape| Prom
```

## Reservation Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant A as API
    participant R as Redis (Stock)
    participant Q as Redis (Queue)
    participant W as Worker
    participant DB as Postgres

    U->>A: POST /reserve
    A->>R: Atomic Decrement
    alt Stock > 0
        R-->>A: Success
        A->>Q: Push Reservation ID
        A-->>U: 202 Accepted
    else Stock <= 0
        R-->>A: Fail
        A-->>U: 409 Conflict
    end

    par Async Processing
        W->>Q: Poll
        Q-->>W: Reservation ID
        W->>DB: Save Reservation
    end
```

## Running it locally

Need Docker installed.

```bash
docker compose up -d --build
```

**Monitoring (Local Only):**
The local version includes a monitoring stack to see performance in real-time:

* **Grafana:** `localhost:13000`
* **Prometheus:** `localhost:19090`

## Load Testing

I used k6 for stress testing. Locally, it processes over 10,000 requests per second with one instance.

```bash
k6 run -e TICKET_ID=<TICKET_ID> load-test.js
```

## AWS Deployment (Terraform)

The `terraform/` folder has the code to deploy this to AWS. Note that the AWS version is minimal and does not include the Grafana/Prometheus stack to save on costs.

### AWS Infrastructure Diagram

![AWS Architecture](docs/aws_architecture.png)

*Diagram generated using [Diagram-as-code](https://github.com/awslabs/diagram-as-code).*
