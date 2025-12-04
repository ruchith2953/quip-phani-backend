# ─────────────────────────────
#  Stage 1 — Build
# ─────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ─────────────────────────────
#  Stage 2 — Runtime
# ─────────────────────────────
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]
