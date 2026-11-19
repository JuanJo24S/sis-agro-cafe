# Manual Técnico — AgroCafé (`sis-agro-cafe`)

Sistema distribuido orientado a eventos para la gestión agronómica, de cosecha y financiera de fincas cafeteras.

- **Asignatura:** Programación Avanzada — Ingeniería de Software, Fundación Escuela Tecnológica de Neiva *Jesús Oviedo Pérez* (FET)
- **Asesor:** Diego F. Vargas Ramírez
- **Equipo:** Julian Alfonso Montero Trujillo, Brayan Steven Aya Salinas, Arlen José Perdomo Rojas, Cristian Camilo Torres Cleves, María José Ospina Polania, Juan José Valbuena Camacho
- **Repositorio:** <https://github.com/JuanJo24S/sis-agro-cafe>

---

## 1. Componentes y responsabilidades

El sistema se organiza en **5 servicios Spring Boot 4.1.1 (Java 17)**, un **frontend SPA en React 19**, **infraestructura de datos y mensajería** (PostgreSQL 16 y Apache Kafka 4.1.2 en modo KRaft) y un **stack de observabilidad** (Jaeger 2.10.0, Prometheus 3.5.0, Grafana 12.1.1, Kafka Exporter 1.9.0 y Kafka UI 1.3.0).

| Componente | Contenedor | Puerto interno | Puerto host (`.env.example`) | Responsabilidad |
| --- | --- | --- | --- | --- |
| `frontend` | `agrocafe-frontend` | `80` | `15173` | SPA React 19 + TypeScript + Vite servida por Nginx 1.27; actúa como proxy inverso hacia `/graphql` y `/ms/<servicio>/`. |
| `gateway-graphql` | `agrocafe-gateway` | `8080` | `18080` | BFF (Backend for Frontend) GraphQL con GraphiQL. Unifica consultas entre microservicios, propaga el token JWT y maneja errores tipados. |
| `ms-auth` | `agrocafe-ms-auth` | `8081` | `18081` | Registro de usuarios, autenticación, emisión de JWT (HS256), consulta/edición de perfil, cambio de contraseña y estado del usuario. |
| `ms-agronomico` | `agrocafe-ms-agronomico` | `8082` | `18082` | Gestión de fincas, lotes (ficha técnica), labores culturales y fertilizaciones (programadas y ejecutadas). Publica eventos de costo en Kafka. |
| `ms-cosecha` | `agrocafe-ms-cosecha` | `8083` | `18083` | Registro de recolecciones con máquina de estados del grano (`CEREZA` → `MOJADO` → `SECO_AGUA` → `SECO_TRILLA` → `ALMACENADO`) y registro/anulación de ventas. Valida propiedad de lotes contra `ms-agronomico` y publica eventos de costo e ingreso en Kafka. |
| `ms-financiero` | `agrocafe-ms-financiero` | `8084` | `18084` | Consumidor Kafka que consolida balances por lote y por finca de forma asíncrona e idempotente (`eventos_procesados`). Expone consultas de balance. |
| `postgres` | `agrocafe-postgres` | `5432` | `15432` | Instancia única de PostgreSQL 16-alpine con 4 bases de datos lógicas aisladas (patrón *database per service*). |
| `kafka` | `agrocafe-kafka` | `9092` / `29092` | `19092` | Broker Apache Kafka 4.1.2 en modo KRaft (sin ZooKeeper) para mensajería asíncrona entre microservicios. |
| `kafka-ui` | `agrocafe-kafka-ui` | `8080` | `18085` | Interfaz web (Kafbat Kafka UI) para inspeccionar tópicos, mensajes, grupos de consumo y DLT. |
| `kafka-exporter` | `agrocafe-kafka-exporter` | `9308` | — (interno) | Exporta métricas de offsets y lag de grupos de consumo de Kafka hacia Prometheus. |
| `prometheus` | `agrocafe-prometheus` | `9090` | `19090` | Recolecta métricas cada 10 s desde `/actuator/prometheus` de los 5 servicios Java y desde `kafka-exporter`. |
| `grafana` | `agrocafe-grafana` | `3000` | `13000` | Visualización de métricas y trazas; aprovisiona automáticamente las fuentes Prometheus y Jaeger y el tablero *AgroCafé → Estado de microservicios*. |
| `jaeger` | `agrocafe-jaeger` | `16686` / `4318` | `18686` / `14318` | Recibe trazas distribuidas OpenTelemetry por OTLP/HTTP (`:4318/v1/traces`) con probabilidad de muestreo `1.0`. |

Diagramas de referencia en `docs/diagramas/`:
- `Diagrama_1_Arquitectura.drawio`
- `Diagrama_2_Flujo.drawio`
- `Diagrama_3_Modelo_de_datos.drawio`

