# Code Style and Conventions

## Java Conventions
- **Package Structure**: `com.kt.kol.{module}.{domain}`
- **Class Naming**: PascalCase (e.g., `SoapRequestHandler`, `GlobalBmonLoggingFilterFactory`)
- **Method Naming**: camelCase with descriptive names
- **Variable Naming**: camelCase
- **Constants**: UPPER_SNAKE_CASE in constant classes

## Spring Boot Patterns
- **Component Annotation**: Use specific annotations (@Service, @Component, @Configuration)
- **Dependency Injection**: Constructor injection with `@RequiredArgsConstructor` from Lombok
- **Configuration**: YAML format for application properties
- **Profiles**: local, dev, sit, prd for different environments

## Architecture Patterns Used
- **Strategy Pattern**: EndpointStrategy implementations for different service types
- **Factory Pattern**: AbstractGatewayFilterFactory for custom filters
- **Template Pattern**: SoapTemplateManager for SOAP operations

## Lombok Usage
- `@Slf4j` for logging
- `@RequiredArgsConstructor` for constructor injection
- `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` for data classes

## Reactive Programming
- Use `Mono` and `Flux` for reactive streams
- Chain operations with `.flatMap()`, `.map()`, `.switchIfEmpty()`
- Handle errors with `.onErrorResume()`

## Error Handling
- Custom exceptions extending RuntimeException
- Global error handlers
- Proper error codes and messages