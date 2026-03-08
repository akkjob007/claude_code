# Requirements Specification Document

## Project Overview

A Spring Boot REST API backend application providing a hello world endpoint and user management CRUD operations with caching.

## Functional Requirements

### FR-1: Hello World Endpoint

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /helloworld` |
| **Description** | Returns a simple hello world greeting message |
| **Request Body** | None |
| **Response Code** | `200 OK` |
| **Response Body** | `{"message": "Hello, World!"}` |
| **Content-Type** | `application/json` |

### FR-2: Create User Endpoint

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /users` |
| **Description** | Saves a new user to the database |
| **Request Body** | JSON with user fields (see Data Model) |
| **Response Code** | `201 Created` |
| **Response Body** | The created user object with generated ID |
| **Content-Type** | `application/json` |

**Validation Rules:**
- `name`: Required, non-blank, max 100 characters
- `email`: Required, must be a valid email format, max 255 characters

**Error Responses:**
- `400 Bad Request` -- when validation fails, with descriptive error messages

### FR-3: Get User by ID Endpoint

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /users/{id}` |
| **Description** | Retrieves a user by their unique ID, with LRU caching |
| **Path Parameter** | `id` (Long) -- the user's unique identifier |
| **Response Code** | `200 OK` |
| **Response Body** | The user object |
| **Content-Type** | `application/json` |

**Caching Strategy:**
- LRU (Least Recently Used) cache for user lookups
- Cache capacity: 100 entries (configurable)
- Cache is populated on read and invalidated/updated on write (POST)
- Purpose: reduce database queries for frequently accessed users

**Error Responses:**
- `404 Not Found` -- when no user exists with the given ID

## Data Model

### User Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Primary key, auto-generated | Unique identifier |
| `name` | String | Not blank, max 100 chars | User's full name |
| `email` | String | Valid email, max 255 chars | User's email address |

## Non-Functional Requirements

### NFR-1: Technology Stack
- **Framework**: Spring Boot (latest stable)
- **Build Tool**: Maven
- **Database**: Embedded H2 (in-memory, `jdbc:h2:mem:testdb`)
- **Java Version**: 17+
- **Testing**: JUnit 5, Spring Boot Test

### NFR-2: Caching
- LRU caching implemented using `LinkedHashMap` with access-order or Spring's `@Cacheable` with a custom CacheManager
- Cache must be thread-safe for concurrent access

### NFR-3: API Design
- RESTful conventions followed
- JSON request/response format
- Proper HTTP status codes used
- Input validation with meaningful error messages

### NFR-4: Database
- H2 console enabled for development at `/h2-console`
- Schema auto-created by JPA/Hibernate
- No external database dependencies

### NFR-5: Testing
- Unit tests for service layer (including cache behavior)
- Integration tests for controller endpoints
- All tests must pass via `mvn test`

## Assumptions

1. No authentication/authorization is required for any endpoint
2. The user entity is intentionally simple (name + email) -- no password or role fields
3. The LRU cache is application-level (single-node, not distributed)
4. The H2 database is in-memory and data is lost on application restart
5. No pagination is required for the current scope
6. Email uniqueness is NOT enforced (unless user specifies otherwise)

## Out of Scope

- User update (PUT/PATCH) and delete (DELETE) endpoints
- Authentication and authorization
- Distributed caching
- Persistent database storage
- API versioning
- Pagination
