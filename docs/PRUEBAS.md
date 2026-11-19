# Matriz de Pruebas — AgroCafé (`sis-agro-cafe`)

Registro de las pruebas efectivamente ejecutadas sobre el sistema completo (pruebas unitarias con Gradle, prueba de humo E2E y prueba de integración del Gateway GraphQL sobre Docker Compose).

- **Fecha de ejecución:** 2026-11-18
- **Entorno:** Docker Desktop (Compose v5.7.0), OpenJDK 25 / contenedor Java 17 (`eclipse-temurin:17-jre`), PostgreSQL 16, Apache Kafka 4.1.2 (KRaft).

---

## 1. Pruebas unitarias (`./gradlew test`)

Ejecutadas en los 5 servicios con `--rerun-tasks --console=plain`. Salida completa en [`docs/evidencias/pruebas-unitarias.txt`](evidencias/pruebas-unitarias.txt).

| ID | Servicio | Suites ejecutadas | Caso / Alcance | Resultado esperado | Resultado obtenido | Estado | Evidencia |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PU-01 | `ms-auth` | `UsuarioTest` (2), `UsuarioServiceTest` (5) | Validación de dominio de usuario, registro, duplicados, login y cambio de contraseña | 7 pruebas exitosas (`exit=0`) | 7 pruebas ejecutadas, 0 fallos, 0 errores | **PASS** | [`pruebas-unitarias.txt`](evidencias/pruebas-unitarias.txt) |
| PU-02 | `ms-agronomico` | `FertilizacionTest` (3), `LaborTest` (2) | Cálculo de costos de labores y ciclo de vida de fertilizaciones | 5 pruebas exitosas (`exit=0`) | 5 pruebas ejecutadas, 0 fallos, 0 errores | **PASS** | [`pruebas-unitarias.txt`](evidencias/pruebas-unitarias.txt) |
| PU-03 | `ms-cosecha` | `EstadoGranoTest` (3), `VentaTest` (3) | Máquina de estados del grano y cálculo de montos en ventas | 6 pruebas exitosas (`exit=0`) | 6 pruebas ejecutadas, 0 fallos, 0 errores | **PASS** | [`pruebas-unitarias.txt`](evidencias/pruebas-unitarias.txt) |
| PU-04 | `ms-financiero` | `BalanceServiceTest` (4) | Consolidación de costos/ingresos, anulaciones e idempotencia por `eventoId` | 4 pruebas exitosas (`exit=0`) | 4 pruebas ejecutadas, 0 fallos, 0 errores | **PASS** | [`pruebas-unitarias.txt`](evidencias/pruebas-unitarias.txt) |
| PU-05 | `gateway-graphql` | `PropagacionTokenTest` (2), `ManejadorErroresGraphQLTest` (8), `AuthGraphQlControllerTest` (4) | Propagación de JWT, clasificación de errores HTTP → GraphQL y resolvers de auth | 14 pruebas exitosas (`exit=0`) | 14 pruebas ejecutadas, 0 fallos, 0 errores (verificado también tras correcciones del gateway) | **PASS** | [`pruebas-unitarias.txt`](evidencias/pruebas-unitarias.txt) |

**Total unitarias:** 36/36 pruebas aprobadas (10 suites, 0 fallos, 0 omitidas).

---

## 2. Pruebas de integración y flujo E2E (Docker Compose)

Ejecutadas tras `cp .env.example .env && docker compose up -d --build --wait` con los 13 contenedores en estado `Up` / `healthy`.