---

## 2. Comunicación entre componentes

### 2.1 Comunicación síncrona (HTTP / GraphQL / REST)
1. **Frontend → Nginx → Gateway / Microservicios:**
   - Las consultas agregadas y la mayoría de mutaciones se envían a `/graphql` (reenviado a `http://gateway-graphql:8080/graphql`).
   - Las operaciones no expuestas por el gateway y las verificaciones de salud pasan por `/ms/{gateway,auth,agronomico,cosecha,financiero}/` (configurado en `frontend/nginx.conf`), evitando problemas de CORS.
2. **Gateway GraphQL → Microservicios (REST):**
   - `gateway-graphql` usa Spring `RestClient` con timeouts configurables (`TIMEOUT_CONEXION=3s`, `TIMEOUT_LECTURA=10s`).
   - Propaga la cabecera `Authorization: Bearer <token>` recibida del cliente mediante `PropagacionToken`.
   - Expone la consulta batch `lotes(ids: [ID!]!)` para mitigar el problema N+1.
3. **`ms-cosecha` → `ms-agronomico` (REST):**
   - Antes de registrar una recolección o venta, `ms-cosecha` consulta `AGROCAFE_URL_AGRONOMICO` (`http://ms-agronomico:8082`) propagando el JWT para verificar que el lote existe, pertenece al usuario autenticado y obtener su `fincaId`.

### 2.2 Comunicación asíncrona (Apache Kafka)
- Cuando se registra o anula un costo o un ingreso, el microservicio productor publica un evento `MovimientoFinancieroEvent` en Kafka con `acks=all`, `retries=5` y `enable.idempotence=true`.
- `ms-financiero` consume los eventos y actualiza la tabla `balances_lote` de forma eventual.
- La trazabilidad distribuida se propaga automáticamente en las cabeceras de Kafka (`spring.kafka.template.observation-enabled=true` y `spring.kafka.listener.observation-enabled=true`), permitiendo ver el flujo completo desde GraphQL hasta `ms-financiero` en Jaeger.

---

## 3. Tópicos Kafka, idempotencia y DLT

Configurados en `services/ms-financiero/src/main/java/com/sisagrodev/financiero/config/KafkaConfig.java` (3 particiones, factor de replicación 1):

| Tópico | Productor(es) | Consumidor | Evento / Trigger |
| --- | --- | --- | --- |
| `costo-registrado` | `ms-agronomico`, `ms-cosecha` | `ms-financiero` | Registro de labor cultural, ejecución de fertilización o registro de recolección. |
| `costo-anulado` | `ms-agronomico`, `ms-cosecha` | `ms-financiero` | Anulación (`DELETE`) de labor, fertilización ejecutada o recolección. |
| `ingreso-registrado` | `ms-cosecha` | `ms-financiero` | Registro de venta de café. |
| `ingreso-anulado` | `ms-cosecha` | `ms-financiero` | Anulación (`DELETE`) de una venta. |
| `costo-registrado.DLT` | `ms-financiero` (`DeadLetterPublishingRecoverer`) | Inspección manual (Kafka UI) | Mensajes fallidos de `costo-registrado` tras 3 reintentos (intervalo fijo de 1 s). |
| `ingreso-registrado.DLT` | `ms-financiero` (`DeadLetterPublishingRecoverer`) | Inspección manual (Kafka UI) | Mensajes fallidos de `ingreso-registrado` tras 3 reintentos (intervalo fijo de 1 s). |

- **Idempotencia en productores (HTTP):** `ms-agronomico` y `ms-cosecha` aceptan la cabecera `Idempotency-Key` (UUID) persistida en la tabla `claves_idempotencia` para evitar duplicados ante reintentos de red.
- **Idempotencia en consumidor (Kafka):** cada evento incluye `eventoId` (UUID). `ms-financiero` inserta el `eventoId` en `eventos_procesados` dentro de la misma transacción en que actualiza `balances_lote`, evitando reprocesamientos duplicados.
- **Manejo de errores:** `DefaultErrorHandler` con `FixedBackOff(1000L, 3L)` y `DeadLetterPublishingRecoverer`.

---

## 4. Bases de datos y migraciones

El script `infra/postgres/init/01-crear-bases.sql` crea 4 bases de datos independientes en la misma instancia PostgreSQL 16. Cada servicio gestiona su esquema con **Flyway** (`spring.jpa.hibernate.ddl-auto=none`, `open-in-view=false`) y pool HikariCP acotado (`maximum-pool-size=6`, `minimum-idle=2`).

