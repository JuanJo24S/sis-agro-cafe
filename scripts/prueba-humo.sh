#!/usr/bin/env bash
# Prueba de humo del flujo completo descrito en la seccion 6 del documento:
# registro -> login -> finca -> lote -> costo -> ingreso -> balance por Kafka.
#
# Uso:  ./scripts/prueba-humo.sh            (contra localhost, con el sistema arriba)
set -euo pipefail

ENV_FILE="$(dirname "$0")/../.env"
puerto() {
  local nombre=$1
  local defecto=$2
  local valor
  valor=$(grep -E "^${nombre}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2 | tr -d '\r' || true)
  printf '%s' "${valor:-$defecto}"
}

AUTH=${AUTH:-http://localhost:$(puerto PUERTO_AUTH 18081)}
AGRO=${AGRO:-http://localhost:$(puerto PUERTO_AGRONOMICO 18082)}
COSECHA=${COSECHA:-http://localhost:$(puerto PUERTO_COSECHA 18083)}
FINANCIERO=${FINANCIERO:-http://localhost:$(puerto PUERTO_FINANCIERO 18084)}
GATEWAY=${GATEWAY:-http://localhost:$(puerto PUERTO_GATEWAY 18080)}

command -v jq >/dev/null || { echo "Falta jq. Instalalo con: sudo pacman -S jq"; exit 1; }

CORREO="humo-$(date +%s)@finca.co"
CLAVE="cafe12345"

paso() { printf '\n\033[1;33m▸ %s\033[0m\n' "$1"; }

paso "1. Registro de usuario ($CORREO)"
curl -fsS -X POST "$AUTH/api/v1/auth/registro" -H 'Content-Type: application/json' \
  -d "{\"nombre\":\"Prueba Humo\",\"email\":\"$CORREO\",\"password\":\"$CLAVE\",\"rol\":\"CAFICULTOR\"}" \
  | jq -c '{id, email, rol}'

paso "2. Login (RF01)"
TOKEN=$(curl -fsS -X POST "$AUTH/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$CORREO\",\"password\":\"$CLAVE\"}" | jq -r .accessToken)
echo "JWT obtenido (${#TOKEN} caracteres)"
AUTORIZACION="Authorization: Bearer $TOKEN"

paso "3. Finca y lote (RF02)"
FINCA=$(curl -fsS -X POST "$AGRO/api/v1/fincas" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"nombre":"La Esperanza","municipio":"Rivera","departamento":"Huila","areaTotalHectareas":4.5}' | jq -r .id)
LOTE=$(curl -fsS -X POST "$AGRO/api/v1/fincas/$FINCA/lotes" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"nombre":"Lote 3 - La Cuchilla","variedad":"Castillo","fechaSiembra":"2024-03-01","areaHectareas":1.2,"densidadSiembra":5000,"alturaMsnm":1650,"phSuelo":5.4,"tipoSuelo":"Franco arcilloso","temperaturaPromedio":21.5}' | jq -r .id)
echo "finca=$FINCA  lote=$LOTE"

paso "4. Labor cultural -> publica CostoRegistrado (RF03)"
curl -fsS -X POST "$AGRO/api/v1/lotes/$LOTE/labores" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"tipo":"LIMPIA","descripcion":"Limpia general","fechaEjecucion":"2026-02-10","jornales":3,"costoManoObra":180000,"costoInsumos":45000}' \
  | jq -c '{tipo, costoTotal}'

paso "5. Fertilizacion programada y su ejecucion (RF04)"
FERT=$(curl -fsS -X POST "$AGRO/api/v1/lotes/$LOTE/fertilizaciones" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"estado":"PROGRAMADA","fecha":"2026-04-01","composicion":"25-4-24-3MgO","dosisKgPorArbol":0.12,"cantidadTotalKg":600,"costoInsumos":1200000,"costoManoObra":240000}' | jq -r .id)
curl -fsS -X POST "$AGRO/api/v1/fertilizaciones/$FERT/ejecucion" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"fechaEjecucion":"2026-04-03","costoInsumos":1250000,"costoManoObra":240000,"observaciones":"Aplicada tras la lluvia"}' \
  | jq -c '{estado, costoTotal}'

paso "6. Recoleccion y avance del estado del grano (RF05)"
RECO=$(curl -fsS -X POST "$COSECHA/api/v1/lotes/$LOTE/recolecciones" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"fecha":"2026-05-12","pesoCascaraKg":320,"valorPorKgArroba":2500,"recolector":"Pedro Gomez"}' | jq -r .id)
curl -fsS -X PATCH "$COSECHA/api/v1/recolecciones/$RECO/estado" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d '{"estadoGrano":"SECO_TRILLA"}' | jq -c '{estadoGrano, costoManoObra}'

paso "7. Venta -> publica IngresoRegistrado (RF06)"
curl -fsS -X POST "$COSECHA/api/v1/ventas" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d "{\"loteId\":\"$LOTE\",\"fecha\":\"2026-05-20\",\"cantidad\":12.5,\"precioPorArroba\":950000,\"comprador\":\"Cooperativa del Huila\"}" \
  | jq -c '{comprador, montoTotal}'

paso "8. Balance consolidado por Kafka (RF07, RNF01)"
for intento in $(seq 1 15); do
  BALANCE=$(curl -fsS "$FINANCIERO/api/v1/balance/lotes/$LOTE" -H "$AUTORIZACION" 2>/dev/null || true)
  if [ -n "$BALANCE" ]; then break; fi
  sleep 2
done
[ -n "$BALANCE" ] || { echo "El balance no llego; revisa 'docker compose logs ms-financiero'"; exit 1; }
echo "$BALANCE" | jq .
echo "Balance de la finca:"
curl -fsS "$FINANCIERO/api/v1/balance/fincas/$FINCA" -H "$AUTORIZACION" | jq .

paso "9. Consulta unificada por el Gateway GraphQL (RNF03)"
curl -fsS -X POST "$GATEWAY/graphql" -H "$AUTORIZACION" -H 'Content-Type: application/json' \
  -d "{\"query\":\"{ lote(id: \\\"$LOTE\\\") { nombre variedad labores { tipo costoTotal } fertilizaciones { estado costoTotal } ventas { comprador montoTotal } balance { costoTotal ingresoTotal balance } } }\"}" \
  | jq .

printf '\n\033[1;32m✓ Flujo completo verificado\033[0m\n'
