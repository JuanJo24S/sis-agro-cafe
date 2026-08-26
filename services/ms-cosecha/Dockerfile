# Imagen de ms-cosecha.
# Build multi-etapa: se compila con Gradle y se ejecuta sobre un JRE sin JDK.
# Las dependencias se descargan en una capa propia para que un cambio de codigo
# no obligue a volver a bajarlas.

# --- Etapa de build ---
FROM gradle:9.5.1-jdk17 AS build
ENV GRADLE_USER_HOME=/gradle-cache
WORKDIR /workspace

COPY settings.gradle build.gradle ./
RUN gradle descargarDependencias --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon -x test \
 && cp build/libs/*.jar /workspace/app.jar

# --- Etapa de ejecucion ---
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system agrocafe \
 && useradd --system --gid agrocafe agrocafe
COPY --from=build /workspace/app.jar app.jar
USER agrocafe
EXPOSE 8083
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
  CMD curl -fsS http://localhost:8083/actuator/health | grep -q '"status":"UP"' || exit 1
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