| Servicio | Base de datos | Migración Flyway | Tablas principales |
| --- | --- | --- | --- |
| `ms-auth` | `agrocafe_auth` | `V1__crear_usuarios.sql` | `usuarios` (`id`, `nombre`, `email` único, `password_hash`, `rol` ∈ `{CAFICULTOR, ANALISTA_FINANCIERO}`, `estado` ∈ `{ACTIVO, INACTIVO}`, `fecha_creacion`). |
| `ms-agronomico` | `agrocafe_agronomico` | `V1__esquema_agronomico.sql` | `fincas`, `lotes`, `labores` (soft-delete `anulada`), `fertilizaciones` (`PROGRAMADA`, `EJECUTADA`, `CANCELADA`), `claves_idempotencia`. |
| `ms-cosecha` | `agrocafe_cosecha` | `V1__esquema_cosecha.sql` | `recolecciones` (estados del grano + `anulada`), `ventas` (`anulada`), `claves_idempotencia`. |
| `ms-financiero` | `agrocafe_financiero` | `V1__esquema_financiero.sql` | `balances_lote` (con columna `version` para bloqueo optimista `@Version`), `eventos_procesados`. |

---

## 5. API (OpenAPI y GraphQL)

### 5.1 Contratos OpenAPI 3 (REST)
Cada microservicio expone Swagger UI en `/swagger-ui.html` y su especificación en `/v3/api-docs`. Las especificaciones versionadas se encuentran en `docs/openapi/` y `openapi/`:

- **`ms-auth` (`/api/v1`):**
  - `POST /auth/registro`, `POST /auth/login`, `POST /auth/cambiar-password`
  - `GET /usuarios`, `GET /usuarios/me`, `PUT /usuarios/me`, `PATCH /usuarios/me/estado`
- **`ms-agronomico` (`/api/v1`):**
  - `POST /fincas`, `GET /fincas`, `GET /fincas/{fincaId}`, `PUT /fincas/{fincaId}`
  - `POST /fincas/{fincaId}/lotes`, `GET /fincas/{fincaId}/lotes`, `GET /lotes`, `GET /lotes/{loteId}`, `PUT /lotes/{loteId}`
  - `POST /lotes/{loteId}/labores`, `GET /lotes/{loteId}/labores`, `GET /labores/{laborId}`, `DELETE /labores/{laborId}`
  - `POST /lotes/{loteId}/fertilizaciones`, `GET /lotes/{loteId}/fertilizaciones`, `GET /fertilizaciones/{fertilizacionId}`, `POST /fertilizaciones/{fertilizacionId}/ejecucion`, `DELETE /fertilizaciones/{fertilizacionId}`
- **`ms-cosecha` (`/api/v1`):**
  - `POST /lotes/{loteId}/recolecciones`, `GET /lotes/{loteId}/recolecciones`, `GET /recolecciones/{recoleccionId}`, `PATCH /recolecciones/{recoleccionId}/estado`, `DELETE /recolecciones/{recoleccionId}`
  - `POST /ventas`, `GET /ventas`, `GET /ventas/{ventaId}`, `DELETE /ventas/{ventaId}`
- **`ms-financiero` (`/api/v1/balance`):**
  - `GET /balance/lotes/{loteId}`, `GET /balance/lotes`, `GET /balance/fincas/{fincaId}`, `GET /balance/fincas/{fincaId}/lotes`, `GET /balance`

