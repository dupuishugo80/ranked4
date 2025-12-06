# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Shop service is a Spring Boot 3.5.7 microservice for the Ranked4 platform, handling product catalog management and purchase transactions. It uses Java 21, PostgreSQL for persistence, and communicates with the userprofile-service via WebClient for payment processing.

## Build and Development Commands

### Build
```bash
./mvnw clean package
```

### Build (skip tests)
```bash
./mvnw clean package -DskipTests
```

### Run locally
```bash
./mvnw spring-boot:run
```

### Docker build
```bash
docker build -t shop-service .
```

## Architecture

### Service Communication Pattern
The shop service follows a microservices architecture and communicates with other Ranked4 services:
- **userprofile-service**: Called synchronously via WebClient to debit gold when users purchase products
  - Endpoint: POST `/api/profiles/debit-gold?amount={amount}` with `X-User-Id` header
  - Handles insufficient funds (402 PAYMENT_REQUIRED) and user not found (404) responses
- **Gateway integration**: Expects `X-User-Id` and `X-User-Roles` headers forwarded from the API gateway for authentication/authorization

### Transaction Flow
Purchase transactions (`ShopService.buyProduct`) are handled with `@Transactional` to ensure atomicity:
1. Fetch product from database
2. Call userprofile-service to debit gold (blocking WebClient call)
3. If payment succeeds, save purchase record
4. If payment fails, transaction rolls back

### Security Model
- Spring Security is configured in `SecurityConfig` with CSRF disabled and stateless session management
- All endpoints currently permit all requests (security handled at gateway level)
- Admin operations (like creating products) validate `ROLE_ADMIN` in the `X-User-Roles` header manually in the controller

### Data Models
- **Product**: Catalog items with name (unique), description, imageUrl, and price (int, representing gold cost)
- **Purchase**: Transaction records linking userId (UUID) to productId with priceAtPurchase and timestamp

### Pagination
The service uses Spring Data's `Pageable` for product listings. Default is 10 items per page, sorted by ID descending.

## Configuration

Key properties in `application.properties`:
- Server runs on port 8080
- Database connection: `jdbc:postgresql://postgres:5432/ranked4_db`
- Hibernate DDL auto-update enabled
- userprofile-service base URL: `http://userprofile-service:8080`

These values assume Docker Compose networking (service names as hostnames).

## API Endpoints

- `POST /api/shop/buy/{productId}` - Purchase a product (requires `X-User-Id` header)
- `GET /api/shop/products` - Get paginated product list
- `POST /api/shop/products` - Create new product (requires `ROLE_ADMIN` in `X-User-Roles` header)
