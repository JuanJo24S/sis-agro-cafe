# sis-agro-cafe

Sistema de Gestión Agronómica y Financiera para Caficultores — backend distribuido
orientado a eventos.

Proyecto de **Programación Avanzada**, Ingeniería de Software, Fundación Escuela
Tecnológica de Neiva _Jesús Oviedo Pérez_ (FET). Documento de entrega:
[`PA_G1_Equipo1_Entrega2P2.pdf`](docs/PA_G1_Equipo1_Entrega2P2.pdf).

---

## Arranque rápido

```bash
git clone <URL_DEL_REPOSITORIO>
cd sis-agro-cafe
cp .env.example .env      # y cambia JWT_SECRET por uno propio
docker compose up --build
```

Un solo comando levanta PostgreSQL, Kafka, los cuatro microservicios y el gateway
(RNF04). La primera vez tarda varios minutos porque cada servicio compila su jar
dentro de la imagen; las siguientes son casi inmediatas gracias a la caché de Gradle.

| Servicio                   | URL                                        |
| -------------------------- | ------------------------------------------ |
| **Frontend (React)**       | <http://localhost:15173>                    |
| Gateway GraphQL (GraphiQL) | <http://localhost:18080/graphiql>           |
| ms-auth (Swagger UI)       | <http://localhost:18081/swagger-ui.html>    |
| ms-agronomico (Swagger UI) | <http://localhost:18082/swagger-ui.html>    |
| ms-cosecha (Swagger UI)    | <http://localhost:18083/swagger-ui.html>    |
| ms-financiero (Swagger UI) | <http://localhost:18084/swagger-ui.html>    |
| PostgreSQL                 | `localhost:15432` (usuario/clave de `.env`) |
| Kafka (desde el host)      | `localhost:19092`                           |
| Jaeger (trazas)            | <http://localhost:18686>                   |
| Prometheus (targets)       | <http://localhost:19090/targets>            |
| Grafana (admin/admin)      | <http://localhost:13000>                    |

Para apagar todo: `docker compose down` (añade `-v` para borrar también los datos).

> Si alguno de esos puertos ya está ocupado en tu máquina, cámbialo en `.env`
> (`PUERTO_GATEWAY`, `PUERTO_AUTH`, …) sin tocar `docker-compose.yml`.
> Los valores incluidos en `.env.example` usan un bloque de puertos altos para
> reducir conflictos con otros proyectos. Si alguno también está ocupado, cambia
> la variable `PUERTO_*` correspondiente en `.env`; los scripts de `scripts/` leen
> esos valores automáticamente.

### Trazas con Jaeger

Cada microservicio exporta sus trazas (OpenTelemetry, OTLP/HTTP) a Jaeger. En
En <http://localhost:18686> elige el servicio (`gateway-graphql`, `ms-auth`,
`ms-agronomico`, `ms-cosecha`, `ms-financiero`) y pulsa _Find Traces_: una misma
traza muestra la petición GraphQL, las llamadas REST a cada microservicio, las
consultas y la publicación/consumo en Kafka hasta `ms-financiero`. Un servicio que
no aparece en la lista no ha recibido tráfico o no está arriba.

### Métricas con Prometheus y Grafana

Cada microservicio publica sus métricas en `/actuator/prometheus`; Prometheus las
recolecta cada 10 s junto con las de Kafka (a través de `kafka-exporter`). En
<http://localhost:19090/targets> todos los _targets_ deben aparecer en `UP`.

Grafana arranca con las fuentes **Prometheus** y **Jaeger** ya configuradas y el
tablero **AgroCafe → Estado de microservicios** (estado ARRIBA/CAÍDO de cada
servicio, peticiones/s, latencia p95, errores 5xx, heap de la JVM y Kafka).

---

## Arquitectura

