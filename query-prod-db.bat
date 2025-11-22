@echo off
SET SPRING_PROFILES_ACTIVE=prod
SET DB_URL=jdbc:postgresql://sensorvision-db.cdwaw2eu68gm.us-west-2.rds.amazonaws.com:5432/sensorvision
SET DB_USERNAME=sensorvision
SET DB_PASSWORD=sensorvision123
SET JWT_SECRET=temp-secret-for-query-only-not-real
SET SIMULATOR_ENABLED=false
.\gradlew.bat bootRun