# sis-agro-cafe

Sistema de Gestión Agronómica y Financiera para Caficultores — backend distribuido
orientado a eventos.

Proyecto de **Programación Avanzada**, Ingeniería de Software, Fundación Escuela
Tecnológica de Neiva *Jesús Oviedo Pérez* (FET). Documento base:
`PA_G1_Equipo1_Entrega1P1.pdf`. Plan de trabajo y decisiones de diseño: [`ROADMAP.md`](ROADMAP.md).

---

## Arranque rápido

```bash
cp .env.example .env      # y cambia JWT_SECRET por uno propio
docker compose up --build
```

Un solo comando levanta PostgreSQL, Kafka, los cuatro microservicios y el gateway
(RNF04). La primera vez tarda varios minutos porque cada servicio compila su jar
dentro de la imagen; las siguientes son casi inmediatas gracias a la caché de Gradle.

| Servicio | URL |
|---|---|
| Gateway GraphQL (GraphiQL) | <http://localhost:8080/graphiql> |
| ms-auth (Swagger UI) | <http://localhost:8081/swagger-ui.html> |
| ms-agronomico (Swagger UI) | <http://localhost:8082/swagger-ui.html> |
| ms-cosecha (Swagger UI) | <http://localhost:8083/swagger-ui.html> |
| ms-financiero (Swagger UI) | <http://localhost:8084/swagger-ui.html> |
| PostgreSQL | `localhost:5432` (usuario/clave de `.env`) |
| Kafka (desde el host) | `localhost:29092` |

Para apagar todo: `docker compose down` (añade `-v` para borrar también los datos).

> Si alguno de esos puertos ya está ocupado en tu máquina, cámbialo en `.env`
> (`PUERTO_GATEWAY`, `PUERTO_AUTH`, …) sin tocar `docker-compose.yml`.

---

## Arquitectura

```
                    Cliente (web / app)
                            │  GraphQL
                    ┌───────▼────────┐
                    │ gateway-graphql│  BFF :8080
                    └───┬───┬───┬───┬┘
                REST │   │   │   │
        ┌────────────┘   │   │   └────────────┐
   ┌────▼────┐  ┌────────▼──┐  ┌──▼────────┐  ┌▼─────────────┐
   │ ms-auth │  │ms-agronom.│  │ms-cosecha │  │ms-financiero │
   │  :8081  │  │   :8082   │  │  :8083    │  │    :8084     │
   └────┬────┘  └─────┬─────┘  └─────┬─────┘  └──────▲───────┘
        │             │  CostoRegistrado │ IngresoRegistrado │
        │             └────────┬─────────┘                   │
        │                 ┌────▼──────────────┐   consume    │
        │                 │   Apache Kafka    ├──────────────┘
        │                 └───────────────────┘
   ┌────▼──────────────────────────────────────────────┐
   │  PostgreSQL — una base de datos por microservicio  │
   └────────────────────────────────────────────────────┘
```

- **Síncrono** (cliente → sistema): GraphQL contra el gateway, que traduce a REST.
- **Asíncrono** (entre microservicios): Kafka. El caficultor nunca espera a que se
  recalcule el balance (RNF01).
- **Seguridad**: `ms-auth` firma el JWT (HS256); los demás lo validan **localmente**
  con el secreto compartido, sin llamar a `ms-auth` en cada petición (RNF02).

### Eventos

Ambos tópicos transportan el mismo contrato, `MovimientoFinancieroEvent`:

| Tópico | Publica | Cuándo |
|---|---|---|
| `costo-registrado` | `ms-agronomico` | labor ejecutada, fertilización ejecutada |
| `costo-registrado` | `ms-cosecha` | jornada de recolección (ver decisión D4 del roadmap) |
| `ingreso-registrado` | `ms-cosecha` | venta registrada |

El campo `operacion` distingue `REGISTRO` de `ANULACION` (evento compensatorio), y
`eventoId` permite que `ms-financiero` consuma de forma idempotente.

---

## Estructura del repositorio

