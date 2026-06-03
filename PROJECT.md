# Project: Shop — microservice e-commerce app

## Goal
A simple but scale-ready online store: browse products, add to cart, place orders,
user accounts. Built microservice-style so it can grow.

## Architecture (build to this)
- web/         React + Vite storefront. Calls the API only through the gateway.
- gateway/     Java Spring Boot API gateway. Single public entry (/api).
- product-svc/ Java Spring Boot. Product catalog. Owns its tables. Caches hot reads in Redis.
- order-svc/   Java Spring Boot. Orders + checkout. Publishes "order.placed" to RabbitMQ.
- user-svc/    Java Spring Boot. Accounts + auth (JWT).
- analytics/   Python FastAPI. Consumes RabbitMQ events, stores simple metrics.
- scraper/     Python. Optional competitor-price scraper (run on schedule).
- Shared infra (already running): PostgreSQL, Redis, RabbitMQ on the 'stacknet' network.

## Rules
- Each service has its own Dockerfile and joins the external 'stacknet' network.
- Bind services to internal network only; the gateway is the sole entry, behind Caddy /api.
- Every service has a /health endpoint.
- Write tests. Commit in small steps with clear messages.
- [SCALE SEAM] Single instances now. Later: multiple replicas + load balancer,
  managed Postgres with read replicas, managed Kafka instead of RabbitMQ, Redis cluster.

## Milestone 1 (start here)
1. user-svc: register/login with JWT.
2. product-svc: list + view products (seed 10 demo products), cache list in Redis.
3. gateway: route /api/users and /api/products.
4. web: product list page + login page, calling the gateway.
5. order-svc: place an order, publish order.placed.
6. analytics: count orders from the event stream.
7. CI: tests pass; deploy with docker compose; health checks green.
