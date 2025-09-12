# Suggested Commands for KOL ITG Gateway

## Build Commands
```bash
# Build the entire project
mvn clean compile

# Package the application
mvn clean package

# Skip tests during build
mvn clean package -DskipTests
```

## Testing Commands
```bash
# Run all tests
mvn test

# Run tests with specific profile
mvn test -Dspring.profiles.active=local

# Run tests for specific module
mvn test -pl app
mvn test -pl common
```

## Running the Application
```bash
# Run the application locally (uses local profile, port 8001)
mvn spring-boot:run -pl app

# Run with specific profile
mvn spring-boot:run -pl app -Dspring.profiles.active=dev
```

## Useful Development Commands
```bash
# Clean and rebuild
mvn clean install

# Generate project dependency tree
mvn dependency:tree

# Check for plugin updates
mvn versions:display-plugin-updates

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Windows System Commands
```bash
# List files and directories
dir
ls (if Git Bash is available)

# Find files
findstr /s /i "pattern" *.java

# Current directory
cd

# Environment variables
set
echo %JAVA_HOME%
```