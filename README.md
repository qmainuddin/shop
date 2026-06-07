# Shop — Microservice E-Commerce Platform

A scale-ready online store built with a microservices architecture. Users can browse products, place orders, and manage accounts. All traffic flows through a single API gateway that enforces JWT authentication.

## Architecture

```
Browser / API Client
        │
        ▼
  [ gateway :8080 ]  ← JWT filter, single public entry (/api)
        │
   ┌────┼────┬────────┐
   ▼    ▼    ▼        ▼
user  product  order  (analytics — planned)
-svc   -svc    -svc
:8081  :8082   :8083
  │      │       │
  └──────┴───────┘
         │
  ┌──────┼──────────┐
  ▼      ▼          ▼
Postgres Redis   RabbitMQ
```

| Service | Language | Description |
|---|---|---|
| `gateway` | Java / Spring Cloud Gateway | Single public entry point. Routes requests, validates JWT tokens. |
| `user-svc` | Java / Spring Boot | User registration and login. Issues JWT tokens. |
| `product-svc` | Java / Spring Boot | Product catalog. Hot reads cached in Redis. |
| `order-svc` | Java / Spring Boot | Order placement. Publishes `order.placed` events to RabbitMQ. |

Shared infrastructure (PostgreSQL, Redis, RabbitMQ) runs on the external Docker network `stacknet`.

---

## Prerequisites

- **Java 21** and **Maven 3.9+** (for local development)
- **Docker** and **Docker Compose** (for containerised runs)
- An external Docker network called `stacknet` with PostgreSQL, Redis, and RabbitMQ already running

### Create the stacknet network (once)

```bash
docker network create stacknet
```

### Start shared services (if not already running)

```bash
docker run -d --name postgres --network stacknet \
  -e POSTGRES_USER=shop -e POSTGRES_PASSWORD=shop -e POSTGRES_DB=shop \
  postgres:16-alpine

docker run -d --name redis --network stacknet redis:7-alpine

docker run -d --name rabbitmq --network stacknet \
  -e RABBITMQ_DEFAULT_USER=shop -e RABBITMQ_DEFAULT_PASS=shop \
  rabbitmq:3-management-alpine
```

---

## Running locally (without Docker)

Each service can be started independently with Maven. Set the required environment variables first.

### user-svc

```bash
cd user-svc
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shop
export SPRING_DATASOURCE_USERNAME=shop
export SPRING_DATASOURCE_PASSWORD=shop
export JWT_SECRET=your-256-bit-secret-key-here-change-in-production
mvn spring-boot:run
# Starts on http://localhost:8081
```

### product-svc

```bash
cd product-svc
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shop
export SPRING_DATASOURCE_USERNAME=shop
export SPRING_DATASOURCE_PASSWORD=shop
export SPRING_DATA_REDIS_HOST=localhost
mvn spring-boot:run
# Starts on http://localhost:8082
```

### order-svc

```bash
cd order-svc
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shop
export SPRING_DATASOURCE_USERNAME=shop
export SPRING_DATASOURCE_PASSWORD=shop
export SPRING_RABBITMQ_HOST=localhost
export SPRING_RABBITMQ_USERNAME=shop
export SPRING_RABBITMQ_PASSWORD=shop
mvn spring-boot:run
# Starts on http://localhost:8083
```

### gateway

```bash
cd gateway
export JWT_SECRET=your-256-bit-secret-key-here-change-in-production
mvn spring-boot:run
# Starts on http://localhost:8080
```

---

## Running tests

Each service has its own unit/integration test suite. Run them individually:

```bash
cd user-svc    && mvn test
cd product-svc && mvn test
cd order-svc   && mvn test
cd gateway     && mvn test
```

Or run all services in Docker (uses H2/mocks — no real infra needed):

```bash
docker compose -f docker-compose.test.yml up --abort-on-container-exit
```

---

## Building Docker images

Each service has a multi-stage `Dockerfile`. Build them individually:

```bash
docker build -t shop/gateway:latest     ./gateway
docker build -t shop/user-svc:latest    ./user-svc
docker build -t shop/product-svc:latest ./product-svc
docker build -t shop/order-svc:latest   ./order-svc
```

---

## Deploying with Docker Compose

> A production `docker-compose.yml` is the recommended deployment method. The CI pipeline builds and starts it automatically when one is present.

A minimal `docker-compose.yml` (adapt passwords and secrets for production):

```yaml
version: "3.9"
networks:
  stacknet:
    external: true

services:
  gateway:
    image: shop/gateway:latest
    ports:
      - "8080:8080"
    environment:
      JWT_SECRET: ${JWT_SECRET}
    networks: [stacknet]

  user-svc:
    image: shop/user-svc:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/shop
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    networks: [stacknet]

  product-svc:
    image: shop/product-svc:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/shop
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
    networks: [stacknet]

  order-svc:
    image: shop/order-svc:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/shop
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBIT_USER}
      SPRING_RABBITMQ_PASSWORD: ${RABBIT_PASSWORD}
    networks: [stacknet]
```

Deploy:

```bash
JWT_SECRET=... DB_USER=shop DB_PASSWORD=... \
  RABBIT_USER=shop RABBIT_PASSWORD=... \
  docker compose up -d
```

---

## CI/CD

GitHub Actions runs on every push to `main` (`.github/workflows/ci.yml`). The pipeline:

1. Checks out the code on the self-hosted VPS runner.
2. Builds and tests all services via Docker Compose.
3. Deploys by running `docker compose up -d` (when `docker-compose.yml` is present).

---

## API overview

All routes go through the gateway at `/api`. Public endpoints do **not** require a JWT; protected endpoints require `Authorization: Bearer <token>`.

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/users/register` | Public | Register a new account |
| `POST` | `/api/users/login` | Public | Login — returns JWT |
| `GET` | `/api/products` | Public | List all products |
| `GET` | `/api/products/{id}` | Public | Get a single product |
| `POST` | `/api/orders` | JWT | Place an order |
| `GET` | `/api/orders/{id}` | JWT | Get order details |
| `GET` | `/api/*/health` | Public | Health check for each service |

---

## Health checks

```bash
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8081/actuator/health   # user-svc
curl http://localhost:8082/actuator/health   # product-svc
curl http://localhost:8083/actuator/health   # order-svc
```

---

## Roadmap

- [ ] `web/` — React + Vite storefront
- [ ] `analytics/` — Python FastAPI consuming RabbitMQ events
- [ ] `scraper/` — competitor price scraper (scheduled)
- [ ] Docker Compose production file
- [ ] Caddy reverse proxy in front of the gateway
