# ===== build =====
FROM gradle:8.10.2-jdk17-alpine AS build
WORKDIR /app
COPY build.gradle* settings.gradle* gradle* ./
COPY src ./src
RUN gradle clean bootJar -x test --no-daemon

# ===== run =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