```
                    Cliente (web / app)
                            │  GraphQL
                    ┌───────▼────────┐
                    │ gateway-graphql│  BFF :18080 (host)
                    └───┬───┬───┬───┬┘
                REST │   │   │   │
        ┌────────────┘   │   │   └────────────┐
   ┌────▼────┐  ┌────────▼──┐  ┌──▼────────┐  ┌▼─────────────┐
   │ ms-auth │  │ms-agronom.│  │ms-cosecha │  │ms-financiero │
   │  :18081 │  │  :18082  │  │  :18083   │  │   :18084     │
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

Cuatro tópicos Kafka con su evento compensatorio de anulación:

| Tópico               | Productor(es)                 | Consumidor      | Se dispara cuando…                                    |
| -------------------- | ----------------------------- | --------------- | ----------------------------------------------------- |
| `costo-registrado`   | `ms-agronomico`, `ms-cosecha` | `ms-financiero` | Labor/fertilización ejecutada, recolección            |
| `costo-anulado`      | `ms-agronomico`, `ms-cosecha` | `ms-financiero` | Se anula (DELETE) una labor/fertilización/recolección |
| `ingreso-registrado` | `ms-cosecha`                  | `ms-financiero` | Venta registrada                                      |
| `ingreso-anulado`    | `ms-cosecha`                  | `ms-financiero` | Se anula (DELETE) una venta                           |

El campo `eventoId` (UUID) garantiza idempotencia en `ms-financiero` vía la tabla `eventos_procesados`.

---

## Estructura del repositorio

```text
sis-agro-cafe/
├── docs/                      # Documentación y contratos
│   ├── diagramas/             # Diagramas de arquitectura y flujos (.drawio)
│   ├── openapi/               # Contratos OpenAPI (REST) y esquema GraphQL
│   ├── PA_G1_Equipo1_Entrega2P2.docx
│   └── PA_G1_Equipo1_Entrega2P2.pdf # Documento formal de entrega
├── frontend/                  # SPA en React 19 + TypeScript + Vite (:15173)
├── infra/                     # Configuración de infraestructura
│   ├── grafana/               # Aprovisionamiento de dashboards
│   ├── postgres/init/         # Script de inicialización de PostgreSQL
│   └── prometheus/            # Recolección de métricas
├── scripts/                   # Scripts utilitarios y pruebas de humo
├── services/                  # Microservicios autónomos (Spring Boot 4 / Java 17)
│   ├── gateway-graphql/       # BFF agregador GraphQL (:18080)
│   ├── ms-auth/               # Autenticación, usuarios y emisión de JWT (:18081)
│   ├── ms-agronomico/         # Fincas, lotes, labores y fertilizaciones (:18082)
│   ├── ms-cosecha/            # Recolecciones y ventas (:18083)
│   └── ms-financiero/         # Balances de lote/finca y consumidor Kafka (:18084)
├── docker-compose.yml         # Orquestación de contenedores
└── .env.example               # Variables de entorno y puertos de referencia
```

---

## Frontend

Interfaz web en **React 19 + TypeScript + Vite** (`frontend/`), pensada para presentar
el sistema. Se levanta con el resto del stack en <http://localhost:15173>.

| Pantalla            | Qué muestra                                                               |
| ------------------- | ------------------------------------------------------------------------- |
| Acceso              | Login / registro; botón **Entrar con la cuenta demo** (la crea si no existe) |
| Panel               | Indicadores, balance general, ingresos vs costos y ventas recientes       |
| Fincas y lotes      | Fincas, lotes con su ficha técnica, edad del cultivo y balance            |
| Detalle de lote     | Labores, fertilización (programar/ejecutar), cosecha con trazabilidad del grano y ventas |
| Ventas / Finanzas   | Historial paginado y rentabilidad por lote (balance/ha, margen)          |
| Estado del sistema  | Salud en vivo de cada microservicio y accesos a Swagger, Grafana, Jaeger… |

- **Datos:** las lecturas y casi todas las escrituras van por el **Gateway GraphQL**
  (el panel se arma con una sola consulta). Lo que el gateway aún no expone (editar
  finca/lote, anular fertilización o recolección, perfil y contraseña) va directo al
  microservicio por REST.
- **Proxy:** los microservicios no exponen CORS, así que el front habla con rutas
  propias — `/graphql` y `/ms/<servicio>/...` — que Vite (desarrollo) y nginx (Docker)
  reenvían al servicio correspondiente.
- **Demo:** con la cuenta vacía, el panel ofrece *Cargar datos de demostración*:
  crea 2 fincas, 3 lotes y su historia usando las mismas mutaciones de la interfaz.
- Las escrituras envían `Idempotency-Key`, y tras cada registro la vista vuelve a
  consultar el balance porque ms-financiero lo consolida desde Kafka (consistencia
  eventual).

Desarrollo (con el backend arriba en Docker):

```bash
cd frontend
npm install
npm run dev        # http://localhost:15173, toma los puertos del .env de la raíz
npm run build      # verificación de tipos + build de producción
```

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
curl -s -X POST localhost:18081/api/v1/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Maria Ospina","email":"maria@finca.co","password":"cafe12345","rol":"CAFICULTOR"}'

TOKEN=$(curl -s -X POST localhost:18081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@finca.co","password":"cafe12345"}' | jq -r .accessToken)

# 2. Finca y lote
FINCA=$(curl -s -X POST localhost:18082/api/v1/fincas -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"La Esperanza","municipio":"Rivera","departamento":"Huila","areaTotalHectareas":4.5}' | jq -r .id)

LOTE=$(curl -s -X POST localhost:18082/api/v1/fincas/$FINCA/lotes -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Lote 3","variedad":"Castillo","fechaSiembra":"2024-03-01","areaHectareas":1.2,"densidadSiembra":5000,"alturaMsnm":1650,"phSuelo":5.4}' | jq -r .id)

# 3. Un costo y un ingreso
curl -s -X POST localhost:18082/api/v1/lotes/$LOTE/labores -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"tipo":"LIMPIA","fechaEjecucion":"2026-02-10","jornales":3,"costoManoObra":180000,"costoInsumos":45000}'

curl -s -X POST localhost:18083/api/v1/ventas -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"loteId\":\"$LOTE\",\"fecha\":\"2026-03-05\",\"cantidad\":12.5,\"precioPorArroba\":950000,\"comprador\":\"Cooperativa del Huila\"}"

# 4. Balance consolidado por Kafka (unos segundos después)
curl -s localhost:18084/api/v1/balance/lotes/$LOTE -H "Authorization: Bearer $TOKEN"
```

Lo mismo, en una sola petición al gateway:

```graphql
query {
  lote(id: "<LOTE>") {
    nombre
    variedad
    labores {
      tipo
      costoTotal
    }
    fertilizaciones {
      estado
      composicion
      costoTotal
    }
    ventas {
      comprador
      montoTotal
    }
    balance {
      costoTotal
      ingresoTotal
      balance
    }
  }
}
```

En GraphiQL agrega la cabecera `{"Authorization": "Bearer <TOKEN>"}`.

---

## Stack

Java 17 · Spring Boot 4.1.1 · Spring Security (OAuth2 Resource Server) ·
Spring Data JPA + Flyway · PostgreSQL 16 · Apache Kafka 4.1 (KRaft, sin ZooKeeper) ·
Spring for GraphQL · springdoc-openapi 3 · Gradle 9.5 · Docker Compose ·
React 19 + TypeScript + Vite · TanStack Query · nginx.

## Equipo

Julian Alfonso Montero Trujillo · Brayan Steven Aya Salinas ·
Arlen José Perdomo Rojas · Cristian Camilo Torres Cleves ·
María José Ospina Polania · Juan Jose Valbuena Camacho

Asesor: Diego F. Vargas Ramírez
