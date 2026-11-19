# AgroCafé (`sis-agro-cafe`)

Sistema distribuido orientado a eventos para la gestión agronómica, de cosecha y financiera de fincas cafeteras.

Proyecto de aula de **Programación Avanzada**, Ingeniería de Software, Fundación Escuela Tecnológica de Neiva *Jesús Oviedo Pérez* (FET).

- **Repositorio:** <https://github.com/JuanJo24S/sis-agro-cafe>
- **Etiqueta de entrega:** `v1.0.0-entrega-final`
- **Commit:** `<PENDIENTE_HASH>`
- **Documentación adicional:** [Manual Técnico](docs/MANUAL_TECNICO.md) · [Matriz de Pruebas](docs/PRUEBAS.md) · [Diagramas](docs/diagramas/) · [Contratos OpenAPI y GraphQL](docs/openapi/)

---

## Equipo

- **Integrantes:**
  - Julian Alfonso Montero Trujillo
  - Brayan Steven Aya Salinas
  - Arlen José Perdomo Rojas
  - Cristian Camilo Torres Cleves
  - María José Ospina Polania
  - Juan José Valbuena Camacho
- **Asesor:** Diego F. Vargas Ramírez

---

## Tecnologías y versiones

| Capa | Tecnología | Versión |
| --- | --- | --- |
| Lenguaje backend | Java (Eclipse Temurin JRE / JDK) | 17 |
| Framework backend | Spring Boot (WebMVC, Security, OAuth2 Resource Server, Data JPA, Kafka, GraphQL, Actuator, OpenTelemetry) | 4.1.1 |
| Compilación backend | Gradle (Wrapper incluido en los 5 servicios) | 9.5.1 |
| Documentación REST | springdoc-openapi (Swagger UI) | 3.1.0 |
| Base de datos | PostgreSQL (`postgres:16-alpine`) + Flyway | 16 |
| Mensajería asíncrona | Apache Kafka (`apache/kafka:4.1.2`, modo KRaft sin ZooKeeper) | 4.1.2 |
| UI de Kafka | Kafbat Kafka UI (`kafbat/kafka-ui:v1.3.0`) | 1.3.0 |
| Observabilidad | Jaeger (`2.10.0`), Prometheus (`v3.5.0`), Grafana (`12.1.1`), Kafka Exporter (`v1.9.0`) | Ver columna |
| Frontend | React `19.3.0`, TypeScript `7.0.2`, Vite `8.3.4`, TanStack Query `5.104.1`, React Router `7.18.4`, Lucide React `1.53.0` | Ver columna |
| Servidor web frontend | Node (`22-alpine` para build) + Nginx (`1.27-alpine` para runtime y proxy inverso) | 22 / 1.27 |
| Orquestación | Docker + Docker Compose | Compose v2/v5 |

---

## Arquitectura

```text
                    Cliente (Navegador / SPA React :15173)
                                  │  Nginx (/graphql y /ms/<servicio>/)
                          ┌───────▼────────┐
                          │ gateway-graphql│  BFF GraphQL :18080
                          └───┬───┬───┬───┬┘
                       REST   │   │   │   │
             ┌────────────────┘   │   │   └────────────────┐
        ┌────▼────┐      ┌────────▼──┐│ ┌──▼────────┐     ┌▼─────────────┐
        │ ms-auth │      │ms-agronom.│└─►ms-cosecha │     │ms-financiero │
        │  :18081 │      │  :18082   │  │  :18083   │     │   :18084     │
        └────┬────┘      └─────┬─────┘  └─────┬─────┘     └──────▲───────┘
             │                 │ CostoRegistrado / IngresoRegistrado     │
             │                 └────────┬─────────┘                      │
             │                     ┌────▼──────────────┐     consume     │
             │                     │   Apache Kafka    ├─────────────────┘
             │                     │  (KRaft :19092)   │
             │                     └───────────────────┘
        ┌────▼───────────────────────────────────────────────────┐
        │  PostgreSQL 16 (:15432) — 4 BD lógicas independientes  │
        │  agrocafe_auth | agrocafe_agronomico | _cosecha | _fin │
        └────────────────────────────────────────────────────────┘
```