### 5.2 Gateway GraphQL (`gateway-graphql`)
- Endpoint: `POST /graphql` · Interfaz interactiva: `GET /graphiql` · Esquema: `services/gateway-graphql/src/main/resources/graphql/schema.graphqls` (copia en `docs/openapi/gateway.graphqls`).
- **8 Queries:** `me`, `fincas`, `finca`, `lote`, `lotes`, `ventas`, `balanceLote`, `balanceFinca`.
- **13 Mutations:** `registro`, `login`, `cambiarPassword`, `crearFinca`, `crearLote`, `registrarLabor`, `anularLabor`, `registrarFertilizacion`, `ejecutarFertilizacion`, `registrarRecoleccion`, `actualizarEstadoGrano`, `registrarVenta`, `anularVenta`.
- **Errores tipados:** `ManejadorErroresGraphQL` traduce respuestas HTTP de los microservicios a clasificaciones GraphQL (`UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `BAD_REQUEST`, etc.).

---

## 6. Seguridad

- **Autenticación sin estado (stateless):** `SessionCreationPolicy.STATELESS`, CSRF deshabilitado.
- **Hashing de contraseñas:** `BCryptPasswordEncoder` en `ms-auth`.
- **Emisión y validación JWT:** `ms-auth` firma tokens con **HS256** (`NimbusJwtEncoder`) usando `JWT_SECRET` (mínimo 32 bytes; validado al arranque) y expiración configurable (`JWT_EXPIRACION_SEGUNDOS=3600`). Los demás microservicios (`ms-agronomico`, `ms-cosecha`, `ms-financiero`) actúan como **OAuth2 Resource Server** y validan la firma localmente sin consultar a `ms-auth` en cada petición.
- **Rutas públicas:** únicamente `/api/v1/auth/registro`, `/api/v1/auth/login`, `/actuator/**`, `/v3/api-docs/**` y `/swagger-ui/**`. El resto requiere `Authorization: Bearer <JWT>`.
- **Aislamiento de datos:** cada consulta y mutación verifica la propiedad del recurso según el `sub` (UUID del usuario) extraído del JWT, impidiendo el acceso a fincas, lotes o balances de otro usuario.
- **Gestión de secretos:** `.env` está excluido en `.gitignore`; `.env.example` contiene únicamente valores de referencia para entorno local.

---

## 7. Instalación, orden de arranque y uso

1. Copiar variables de entorno: `cp .env.example .env` (reemplazar `JWT_SECRET` por un valor aleatorio de al menos 32 caracteres en despliegues reales).
2. Levantar el stack completo: `docker compose up -d --build`.
3. **Orden de arranque gestionado por `depends_on` y `healthcheck`:**
   1. `postgres`, `kafka`, `prometheus`, `jaeger`
   2. `kafka-ui`, `kafka-exporter`, `grafana` (tras `kafka` / `prometheus`)
   3. `ms-auth`, `ms-agronomico`, `ms-financiero` (tras `postgres` y `kafka` `healthy`)
   4. `ms-cosecha` (tras `postgres`, `kafka` y `ms-agronomico` `healthy`)
   5. `gateway-graphql` (tras los 4 microservicios `healthy`)
   6. `frontend` (tras `gateway-graphql` `healthy`)
4. Verificar estado: `docker compose ps` (todos en `Up` / `healthy`).
5. Ejecutar pruebas automatizadas:
   - Unitarias: `./gradlew test` en cada directorio de `services/`.
   - Humo E2E: `./scripts/prueba-humo.sh` (o `scripts/prueba-humo.ps1` en PowerShell).
   - Gateway GraphQL: `./scripts/prueba-gateway.sh`.
6. Apagar y limpiar: `docker compose down` (añadir `-v` para eliminar volúmenes de PostgreSQL y Grafana).

---

## 8. Limitaciones conocidas

1. **Operaciones REST no expuestas por el Gateway GraphQL:**
   - `ms-auth`: `GET /api/v1/usuarios`, `PUT /api/v1/usuarios/me`, `PATCH /api/v1/usuarios/me/estado`.
   - `ms-agronomico`: `PUT /api/v1/fincas/{fincaId}`, `PUT /api/v1/lotes/{loteId}`, `DELETE /api/v1/fertilizaciones/{fertilizacionId}`.
   - `ms-cosecha`: `DELETE /api/v1/recolecciones/{recoleccionId}`.
   - `ms-financiero`: `GET /api/v1/balance`, `GET /api/v1/balance/lotes`, `GET /api/v1/balance/fincas/{fincaId}/lotes`.
   - El frontend consume estas operaciones directamente vía proxy Nginx (`/ms/<servicio>/...`).
2. **DLT declarada explícitamente solo en 2 tópicos:**
   - En `KafkaConfig` de `ms-financiero`, solo se declaran explícitamente los `NewTopic` `costo-registrado.DLT` e `ingreso-registrado.DLT`. No existen beans `NewTopic` explícitos para `costo-anulado.DLT` ni `ingreso-anulado.DLT` (dependen de `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true` si ocurriera un fallo en esos tópicos).
3. **Sin CORS en los microservicios ni TLS/HTTPS interno:**
   - La comunicación dentro de `agrocafe-net` es HTTP plano y los microservicios no exponen cabeceras CORS; el acceso desde navegador se canaliza a través del proxy inverso de Nginx en `frontend`.
4. **Pendientes de Fase 2 (fuera del alcance de esta entrega):**
   - Exposición completa del resto de operaciones CRUD/anulaciones en `gateway-graphql`.
   - Refresh tokens, revocación de JWT y gestión avanzada de roles/permisos para `ANALISTA_FINANCIERO` con acceso multi-finca.
   - Declaración explícita de `costo-anulado.DLT` e `ingreso-anulado.DLT`, y mecanismo automático de reprocesamiento de DLT.
   - Reportes exportables (PDF/CSV), alertas agronómicas programadas y despliegue productivo con HTTPS y gestor de secretos externo.
