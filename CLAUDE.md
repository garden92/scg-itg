# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KOS-O/L Integration Gateway - A Spring Cloud Gateway-based API Gateway for interfacing with KOS-ESB. Built with a Modular Monolithic architecture using Spring Boot 3.5.4 and Java 17.

## Maven Commands

```bash
# Build the entire project
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package

# Run the application locally
mvn spring-boot:run -pl app

# Skip tests during build
mvn clean package -DskipTests
```

## Development Setup

- **Java Version**: 17
- **Spring Boot**: 3.5.4 
- **Spring Cloud**: 2025.0.0
- **Default Profile**: local (runs on port 8001)
- **Production Profile**: Changes port to 8080

## Module Architecture

### app (Main Application Module)
- Contains the Spring Boot main application (`Application.java`)
- Integrates all domain modules and runs the main application
- Key components:
  - **Filters**: Global request validation, logging, rewrite filters
  - **Handlers**: SOAP request handling, error handling
  - **Endpoints**: Fallback endpoints for circuit breaker
- Dependencies: Spring Cloud Gateway, WebFlux, Security, Resilience4j

### common (Shared Module)
- Contains global objects used across all modules
- Key components:
  - **SOAP Models**: Envelope, Header structures for SOAP communication
  - **Feign Clients**: Configuration for SOAP and JSON HTTP clients
  - **Exceptions**: Business exceptions with custom error codes
  - **Annotations**: Integration logging annotations
- Dependencies: OpenFeign, OpenAPI, JAXB for XML processing

## Configuration Structure

The application uses Spring profiles:
- **local**: Development (port 8001)
- **dev**: Development environment
- **sit**: System Integration Testing
- **prd**: Production

Each profile configures different SOAP service endpoints for:
- ORD (Order) PO/ESB endpoints
- CRM (Customer) PO/ESB endpoints
- Stub endpoints for testing

## Circuit Breaker Configuration

Uses Resilience4j for fault tolerance:
- Minimum 10 calls required before circuit breaker activation
- 60% failure threshold
- 5-second wait duration in open state
- Monitors both failure rate and slow call rate

## Key Integration Patterns

### SOAP Service Integration
- Uses Feign clients for SOAP service communication
- XML/JSON format conversion capabilities
- Custom SOAP envelope structures in `common.model.soap`

### Error Handling
- Global error handler in `app.handler.ErrorHandler`
- Custom business exceptions with error codes
- Integration logging with custom annotations

### Security & Logging
- Spring Security integration
- Custom logging filters for request/response tracking
- Business monitoring (BMON) logging support

## Testing

Run tests with appropriate Spring profiles:
```bash
# Run with local profile
mvn test -Dspring.profiles.active=local

# Run specific module tests
mvn test -pl app
mvn test -pl common
```