```
services/
  ms-auth/            proyecto Gradle independiente (RF01)
  ms-agronomico/      proyecto Gradle independiente (RF02, RF03, RF04)
  ms-cosecha/         proyecto Gradle independiente (RF05, RF06)
  ms-financiero/      proyecto Gradle independiente (RF07)
  gateway-graphql/    proyecto Gradle independiente (RNF03)
openapi/              contratos OpenAPI de los cuatro microservicios
infra/postgres/init/  creación de una base de datos por microservicio
scripts/              prueba de humo del flujo completo
docker-compose.yml    orquestación completa (RNF04)
ROADMAP.md            fases, decisiones de diseño y reparto del equipo
```

Cada carpeta de `services/` es **un proyecto autónomo**: se puede abrir, compilar y
ejecutar sola, sin depender de las demás ni de un proyecto raíz.

---

## Desarrollo sin Docker

Levanta solo la infraestructura y ejecuta el servicio que estés trabajando:

```bash
docker compose up -d postgres kafka
cd services/ms-agronomico
./gradlew bootRun
```

Los valores por defecto de cada `application.yml` ya apuntan a `localhost:5432` y
`localhost:9092`, así que no hace falta configurar nada más. Exporta `JWT_SECRET`
con el mismo valor en todos los servicios que ejecutes a la vez.

Compilar un servicio: `./gradlew build` · Ejecutar pruebas: `./gradlew test`.

---

## Prueba de humo del flujo completo

Con el sistema arriba y `jq` instalado, el script recorre el flujo entero (registro →
login → finca → lote → labor → fertilización → recolección → venta → balance → consulta
GraphQL) y falla si algún paso no responde:

```bash
./scripts/prueba-humo.sh
```

Lo mismo, paso a paso y a mano:

```bash
# 1. Registro y login
curl -s -X POST localhost:8081/api/v1/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Maria Ospina","email":"maria@finca.co","password":"cafe12345","rol":"CAFICULTOR"}'

TOKEN=$(curl -s -X POST localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@finca.co","password":"cafe12345"}' | jq -r .accessToken)

# 2. Finca y lote
FINCA=$(curl -s -X POST localhost:8082/api/v1/fincas -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"La Esperanza","municipio":"Rivera","departamento":"Huila","areaTotalHectareas":4.5}' | jq -r .id)

LOTE=$(curl -s -X POST localhost:8082/api/v1/fincas/$FINCA/lotes -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Lote 3","variedad":"Castillo","fechaSiembra":"2024-03-01","areaHectareas":1.2,"densidadSiembra":5000,"alturaMsnm":1650,"phSuelo":5.4}' | jq -r .id)

# 3. Un costo y un ingreso
curl -s -X POST localhost:8082/api/v1/lotes/$LOTE/labores -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"tipo":"LIMPIA","fechaEjecucion":"2026-02-10","jornales":3,"costoManoObra":180000,"costoInsumos":45000}'

curl -s -X POST localhost:8083/api/v1/ventas -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"loteId\":\"$LOTE\",\"fecha\":\"2026-03-05\",\"cantidad\":12.5,\"precioPorArroba\":950000,\"comprador\":\"Cooperativa del Huila\"}"

# 4. Balance consolidado por Kafka (unos segundos después)
curl -s localhost:8084/api/v1/balance/lotes/$LOTE -H "Authorization: Bearer $TOKEN"
```

Lo mismo, en una sola petición al gateway:

```graphql
query {
  lote(id: "<LOTE>") {
    nombre
    variedad
    labores { tipo costoTotal }
    fertilizaciones { estado composicion costoTotal }
    ventas { comprador montoTotal }
    balance { costoTotal ingresoTotal balance }
  }
}
```

En GraphiQL agrega la cabecera `{"Authorization": "Bearer <TOKEN>"}`.

---

## Stack

Java 17 · Spring Boot 4.1.1 · Spring Security (OAuth2 Resource Server) ·
Spring Data JPA + Flyway · PostgreSQL 16 · Apache Kafka 4.1 (KRaft, sin ZooKeeper) ·
Spring for GraphQL · springdoc-openapi 3 · Gradle 9.5 · Docker Compose.

## Equipo

Julian Alfonso Montero Trujillo · Brayan Steven Aya Salinas ·
Arlen José Perdomo Rojas · Cristian Camilo Torres Cleves ·
María José Ospina Polania

Asesor: Diego F. Vargas Ramírez
