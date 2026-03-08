# Technical Architecture Document

## 1. Overview

This document defines the technical architecture for a Spring Boot REST API application with three endpoints: a hello world greeting, user creation, and user retrieval with LRU caching. The application uses an embedded H2 database and follows a standard layered architecture.

## 2. Architecture Style

**Layered Architecture** with three tiers:

```
[Client / HTTP]
       |
  [Controller Layer]   -- REST endpoints, input validation, HTTP concerns
       |
  [Service Layer]      -- Business logic, caching
       |
  [Repository Layer]   -- Data access via Spring Data JPA
       |
  [H2 Database]        -- Embedded in-memory store
```

## 3. Project Structure

```
src/
 main/
  java/com/example/userapi/
   UserApiApplication.java              -- Spring Boot main class
   controller/
    HelloWorldController.java           -- GET /helloworld
    UserController.java                 -- POST /users, GET /users/{id}
   model/
    User.java                           -- JPA entity
   dto/
    CreateUserRequest.java              -- Request DTO with validation annotations
    UserResponse.java                   -- Response DTO
    HelloWorldResponse.java             -- Response DTO for hello world
    ErrorResponse.java                  -- Standardized error response
   service/
    UserService.java                    -- Interface
    UserServiceImpl.java                -- Implementation with LRU cache
   repository/
    UserRepository.java                 -- Spring Data JPA repository
   cache/
    LruCache.java                       -- Generic thread-safe LRU cache
   exception/
    UserNotFoundException.java          -- Custom exception
    GlobalExceptionHandler.java         -- @ControllerAdvice for error handling
  resources/
   application.properties               -- H2, JPA, server config
 test/
  java/com/example/userapi/
   controller/
    HelloWorldControllerTest.java       -- Integration test
    UserControllerTest.java             -- Integration test
   service/
    UserServiceImplTest.java            -- Unit test with cache verification
   cache/
    LruCacheTest.java                   -- Unit test for cache logic
```

## 4. Component Design

### 4.1 Controller Layer

**HelloWorldController**
- `@RestController`
- Maps `GET /helloworld`
- Returns `HelloWorldResponse` with message "Hello, World!"
- No dependencies on service or repository layers

**UserController**
- `@RestController` with base path (no prefix -- endpoints are `/users`)
- `POST /users` -- accepts `@Valid @RequestBody CreateUserRequest`, delegates to `UserService`, returns `ResponseEntity<UserResponse>` with 201 status
- `GET /users/{id}` -- accepts `@PathVariable Long id`, delegates to `UserService`, returns `UserResponse`

### 4.2 DTO Layer

Separate DTOs from the JPA entity to decouple API contract from persistence:

**CreateUserRequest**
```java
String name;   // @NotBlank, @Size(max = 100)
String email;  // @NotBlank, @Email, @Size(max = 255)
```

**UserResponse**
```java
Long id;
String name;
String email;
```

**HelloWorldResponse**
```java
String message;
```

**ErrorResponse**
```java
int status;
String error;
Map<String, String> details;  // field-level validation errors (nullable)
```

### 4.3 Service Layer

**UserService (interface)**
```java
UserResponse createUser(CreateUserRequest request);
UserResponse getUserById(Long id);
```

**UserServiceImpl**
- Injects `UserRepository` and `LruCache<Long, User>`
- `createUser`: saves entity via repository, puts result in cache, returns response
- `getUserById`: checks cache first; on miss, queries repository, populates cache, returns response; throws `UserNotFoundException` on miss from both cache and DB

### 4.4 LRU Cache Design

**Custom `LruCache<K, V>` class** rather than Spring `@Cacheable` -- this gives explicit control over LRU eviction, is easy to test, and satisfies the requirement transparently.

**Implementation approach:**
- Wraps a `LinkedHashMap` with `accessOrder = true`
- Overrides `removeEldestEntry()` to enforce max capacity
- All public methods (`get`, `put`, `remove`, `containsKey`, `size`, `clear`) are `synchronized` for thread safety
- Capacity is configurable via constructor parameter
- Default capacity: 100 (injected from `application.properties` as `app.cache.capacity=100`)

**Cache lifecycle:**
- **On GET /users/{id}**: Check cache -> if hit, return cached value. If miss, query DB, put in cache, return.
- **On POST /users**: Save to DB, put new user in cache (pre-warm for immediate subsequent reads).