- **Síncrono:** el frontend consulta `/graphql` contra `gateway-graphql` (BFF), que traduce a REST contra los 4 microservicios y propaga el JWT. `ms-cosecha` valida la propiedad del lote contra `ms-agronomico`.
- **Asíncrono:** `ms-agronomico` y `ms-cosecha` publican eventos de costos e ingresos (y sus anulaciones compensatorias) en Kafka; `ms-financiero` los consume de manera idempotente (`eventos_procesados`) para consolidar balances por lote y por finca.
- **Seguridad:** `ms-auth` emite JWT firmados con **HS256**; los demás microservicios validan la firma localmente con `JWT_SECRET`.
- **Observabilidad:** trazas OTLP/HTTP en Jaeger, métricas `/actuator/prometheus` y `kafka-exporter` en Prometheus, y tablero preconfigurado en Grafana.

---

## Requisitos previos

- **Docker** y **Docker Compose** (con al menos 4 GB de RAM asignados al motor Docker).
- **Conexión a internet** en el primer arranque para descargar imágenes y dependencias externas.
- **Opcional (para scripts y desarrollo local):**
  - `bash`, `curl` y `jq` para ejecutar `scripts/prueba-humo.sh` y `scripts/prueba-gateway.sh` (en Windows también existe `scripts/prueba-humo.ps1`).
  - **JDK 17+** si se desea ejecutar `./gradlew test` o `./gradlew bootRun` fuera de Docker.
  - **Node.js 22+** si se desea ejecutar el frontend en modo desarrollo (`npm run dev`).

### Dependencias externas

Durante la construcción inicial de las imágenes Docker se descargan artefactos desde:

- **Docker Hub:** `postgres:16-alpine`, `apache/kafka:4.1.2`, `kafbat/kafka-ui:v1.3.0`, `danielqsj/kafka-exporter:v1.9.0`, `prom/prometheus:v3.5.0`, `grafana/grafana:12.1.1`, `jaegertracing/jaeger:2.10.0`, `gradle:9.5.1-jdk17`, `eclipse-temurin:17-jre`, `node:22-alpine`, `nginx:1.27-alpine`.
- **Maven Central:** dependencias Java/Spring Boot resueltas por Gradle dentro de los Dockerfiles y en `./gradlew test`.
- **npm registry:** paquetes del frontend instalados con `npm ci` usando `frontend/package-lock.json`.

---

## Instalación y ejecución

```bash
git clone https://github.com/JuanJo24S/sis-agro-cafe.git
cd sis-agro-cafe
cp .env.example .env      # En producción, reemplazar JWT_SECRET por un secreto aleatorio (>= 32 chars)
docker compose up -d --build
```

Esperar a que todos los contenedores estén en estado `Up` / `healthy`:

```bash
docker compose ps
```

> La primera compilación tarda varios minutos porque cada microservicio descarga dependencias de Maven Central y compila su JAR dentro del contenedor. Las siguientes reconstrucciones reutilizan la caché de capas de Docker.

Para detener el sistema:

```bash
docker compose down       # añadir -v para eliminar también los volúmenes de PostgreSQL y Grafana
```

### Orden de arranque

Orquestado automáticamente por `docker-compose.yml` mediante `depends_on` y `healthcheck`:

1. **Infraestructura base:** `postgres`, `kafka`, `prometheus`, `jaeger`.
2. **Herramientas de infraestructura:** `kafka-ui`, `kafka-exporter` (tras `kafka` healthy) y `grafana` (tras `prometheus` healthy y `jaeger` iniciado).
3. **Microservicios núcleo:** `ms-auth`, `ms-agronomico`, `ms-financiero` (tras `postgres` y `kafka` healthy).
4. **Microservicio dependiente:** `ms-cosecha` (tras `postgres`, `kafka` y `ms-agronomico` healthy).
5. **Gateway BFF:** `gateway-graphql` (tras los 4 microservicios healthy).
6. **Frontend SPA:** `frontend` (tras `gateway-graphql` healthy).

---

## Puertos expuestos

Los puertos del host se configuran en `.env` (valores por defecto de `.env.example` en rango alto para evitar colisiones):

