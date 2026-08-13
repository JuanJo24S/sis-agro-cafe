# Dockerfile centralizado, reutilizado por los 4 microservicios y el gateway
# (todos Java / Spring Boot con la misma estructura Maven). El módulo a
# construir se selecciona con el build-arg SERVICE_DIR desde docker-compose.yml.
# Asume pom.xml + src/main/java dentro de cada carpeta de servicio.

ARG SERVICE_DIR

# --- Etapa de build ---
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE_DIR
WORKDIR /app
COPY ${SERVICE_DIR}/pom.xml .
RUN mvn -B dependency:go-offline
COPY ${SERVICE_DIR}/src ./src
RUN mvn -B package -DskipTests

# --- Etapa de ejecución ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
