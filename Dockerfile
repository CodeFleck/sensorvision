FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts /workspace/
COPY gradle /workspace/gradle
COPY src /workspace/src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/sensorvision-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
