# KOL ITG Gateway Project Overview

## Purpose
KOS-O/L Integration Gateway - A Spring Cloud Gateway-based API Gateway for interfacing with KOS-ESB. Built with a Modular Monolithic architecture.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.4, Spring Cloud Gateway 2025.0.0
- **Architecture**: Modular Monolithic
- **Build Tool**: Maven
- **Dependencies**: Spring WebFlux, Spring Security, Resilience4j, OpenFeign, JAXB

## Module Structure
### app (Main Application Module)
- Main Spring Boot application entry point
- Gateway filters, handlers, endpoints
- Route configuration and circuit breaker setup
- Dependencies: Spring Cloud Gateway, WebFlux, Security, Resilience4j

### common (Shared Module)  
- Global objects and utilities shared across modules
- SOAP models, Feign clients, exceptions, annotations
- Dependencies: OpenFeign, OpenAPI, JAXB for XML processing

## Key Components
- **Filters**: GlobalBmonLoggingFilterFactory, GlobalRequestValidationFilter, RewriteGlobalFilter
- **Handlers**: SoapRequestHandler, ErrorHandler
- **Strategy Pattern**: EndpointStrategyResolver with CRM, ORD, Stub strategies
- **Circuit Breaker**: Resilience4j configuration for fault tolerance
- **SOAP Integration**: Custom SOAP envelope structures and converters