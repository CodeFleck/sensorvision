# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/org/sensorvision/...` contains Spring Boot code organized by feature: `controller`, `service`, `mqtt`, `simulator`, `model`, and `repository`.
- `src/main/resources` holds configuration, Flyway migrations in `db/migration`, and other runtime assets.
- `src/test/java/org/sensorvision` mirrors production packages, with payload fixtures stored in `src/test/resources`.
- Operational assets live under `ops/`, while Mosquitto broker files reside in `mosquitto/config`, `mosquitto/data`, and `mosquitto/log`.

## Build, Test, and Development Commands
- `./gradlew clean build` compiles, runs JUnit 5 tests, and produces the executable jar in `build/libs/`.
- `./gradlew bootRun` launches the application for iterative local development against live services.
- `./gradlew test --info` executes the test suite with verbose diagnostics for flaky assertions.
- `docker-compose up -d` starts Postgres, Mosquitto, and other dependencies; `docker-compose logs mosquitto` inspects broker output.
- `docker-compose down` stops supporting services before branch changes to prevent stale containers.

## Coding Style & Naming Conventions
- Write Java 17 with four-space indentation and avoid wildcard imports; annotate domain entities with Lombok where helpful.
- Name classes in PascalCase, methods and fields in camelCase, and keep DTOs immutable when practical.
- Configuration keys stay kebab-case in `application.yml`; MQTT topics follow `sensorvision/<domain>/...`.
- Mirror the existing package layout when adding features under `org.sensorvision`.

## Testing Guidelines
- Favor focused unit tests and Spring slice tests (e.g., `@DataJpaTest`, `@SpringBootTest`) over full-stack startups.
- Name test classes `*Tests` and match the production package of the code under test.
- Guard Docker-dependent integrations behind explicit profiles so `./gradlew test` stays fast by default.
- Store representative MQTT payload samples alongside tests in `src/test/resources`.

## Commit & Pull Request Guidelines
- Write imperative, <=72-character subjects (e.g., `service: handle offline device alerts`) and group related edits.
- Provide descriptive bodies with context, linked issues, and rationale for non-obvious changes.
- Pull requests should outline the change, list manual verification steps, and include Grafana screenshots when dashboards move.
- Call out adjustments under `ops/` or `mosquitto/`, request subsystem reviewers, and wait for CI to pass before merging.

## Security & Configuration Tips
- Keep secrets out of the repo; prefer environment overrides for sensitive configuration values in `application.yml`.
- Coordinate telemetry adapter or broker configuration updates with the ops team so `mosquitto` and `ops/` assets stay aligned.
- Use `docker-compose down` before switching branches to avoid persisting stale database or broker state between runs.

