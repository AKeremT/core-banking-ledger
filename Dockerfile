# Stage 1: Builder
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

#Docker Layer Caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runner
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/target/*.jar app.jar

# JVM Container Optimisation
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# APP PORT
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]