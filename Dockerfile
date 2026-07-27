FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/eventmanagementt-1.0.0.jar app.jar

USER appuser
EXPOSE 10000
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=50.0", "-XX:MaxMetaspaceSize=128m", "-Xss512k", "-XX:ReservedCodeCacheSize=64m", "-jar", "/app/app.jar"]