### 4.5 Repository Layer

**UserRepository**
- Extends `JpaRepository<User, Long>`
- No custom query methods needed for current requirements

### 4.6 Exception Handling

**UserNotFoundException**
- Extends `RuntimeException`
- Thrown when `GET /users/{id}` finds no matching user

**GlobalExceptionHandler** (`@RestControllerAdvice`)
- Handles `UserNotFoundException` -> 404 with `ErrorResponse`
- Handles `MethodArgumentNotValidException` -> 400 with field-level error details
- Handles generic `Exception` -> 500 with generic message

## 5. Data Model

### User Table (JPA-managed)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL
);
```

**JPA Entity annotations:**
- `@Entity`, `@Table(name = "users")`
- `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` on `id`
- `@Column(nullable = false, length = 100)` on `name`
- `@Column(nullable = false, length = 255)` on `email`

## 6. Configuration

### application.properties

```properties
# Server
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Application
app.cache.capacity=100
```

## 7. Maven Dependencies

```xml
<!-- Spring Boot Starters -->
spring-boot-starter-web          -- REST controllers, Jackson JSON
spring-boot-starter-data-jpa     -- JPA, Hibernate
spring-boot-starter-validation   -- Jakarta Bean Validation (@Valid, @NotBlank, etc.)

<!-- Database -->
h2 (runtime scope)               -- Embedded database

<!-- Testing -->
spring-boot-starter-test          -- JUnit 5, Mockito, MockMvc, AssertJ
```

**Spring Boot version:** 3.2.x (Java 17 baseline)

## 8. Testing Strategy

### 8.1 Unit Tests

**LruCacheTest**
- Test basic put/get operations
- Test LRU eviction when capacity exceeded
- Test access-order promotion (recently accessed items survive eviction)
- Test remove operation
- Test cache miss returns null

**UserServiceImplTest**
- Mock `UserRepository` with Mockito
- Test `createUser` saves to DB and populates cache
- Test `getUserById` returns from cache on hit (verify repository NOT called)
- Test `getUserById` queries DB on cache miss and populates cache
- Test `getUserById` throws `UserNotFoundException` when not in cache or DB

### 8.2 Integration Tests (MockMvc)

**HelloWorldControllerTest**
- `GET /helloworld` returns 200 with expected JSON

**UserControllerTest**
- `POST /users` with valid body returns 201 with created user
- `POST /users` with missing name returns 400
- `POST /users` with invalid email returns 400
- `GET /users/{id}` with existing user returns 200
- `GET /users/{id}` with non-existent ID returns 404

## 9. Sequence Diagrams

### GET /users/{id} (Cache Hit)

```
Client -> UserController: GET /users/1
UserController -> UserService: getUserById(1)
UserService -> LruCache: get(1)
LruCache -> UserService: User (hit)
UserService -> UserController: UserResponse
UserController -> Client: 200 OK + JSON
```

### GET /users/{id} (Cache Miss)

```
Client -> UserController: GET /users/1
UserController -> UserService: getUserById(1)
UserService -> LruCache: get(1)
LruCache -> UserService: null (miss)
UserService -> UserRepository: findById(1)
UserRepository -> UserService: Optional<User>
UserService -> LruCache: put(1, user)
UserService -> UserController: UserResponse
UserController -> Client: 200 OK + JSON
```

### POST /users

```
Client -> UserController: POST /users {name, email}
UserController -> UserService: createUser(request)
UserService -> UserRepository: save(user)
UserRepository -> UserService: User (with generated ID)
UserService -> LruCache: put(id, user)
UserService -> UserController: UserResponse
UserController -> Client: 201 Created + JSON
```

## 10. Design Decisions

| Decision | Rationale |
|----------|-----------|
| Custom LRU cache over Spring @Cacheable | Explicit control over eviction policy; easier to unit test; requirement specifically asks for LRU strategy |
| DTOs separate from entity | Decouples API contract from persistence; allows independent evolution |
| No Lombok | Keeps dependencies minimal; records or manual getters/setters for clarity |
| GlobalExceptionHandler | Centralized error handling; consistent error response format |
| synchronized cache methods | Simple thread safety; adequate for single-node in-memory use case |
| Cache pre-warming on POST | Avoids immediate cache miss after creating a user |