| Servicio | Variable `.env` | Puerto host | Puerto interno | URL / Acceso |
| --- | --- | --- | --- | --- |
| Frontend (React + Nginx) | `PUERTO_FRONTEND` | `15173` | `80` | <http://localhost:15173> |
| Gateway GraphQL (GraphiQL) | `PUERTO_GATEWAY` | `18080` | `8080` | <http://localhost:18080/graphiql> |
| `ms-auth` (Swagger UI) | `PUERTO_AUTH` | `18081` | `8081` | <http://localhost:18081/swagger-ui.html> |
| `ms-agronomico` (Swagger UI) | `PUERTO_AGRONOMICO` | `18082` | `8082` | <http://localhost:18082/swagger-ui.html> |
| `ms-cosecha` (Swagger UI) | `PUERTO_COSECHA` | `18083` | `8083` | <http://localhost:18083/swagger-ui.html> |
| `ms-financiero` (Swagger UI) | `PUERTO_FINANCIERO` | `18084` | `8084` | <http://localhost:18084/swagger-ui.html> |
| Kafka UI | `PUERTO_KAFKA_UI` | `18085` | `8080` | <http://localhost:18085> |
| PostgreSQL 16 | `PUERTO_POSTGRES` | `15432` | `5432` | `localhost:15432` (credenciales en `.env`) |
| Apache Kafka (externo) | `PUERTO_KAFKA` | `19092` | `29092` | `localhost:19092` |
| Jaeger UI | `PUERTO_JAEGER` | `18686` | `16686` | <http://localhost:18686> |
| Jaeger OTLP/HTTP | `PUERTO_OTLP_HTTP` | `14318` | `4318` | `http://localhost:14318/v1/traces` |
| Prometheus | `PUERTO_PROMETHEUS` | `19090` | `9090` | <http://localhost:19090/targets> |
| Grafana (`admin`/`admin`) | `PUERTO_GRAFANA` | `13000` | `3000` | <http://localhost:13000> |

---

## Verificación y pruebas

Con el stack levantado y saludable:

```bash
# Prueba de humo E2E (REST + Kafka + GraphQL)
./scripts/prueba-humo.sh

# Prueba de integración completa del Gateway GraphQL
./scripts/prueba-gateway.sh
```

Pruebas unitarias por servicio (requiere JDK 17+):

```bash
cd services/ms-auth && ./gradlew test
cd ../ms-agronomico && ./gradlew test
cd ../ms-cosecha && ./gradlew test
cd ../ms-financiero && ./gradlew test
cd ../gateway-graphql && ./gradlew test
```

Los resultados y evidencias reales de ejecución están documentados en [docs/PRUEBAS.md](docs/PRUEBAS.md) y `docs/evidencias/`.

---

## Estructura del repositorio

```text
sis-agro-cafe/
├── docs/
│   ├── diagramas/             # Diagramas de arquitectura, flujo y modelo de datos (.drawio)
│   ├── evidencias/            # Salidas de pruebas y capturas reales
│   ├── openapi/               # Especificaciones OpenAPI y esquema GraphQL
│   ├── MANUAL_TECNICO.md      # Manual técnico del sistema
│   └── PRUEBAS.md             # Matriz de pruebas ejecutadas
├── frontend/                  # SPA React 19 + TypeScript + Vite + Nginx
├── infra/                     # Provisioning Grafana, init PostgreSQL, Prometheus
├── openapi/                   # Contratos OpenAPI y esquema GraphQL
├── scripts/                   # Scripts de prueba de humo y verificación del gateway
├── services/                  # 5 servicios Spring Boot 4.1.1 / Gradle 9.5.1
│   ├── gateway-graphql/
│   ├── ms-auth/
│   ├── ms-agronomico/
│   ├── ms-cosecha/
│   └── ms-financiero/
├── docker-compose.yml
└── .env.example
```

---

## Limitaciones

- **Cobertura parcial del Gateway GraphQL:** operaciones como edición de finca/lote (`PUT`), anulación de fertilización/recolección (`DELETE`), gestión de perfil/estado de usuario y listados masivos de balances se consumen vía REST directo (el frontend los enruta mediante el proxy Nginx `/ms/<servicio>/`).
- **DLT explícita solo en 2 tópicos:** `ms-financiero` declara explícitamente `costo-registrado.DLT` e `ingreso-registrado.DLT`; los tópicos de anulación (`costo-anulado`, `ingreso-anulado`) no tienen bean `NewTopic` DLT dedicado.
- **Entorno académico/local:** sin TLS/HTTPS interno, sin CORS en los microservicios (resuelto vía proxy Nginx) y sin refresh tokens ni revocación de JWT. Ver detalle completo en [docs/MANUAL_TECNICO.md](docs/MANUAL_TECNICO.md#8-limitaciones-conocidas).
