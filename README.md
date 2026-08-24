# Notification API

REST API for scheduling and managing notifications, built and developed by Fernando Torres with **Java 21**, **Spring Boot 3** and **PostgreSQL**.

The API lets clients register delivery **channels** (email, SMS, push, etc.), create notifications targeted at those channels, schedule them for future delivery, and automatically dispatches due notifications through a background scheduler.

## Features

- CRUD for notification **channels**
- Create, list (paginated & filterable by status), cancel and delete **notifications**
- Automatic status lifecycle: `PENDING → SCHEDULED → SENT / FAILED / CANCELLED`
- Background **scheduled task** that dispatches due notifications every 30 seconds
- Startup **data seeding** (`DataLoader`) with default channels (EMAIL, SMS, PUSH)
- Bean Validation on all input DTOs
- Centralized exception handling with consistent JSON error responses
- Interactive API documentation via **Swagger / OpenAPI**
- Unit tests with **JUnit 5** and **Mockito**
- Containerized with **Docker** and **Docker Compose** (API + PostgreSQL + frontend)
- **GitHub Actions** CI pipeline running the test suite on every push
- A lightweight **frontend** (`/frontend`) to manage channels and notifications from the browser — plain HTML/CSS/JS, no build step

## Tech Stack

| Layer            | Technology                         |
|------------------|------------------------------------|
| Language         | Java 21                            |
| Framework        | Spring Boot 3 (Web, Data JPA, Validation) |
| Database         | PostgreSQL                         |
| ORM              | Hibernate / Spring Data JPA        |
| Build            | Maven                              |
| Documentation    | springdoc-openapi (Swagger UI)     |
| Testing          | JUnit 5, Mockito, AssertJ          |
| Containerization | Docker, Docker Compose             |
| CI               | GitHub Actions                     |
| Frontend         | Vanilla HTML/CSS/JS (fetch API)    |

## Architecture

Simple layered architecture:

```
Controller → Service → Repository → Database
```

with DTOs (Java records) for input/output, a dedicated mapper for entity-to-DTO conversion, and a global exception handler translating domain exceptions into consistent HTTP error responses.

```
notification-api/
├── .github/workflows/ci.yml
├── src/
│   ├── main/
│   │   ├── java/com/portfolio/notificationapi/
│   │   │   ├── config/        # DataLoader, OpenAPI config
│   │   │   ├── controller/    # REST controllers
│   │   │   ├── dto/           # Request/response records
│   │   │   ├── entity/        # JPA entities (Notification, Channel, NotificationStatus)
│   │   │   ├── exception/     # Custom exceptions + global handler
│   │   │   ├── mapper/        # Entity <-> DTO mapping
│   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   ├── scheduler/     # Notification dispatch scheduler
│   │   │   └── service/       # Business logic
│   │   └── resources/application.yml
│   └── test/
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.9+
- Docker & Docker Compose (optional, for containerized run)

### Run with Docker Compose (recommended)

```bash
docker compose up --build
```

This starts PostgreSQL, the API and the frontend together:
- API: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run locally

1. Start a PostgreSQL instance and create a `notification_db` database.
2. Set the connection environment variables if they differ from the defaults (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`).
3. Run the application:

```bash
mvn spring-boot:run
```

### API Documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

## Example Requests

Create a channel:

```bash
curl -X POST http://localhost:8080/api/v1/channels \
  -H "Content-Type: application/json" \
  -d '{"name": "EMAIL", "description": "Email notifications", "active": true}'
```

Create a notification:

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Welcome!",
        "message": "Thanks for signing up.",
        "recipient": "user@example.com",
        "channelId": 1,
        "scheduledAt": "2026-09-01T10:00:00"
      }'
```

List notifications by status:

```bash
curl "http://localhost:8080/api/v1/notifications?status=PENDING&page=0&size=10"
```

Cancel a notification:

```bash
curl -X PATCH http://localhost:8080/api/v1/notifications/1/cancel
```

## Running Tests

```bash
mvn test
```

## Notes / Lessons Learned

While building this project, an `IdentifierGenerationException` was hit on the `Channel` entity: it was originally mapped with `GenerationType.SEQUENCE` without a matching `@SequenceGenerator`, so Hibernate could not resolve the sequence name against PostgreSQL at startup. The fix was switching to `GenerationType.IDENTITY`, letting PostgreSQL manage the auto-incrementing column directly — a good reminder to always match the ID generation strategy to what the target database actually supports.

## Frontend

A small browser UI lives in `/frontend` (plain HTML/CSS/JS, no build step) for managing channels and notifications without needing Swagger or curl. Run it either via `docker compose up --build` (see above) or by opening `frontend/index.html` directly in a browser and pointing the "API base URL" field at your running backend.

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for a step-by-step guide to deploying the full stack for free (Render for the API + PostgreSQL, Netlify for the frontend), plus a Docker Compose option for self-hosting on a VPS.

## License

This project is available for portfolio and educational purposes.


## About the Project

This project was designed and developed by **Fernando Torres** as a backend portfolio project, with a focus on building a production-oriented REST API using Java and Spring Boot.

The project demonstrates practical experience with REST API development, layered architecture, database persistence, validation, exception handling, automated testing, scheduled background processing, Docker containerization and CI/CD.

## Author

**Fernando Torres**  
Java Developer | Backend Developer | IT Professional

- GitHub: https://github.com/SEU_USUARIO)](https://github.com/FernandoTorresD97
- LinkedIn: www.linkedin.com/in/fernandotorresdias
