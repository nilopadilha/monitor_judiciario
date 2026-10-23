# syntax=docker/dockerfile:1.7
# Build multi-stage da API. Contexto: raiz do repositório.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY backend/ ./
RUN --mount=type=cache,target=/root/.gradle ./gradlew :apps:api:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar apps/api/build/libs/monitor-judicial-api.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