| ID | Prueba / Script | Caso / Flujo verificado | Resultado esperado | Resultado obtenido | Estado | Evidencia |
| --- | --- | --- | --- | --- | --- | --- |
| PI-01 | `docker compose up --build` | Arranque completo de los 13 contenedores y healthchecks | Todos los servicios `Up` / `healthy` | 13/13 contenedores arriba y saludables | **PASS** | Capturas y logs de ejecución |
| PI-02 | `./scripts/prueba-humo.sh` | Flujo E2E: registro → login → finca → lote → labor (`225000`) → fertilización programada/ejecutada (`1490000`) → recolección (`800000`, `SECO_TRILLA`) → venta (`11875000`) → balance Kafka (`costoTotal=2515000`, `ingresoTotal=11875000`, `balance=9360000`) → consulta GraphQL unificada | `exit=0`, balance exacto y consulta GraphQL sin errores | `exit=0`, balance consolidado exacto (`9360000.00`) y respuesta GraphQL completa sin errores | **PASS** | [`prueba-humo.txt`](evidencias/prueba-humo.txt) |
| PI-03 | `./scripts/prueba-gateway.sh` | Introspección (8 queries, 13 mutations), identidad (`registro`, `login`, error de clave, `me`, `UNAUTHORIZED`), finca y 2 lotes, labores (registro, `Idempotency-Key`, anulación), fertilización (programar/ejecutar), cosecha (recolección, avance a `SECO_TRILLA`, rechazo de retroceso a `CEREZA`), ventas (registro y anulación compensada en Kafka), balance lote/finca, consulta unificada de 4 servicios, filtro por estado, `fincas`, `lotes(ids)` batch con lote sin movimientos, `ventas`, `NOT_FOUND` y aislamiento entre usuarios | 38 comprobaciones aprobadas (`exit=0`) | 38/38 comprobaciones aprobadas (`exit=0`) | **PASS** | [`prueba-gateway.txt`](evidencias/prueba-gateway.txt) |

---

## 3. Verificación visual y de observabilidad

| ID | Componente | URL verificada | Resultado esperado | Resultado obtenido | Estado | Evidencia |
| --- | --- | --- | --- | --- | --- | --- |
| PV-01 | Frontend React (Nginx) | `http://localhost:15173` | Carga SPA con logos, formulario de acceso y opción demo | Interfaz renderizada correctamente con logos e identidad visual | **PASS** | [`captura-frontend.png`](evidencias/captura-frontend.png) |
| PV-02 | Jaeger UI | `http://localhost:18686/search?service=gateway-graphql` | Trazas OTLP recibidas de los 5 servicios | 5 servicios registrados y 100 trazas visibles en `gateway-graphql` | **PASS** | [`captura-jaeger.png`](evidencias/captura-jaeger.png) |
| PV-03 | Grafana | `http://localhost:13000/login` | Servicio Grafana v12.1.1 accesible | Pantalla de acceso de Grafana v12.1.1 cargada correctamente | **PASS** | [`captura-grafana.png`](evidencias/captura-grafana.png) |
| PV-04 | Kafka UI | `http://localhost:18085/ui/clusters/agrocafe/all-topics` | Cluster `agrocafe` activo con tópicos de costos/ingresos y DLTs | 6 tópicos visibles (`costo-registrado`: 14 msgs, `ingreso-registrado`: 4, `costo-anulado`: 2, `ingreso-anulado`: 1, DLTs: 0) | **PASS** | [`captura-kafka-ui.png`](evidencias/captura-kafka-ui.png) |

---

## 4. Fallos detectados y corregidos durante las pruebas

1. **Tamaño de página por defecto incompatible en `gateway-graphql`:** en la primera ejecución de `prueba-humo.sh`, los campos anidados `lote.labores`, `lote.fertilizaciones` y `lote.ventas` respondieron `BAD_REQUEST` porque `schema.graphqls` definía `size: Int = 50` mientras los microservicios imponen `@Max(20)`. Se corrigió el valor por defecto a `size: Int = 20` en `services/gateway-graphql/src/main/resources/graphql/schema.graphqls` (y copias en `docs/openapi/gateway.graphqls` y `openapi/gateway.graphqls`).
2. **Soporte de HTTP `PATCH` en `gateway-graphql`:** en la primera ejecución de `prueba-gateway.sh`, `actualizarEstadoGrano` falló con `INTERNAL_ERROR` porque `ClientesConfig` usaba `SimpleClientHttpRequestFactory` (`HttpURLConnection` no soporta `PATCH`). Se reemplazó por `JdkClientHttpRequestFactory` (`java.net.http.HttpClient`) preservando los timeouts explícitos de conexión y lectura.
3. **Script `scripts/prueba-gateway.sh`:** se actualizó la aserción de introspección de `12` a `13` mutations (para reflejar `cambiarPassword`, ya presente en el esquema) y se hizo portable la función `uuid()` para Windows/Git Bash.
