# Repository Guidelines

## Project Structure & Module Organization
SensorVision is a Spring Boot 3 service. Production code lives in `src/main/java/org/sensorvision`, split by concern: `controller/` for REST entrypoints, `service/` for orchestration and telemetry ingestion, `mqtt/` for broker adapters, `simulator/` for the smart-meter publisher, and `model/` + `repository/` for JPA entities. Configuration beans reside in `config/` while DTOs are grouped under `dto/`. Application settings and Flyway migrations sit in `src/main/resources`, with SQL scripts under `db/migration`. Operational assets stay in `ops/grafana` and `ops/prometheus`, and the Mosquitto broker files are in `mosquitto/config`, `mosquitto/data`, and `mosquitto/log`.

## Build, Test, and Development Commands
Run `./gradlew clean build` (`gradlew.bat` on Windows) to compile, run tests, and produce the executable jar in `build/libs/`. Launch the app with `./gradlew bootRun` for iterative development. Execute `./gradlew test` for the JUnit 5 test suite; add `--info` when inspecting flaky behavior. Start supporting services with `docker-compose up -d` and inspect them via `docker-compose logs mosquitto` or `docker-compose logs postgres`. Shut everything down using `docker-compose down` before switching branches.

## Coding Style & Naming Conventions
Write Java 17 code with four-space indentation and avoid wildcard imports. Name classes with PascalCase (`DeviceService`), methods and fields camelCase, configuration properties with kebab-case in `application.yml`, and MQTT topics using the `sensorvision/<domain>/...` template. Keep DTOs immutable where possible and annotate domain models with Lombok for boilerplate. When adding new packages, mirror the existing feature-based structure under `org.sensorvision`.

## Testing Guidelines
Place tests under `src/test/java/org/sensorvision`, mirroring production packages and naming classes `*Tests`. Favor focused unit tests and Spring slice tests (`@DataJpaTest`, `@SpringBootTest`) over full-stack starts. Provide representative MQTT payload samples in `src/test/resources`. Before every pull request, run `./gradlew test`; integration tests that rely on Docker services should guard themselves behind an explicit profile so they do not block the default test task.

## Commit & Pull Request Guidelines
Write imperative, present-tense commit subjects under 72 characters and group related changes together (e.g., `service: handle offline device alerts`). Use descriptive bodies for context and reference issue IDs where relevant. Pull requests must outline the change, list manual verification (commands or API calls), and include screenshots for Grafana dashboard updates. Call out configuration changes to `ops/` or `mosquitto/`, request reviews from the owning subsystem, and wait for CI to pass before merging.
