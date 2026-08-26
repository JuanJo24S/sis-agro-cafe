# Roadmap — Sistema de Gestión Agronómica y Financiera para Caficultores

Plan de trabajo derivado de `PA_G1_Equipo1_Entrega1P1.pdf` y de los contratos
OpenAPI en [`openapi/`](openapi/). Cada fila indica qué queda **listo en el
repositorio** y qué falta por hacer.

---

## 0. Arquitectura acordada

Cinco proyectos **independientes** (cada uno con su propio `build.gradle`,
su wrapper de Gradle, su `Dockerfile` y su jar ejecutable con Tomcat embebido).
No hay servidor de aplicaciones externo: cada microservicio *es* su propio servidor.

| Componente | Puerto | Base de datos | Rol en Kafka |
|---|---|---|---|
| `gateway-graphql` | 8080 | — | — |
| `ms-auth` | 8081 | `agrocafe_auth` | — |
| `ms-agronomico` | 8082 | `agrocafe_agronomico` | productor (`costo-registrado`) |
| `ms-cosecha` | 8083 | `agrocafe_cosecha` | productor (`ingreso-registrado`, `costo-registrado`) |
| `ms-financiero` | 8084 | `agrocafe_financiero` | consumidor de ambos tópicos |

Stack: Java 17 · Spring Boot 4.1.1 · PostgreSQL 16 · Apache Kafka 4.1 (KRaft) ·
Flyway · springdoc-openapi 3 · Spring for GraphQL · Docker Compose.

---

## 1. Decisiones pendientes del documento, ya resueltas

El documento (sección 8) y los contratos OpenAPI dejaban seis decisiones abiertas.
Estas son las resoluciones aplicadas en el código, con su justificación:

| # | Decisión | Resolución | Por qué |
|---|---|---|---|
| **D1** | ¿Recordatorio de "fertilización próxima" en el MVP? | **Fase 2.** Sí se implementa el registro de fertilizaciones `PROGRAMADA` (RF04), pero no el servicio simulador que emite avisos. | Mantiene un solo dominio de eventos (financiero) y evita un microservicio extra sin valor evaluable. |
| **D2** | ¿Base por microservicio o instancia única con esquemas? | **Una instancia PostgreSQL con una base de datos por microservicio.** | Conserva el aislamiento del patrón *database per service* (ningún servicio puede leer las tablas de otro) sin multiplicar contenedores en Docker Compose. Migrar luego a instancias separadas es cambiar una URL. |
| **D3** | ¿Gateway propio en Spring Boot o gateway externo? | **Spring Boot con `spring-boot-starter-graphql`**, tal como recomienda el documento. | Todo el stack queda en Java; el BFF puede componer respuestas (lote + labores + balance) que un gateway genérico no compone. |
| **D4** | ¿El pago a recolectores es un costo del balance? | **Sí.** `ms-cosecha` publica `CostoRegistrado` al registrar una recolección. | RF05 pide registrar ese pago y RF07 pide "costos vs. ingresos": omitirlo dejaría el balance sistemáticamente optimista. Es reversible con `AGROCAFE_PUBLICAR_COSTO_RECOLECCION=false`. |
| **D5** | ¿Los registros son inmutables o se pueden anular? | **Anulación lógica con evento compensatorio.** `DELETE` marca el registro como anulado y publica el mismo evento con `operacion: ANULACION`. | Un error de digitación no debe obligar a rehacer la finca, y el balance nunca queda desincronizado. El histórico se conserva. |
| **D6** | ¿La variedad de café es texto libre o catálogo? | **Texto libre** (`VARCHAR(80)`). | Suficiente para el MVP; convertirlo en catálogo es una migración Flyway sin cambio de arquitectura. |

---

## 2. Fases

### Fase 0 — Cimientos ✅ *(listo)*
- [x] Repositorio reorganizado: cinco proyectos independientes bajo `services/`.
- [x] Contratos OpenAPI de los cuatro microservicios (`openapi/`).
- [x] `docker-compose.yml` con PostgreSQL, Kafka (KRaft), los cuatro MS y el gateway.
- [x] Migraciones Flyway por servicio; ningún servicio crea el esquema por reflexión.
- [x] Manejo de errores uniforme en `application/problem+json` (RFC 9457).

### Fase 1 — Identidad (RF01, RNF02) ✅ *(listo)*
- [x] Registro con contraseña BCrypt y correo único.
- [x] Login con emisión de JWT HS256 (claims `sub`, `email`, `nombre`, `rol`).
- [x] Validación **local** del JWT en los otros cuatro servicios (secreto compartido).
- [x] Perfil (`/usuarios/me`), cambio de contraseña y baja lógica de la cuenta.
- [ ] Pendiente (fase 2): rotación de secreto / refresh token.

