FROM node:20 AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json* frontend/yarn.lock* ./
RUN npm install
COPY frontend .
RUN npm run build
# Verify the dist folder was created
RUN ls -la /frontend/dist && test -f /frontend/dist/index.html

FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts /workspace/
RUN ls -l /workspace
RUN chmod +x gradlew
COPY gradle /workspace/gradle
COPY src /workspace/src
# Copy frontend build output into backend static resources
COPY --from=frontend-build /frontend/dist /workspace/src/main/resources/static
# Verify frontend files were copied
RUN ls -la /workspace/src/main/resources/static && test -f /workspace/src/main/resources/static/index.html
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/sensorvision-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
