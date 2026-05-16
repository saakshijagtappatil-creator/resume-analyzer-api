# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run with a specific profile
SPRING_PROFILE=dev ./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SomeServiceTest

# Docker build and run
docker build -t resume-analyzer-api .
docker run -p 8080:8080 --env-file .env resume-analyzer-api

Copy .env.example to .env and fill in secrets before running locally.

## Architecture

Resume Analyzer & Job Match API — a Spring Boot 3.2.5 / Java 17 backend that uses Claude (Anthropic) to analyze uploaded PDF resumes and match them against job descriptions.

Request flow:

HTTP → Controller → Service (business logic) → Repository (JPA) → PostgreSQL
↓
RabbitMQ (async tasks, e.g. AI analysis)
↓
Claude API (Anthropic)

Redis is used for caching; results may be cached before hitting the database.

Package layout (com.resumeanalyzer.api):

controller/              REST endpoints (thin layer, delegates to services)
service/ + service/impl/ Business logic
repository/              Spring Data JPA repositories
entity/                  JPA-mapped domain models
dto/request/ & response/ Request/response payloads (never expose entities directly)
security/                JWT filter chain and token utilities
config/                  Spring @Configuration beans (Redis, RabbitMQ, WebClient, etc.)
exception/               Global exception handler (@ControllerAdvice) and custom exceptions
messaging/producer/      RabbitMQ publish
messaging/consumer/      RabbitMQ consume
util/                    Shared helpers (PDF parsing via PDFBox, etc.)

Infrastructure dependencies:
- PostgreSQL — primary store; HikariCP pooling
- Redis — caching (Lettuce driver)
- RabbitMQ — async message queue for AI analysis jobs
- Anthropic Claude API — configured model claude-opus-4-6, called via Spring WebFlux WebClient

## Spring Profiles

Profile   DDL mode      Swagger    Log level
dev       update        enabled    DEBUG
staging   validate      enabled    INFO
prod      validate      disabled   WARN

Set SPRING_PROFILE env var to choose a profile.

## Key Configuration

- Config files: application.yml (base) + application-{profile}.yml overrides
- Local secrets loaded from .env (excluded from git); template is .env.example
- File uploads: PDF only, 5 MB max, stored in FILE_UPLOAD_DIR (default /tmp/resumes)
- API docs (dev/staging): http://localhost:8080/swagger-ui.html, OpenAPI spec at /v3/api-docs
- Health/metrics: /actuator/health, /actuator/prometheus

## Coding Rules (Non-Negotiable)

- Never use System.out.println — always use @Slf4j logger
- Controllers always return ResponseEntity<ApiResponse<T>> — never raw objects
- Services are always @Transactional — never put DB logic in controllers
- Use Lombok — no manual getters/setters/constructors
- All secrets via ${ENV_VAR} — never hardcoded anywhere
- Redis key pattern: resume:analysis:{sha256Hash}
- All heavy tasks async via RabbitMQ — never block the HTTP thread
- PDF parsing and AI calls always go through the consumer, never directly from controller

## Deployment

Layer               Platform
Backend             Oracle OKE (Kubernetes)
Frontend            Vercel (Nuxt 3)
Container Registry  Oracle OCIR
Database            PostgreSQL via Neon/Supabase (external to cluster)
Cache               Redis (self-hosted inside OKE)
Message Broker      RabbitMQ (self-hosted inside OKE)
Monitoring          Grafana + Loki via Grafana Cloud
CI/CD               GitHub Actions → Docker → OCIR → OKE rolling deploy

## Security Rules

- Public endpoints: /auth/** and /actuator/health only
- All other endpoints require valid JWT
- Swagger disabled in prod — never expose in production
- Never commit .env — only .env.example

## K8s Structure

k8s/
├── staging/    # 1 replica, min 1 max 3 pods
└── prod/       # 2 replicas, min 2 max 10 pods

Secrets injected exclusively from GitHub Actions — never manually.

## Conventions

- DTOs are the API contract — never return JPA entities from controllers
- Background/async work goes through RabbitMQ; producer sends, consumer calls the service
- JWT secret and all credentials come from environment variables only — never hardcode them
- JPA ddl-auto is validate in staging and prod; schema changes require a migration script