### Fase 2 — Gestión agronómica (RF02, RF03, RF04) ✅ *(listo)*
- [x] Fincas y lotes con caracterización técnica completa.
- [x] Autorización por propiedad: un caficultor solo ve sus fincas (`403` vs `404`).
- [x] Bitácora de labores con costo de mano de obra e insumos.
- [x] Fertilizaciones `PROGRAMADA` / `EJECUTADA` / `CANCELADA` y transición explícita.
- [x] Publicación de `CostoRegistrado` **después del commit** de la transacción.
- [x] Soporte de `Idempotency-Key` para no duplicar costos en reintentos.

### Fase 3 — Cosecha y post-cosecha (RF05, RF06) ✅ *(listo)*
- [x] Jornadas de recolección con costo calculado (`peso × valor`).
- [x] Máquina de estados del grano: `CEREZA → MOJADO → SECO_AGUA → SECO_TRILLA → ALMACENADO`, sin retrocesos (`409`).
- [x] Ventas con monto calculado y publicación de `IngresoRegistrado`.
- [x] Validación del lote contra `ms-agronomico` vía REST propagando el JWT del usuario.

### Fase 4 — Analítica financiera (RF07, RNF01) ✅ *(listo)*
- [x] Consumidor Kafka de `costo-registrado` e `ingreso-registrado`.
- [x] Consumo **idempotente** (tabla `eventos_procesados`) — Kafka entrega "al menos una vez".
- [x] Balance por lote y agregado por finca.
- [x] Endpoints batch (`?ids=`) para que el gateway no caiga en N+1.

### Fase 5 — Gateway GraphQL (RNF03) ✅ *(listo)*
- [x] Esquema único con `Query` y `Mutation` sobre los cuatro servicios.
- [x] Campos anidados: `finca → lotes → labores / fertilizaciones / recolecciones / ventas / balance`.
- [x] `@BatchMapping` de `Lote.balance` contra el endpoint batch de `ms-financiero`.
- [x] Propagación del JWT del cliente hacia cada microservicio.
- [x] GraphiQL habilitado en `/graphiql`.

### Fase 6 — Empaquetado (RNF04) ✅ *(listo)*
- [x] Un `Dockerfile` multi-etapa por servicio (build con Gradle + runtime JRE, usuario no root).
- [x] `HEALTHCHECK` contra Actuator y `depends_on: service_healthy` en cadena.
- [x] `docker compose up --build` levanta el sistema completo.

### Fase 7 — Lo que sigue 🔜 *(sin hacer)*
- [x] Pruebas unitarias de la lógica de negocio (costos, máquina de estados del grano, acumulación e idempotencia del balance).
- [ ] **Pruebas de integración**: `@SpringBootTest` con Testcontainers (PostgreSQL + Kafka) y pruebas de contrato contra los YAML de `openapi/`.
- [ ] **Datos de demo**: migración `V2__datos_demo.sql` o script `seed.sh` con una finca completa para la sustentación.
- [ ] **Observabilidad**: `/actuator/prometheus`, correlación de trazas entre gateway y MS.
- [ ] **Resiliencia**: reintentos y DLT (`dead letter topic`) en el consumidor de `ms-financiero`.
- [ ] **CI**: workflow de GitHub Actions que compile los cinco proyectos en cada PR.
- [ ] Fases futuras del documento: notificaciones, app móvil, predicción de rendimiento, multi-finca.

---

## 3. Reparto sugerido por integrante

| Integrante | Componente | Qué le queda por delante |
|---|---|---|
| Julian A. Montero Trujillo | `ms-agronomico` | Pruebas de labores/fertilizaciones, catálogo de variedades (D6). |
| Brayan S. Aya Salinas | `ms-cosecha` | Pruebas de la máquina de estados del grano, reporte de rendimiento kg → arrobas. |
| Arlen J. Perdomo Rojas | `ms-financiero` | DLT y reintentos, balance por rango de fechas. |
| Cristian C. Torres Cleves | `ms-auth` + seguridad | Refresh token, pruebas de autorización cruzada entre usuarios. |
| María J. Ospina Polania | `gateway-graphql` | Consultas de sustentación, paginación GraphQL, datos de demo. |

---

## 4. Flujo que demuestra el sistema completo

1. `mutation registro` + `mutation login` → JWT (`ms-auth`).
2. `mutation crearFinca` → `mutation crearLote` (`ms-agronomico`).
3. `mutation registrarLabor` → `ms-agronomico` publica `CostoRegistrado`.
4. `mutation registrarVenta` → `ms-cosecha` publica `IngresoRegistrado`.
5. `ms-financiero` consume ambos en segundo plano y actualiza el balance (RNF01).
6. `query { lote(id:"…") { nombre labores { costoTotal } balance { balance } } }`
   → una sola petición que agrega datos de los tres servicios (RNF03).
