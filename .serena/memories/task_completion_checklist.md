# Task Completion Checklist

## Before Committing Changes
1. **Build Verification**
   ```bash
   mvn clean compile
   ```

2. **Run Tests**
   ```bash
   mvn test -Dspring.profiles.active=local
   ```

3. **Package Check**
   ```bash
   mvn clean package -DskipTests
   ```

## Code Quality Checks
- Verify Lombok annotations are properly used
- Check reactive stream implementations don't block
- Ensure proper error handling with custom exceptions
- Validate Spring profiles configuration

## Performance Considerations
- Check for blocking operations in reactive code
- Verify circuit breaker configurations
- Review timeout settings for WebClient calls
- Ensure async logging is used where appropriate

## Integration Testing
- Test with different Spring profiles (local, dev, sit, prd)
- Verify SOAP service integrations
- Check circuit breaker functionality
- Test error scenarios and fallbacks

## Documentation Updates
- Update CLAUDE.md if commands change
- Update module README files if architecture changes
- Document any new configuration properties