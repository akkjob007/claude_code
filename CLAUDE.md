# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot backend application built with Maven. Uses an embedded database (H2) for local development — no external database setup required.

## Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName
```

## Architecture

- **Framework**: Spring Boot
- **Build tool**: Maven (`pom.xml`)
- **Database**: Embedded H2 (auto-configured by Spring Boot; schema/data initialized via `src/main/resources/schema.sql` and `data.sql` if present)
- **H2 Console**: Available at `http://localhost:8080/h2-console` when enabled in `application.properties`

## Key Configuration

`src/main/resources/application.properties` (or `application.yml`) controls datasource, server port, and H2 console settings. Typical embedded DB config:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true
```

## Settings

- `.claude/settings.local.json`: MCP server auto-enablement is disabled (`enableAllProjectMcpServers: false`).
