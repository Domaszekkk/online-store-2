# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY model model
COPY domain domain
COPY public public
COPY adapter adapter
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/adapter/target/cart-adapter-0.0.1-SNAPSHOT.jar app.jar
RUN useradd --system --no-create-home --uid 10001 shop
USER shop
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -fs http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
