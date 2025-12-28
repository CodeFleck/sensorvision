# Repository Guidelines

## Project Structure & Module Organization
- Spring Boot services live under src/main/java/org/indcloud, grouped by feature (controller, service, mqtt, simulator, model, epository). Example: src/main/java/org/indcloud/controller/DashboardController.java.
- Config, Flyway migrations, and static assets sit in src/main/resources (migrations in db/migration).
- Tests mirror production packages at src/test/java/org/indcloud; shared fixtures live in src/test/resources.
- Operational tooling is in ops/, and Mosquitto broker config/data/logs are under mosquitto/.

## Build, Test, and Development Commands
- ./gradlew clean build — compile, run JUnit 5 suite, and publish the fat jar to uild/libs/.
- ./gradlew bootRun — start the Spring Boot app for iterative work; pairs well with docker-compose up -d.
- ./gradlew test --info — rerun tests with verbose logging to chase flaky failures.
- docker-compose up -d / docker-compose down — provision or tear down Postgres, Mosquitto, and support services; inspect broker output with docker-compose logs mosquitto.

## Coding Style & Naming Conventions
- Target Java 17, four-space indentation, no wildcard imports, and prefer Lombok on entities and DTO builders.
- Classes use PascalCase, methods and fields camelCase, DTO records stay immutable; align new packages under org.indcloud.
- Keep configuration keys kebab-case in pplication.yml; MQTT topics follow indcloud/<domain>/<resource>.

## Testing Guidelines
- Use focused unit tests or Spring slice tests (@DataJpaTest, @SpringBootTest) instead of full-stack startups.
- Name test classes *Tests and match the package of the class under test.
- Place representative payloads in src/test/resources; guard Docker-dependent tests behind explicit profiles.
- Run ./gradlew test before pushing; add regression coverage for all bug fixes.

## Commit & Pull Request Guidelines
- Commit subjects are imperative, =72 chars (e.g., service: handle offline device alerts) with contextual bodies and linked issues.
- Group related changes, include manual verification steps, and attach Grafana screenshots when dashboards shift.
- Flag edits under ops/ or mosquitto/ and request the relevant subsystem reviewers; wait for CI to pass before merge.

## Security & Configuration Tips
- Never commit secrets; rely on environment overrides layered over pplication.yml.
- Coordinate broker or telemetry adapter changes with the ops team so mosquitto/ and ops/ stay in sync.
- Always run docker-compose down before changing branches to avoid stale service state.
