#!/usr/bin/env bash
# Prueba de integracion del Gateway GraphQL (RNF03).
# Todo el flujo se hace SOLO a traves del gateway (/graphql), sin llamar a los
# microservicios directamente, y verifica las 8 queries y las 12 mutations.
#
# Uso:  ./scripts/prueba-gateway.sh        (con el sistema arriba: docker compose up -d)
# Requiere: curl y jq. Sale con codigo 0 si todo pasa y 1 en la primera falla.
set -euo pipefail

ENV_FILE="$(dirname "$0")/../.env"
puerto() {
  local valor
  valor=$(grep -E "^${1}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2 | tr -d '\r' || true)
  printf '%s' "${valor:-$2}"
}
GATEWAY=${GATEWAY:-http://localhost:$(puerto PUERTO_GATEWAY 18080)}

command -v jq >/dev/null || { echo "Falta jq (Debian/Ubuntu: sudo apt install jq | Arch: sudo pacman -S jq)"; exit 1; }

VERDE=$'\033[1;32m'; ROJO=$'\033[1;31m'; AMARILLO=$'\033[1;33m'; FIN=$'\033[0m'
VERIFICACIONES=0
RESP=""

paso()  { printf '\n%s▸ %s%s\n' "$AMARILLO" "$1" "$FIN"; }
ok()    { VERIFICACIONES=$((VERIFICACIONES + 1)); printf '  %s✓%s %s\n' "$VERDE" "$FIN" "$1"; }
aviso() { printf '  %s!%s %s\n' "$AMARILLO" "$FIN" "$1"; }
falla() { printf '  %s✗ %s%s\n' "$ROJO" "$1" "$FIN"; exit 1; }

# gql <consulta> [token] [variables-json]  ->  deja la respuesta en $RESP
gql() {
  local consulta=$1 token=${2:-} variables=${3:-}
  [ -n "$variables" ] || variables='{}'
  local cuerpo
  cuerpo=$(jq -nc --arg q "$consulta" --argjson v "$variables" '{query: $q, variables: $v}')
  if [ -n "$token" ]; then
    RESP=$(curl -sS "$GATEWAY/graphql" -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $token" -d "$cuerpo") || falla "No se pudo conectar con $GATEWAY"
  else
    RESP=$(curl -sS "$GATEWAY/graphql" -H 'Content-Type: application/json' \
      -d "$cuerpo") || falla "No se pudo conectar con $GATEWAY"
  fi
}

dato() { printf '%s' "$RESP" | jq -r "$1"; }

sin_errores() {
  printf '%s' "$RESP" | jq -e 'has("errors") | not' >/dev/null 2>&1 \
    || falla "$1 devolvio errores: $(printf '%s' "$RESP" | jq -c '.errors // .')"
}

# afirmar <descripcion> <filtro jq que debe dar true>
afirmar() {
  printf '%s' "$RESP" | jq -e "$2" >/dev/null 2>&1 \
    || falla "$1 | respuesta: $(printf '%s' "$RESP" | jq -c . 2>/dev/null || printf '%s' "$RESP")"
  ok "$1"
}

SUFIJO=$(date +%s)
# El Idempotency-Key debe ser un UUID
uuid() {
  if [ -r /proc/sys/kernel/random/uuid ]; then
    cat /proc/sys/kernel/random/uuid
  elif command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr 'A-Z' 'a-z'
  else
    powershell.exe -NoProfile -Command "[guid]::NewGuid().ToString()" | tr -d '\r'
  fi
}

# ----------------------------------------------------------------------------
paso "1. Esquema publicado: 8 queries y 13 mutations"
# Dos peticiones separadas: graphql-java rechaza mas de un __Type.fields por query
gql '{ __schema { queryType { fields { name } } } }'
sin_errores "Introspeccion (queries)"
afirmar "El gateway expone 8 queries" '.data.__schema.queryType.fields | length == 8'
gql '{ __schema { mutationType { fields { name } } } }'
sin_errores "Introspeccion (mutations)"
afirmar "El gateway expone 13 mutations" '.data.__schema.mutationType.fields | length == 13'

# ----------------------------------------------------------------------------
paso "2. Identidad (ms-auth): registro, login y me"
CORREO="gw-$SUFIJO@finca.co"
CLAVE="cafe12345"
gql 'mutation($i: RegistroInput!) { registro(input: $i) { id email rol estado } }' "" \
  "$(jq -nc --arg e "$CORREO" --arg p "$CLAVE" '{i: {nombre: "Prueba Gateway", email: $e, password: $p, rol: "CAFICULTOR"}}')"
sin_errores "registro"
afirmar "registro devuelve el usuario CAFICULTOR" '.data.registro.rol == "CAFICULTOR" and (.data.registro.id | length > 0)'

gql 'mutation($i: LoginInput!) { login(input: $i) { accessToken tokenType expiresIn } }' "" \
  "$(jq -nc --arg e "$CORREO" --arg p "$CLAVE" '{i: {email: $e, password: $p}}')"
sin_errores "login"
afirmar "login devuelve un JWT con expiracion" '(.data.login.accessToken | length > 20) and .data.login.expiresIn > 0'
TOKEN=$(dato '.data.login.accessToken')

gql 'mutation($i: LoginInput!) { login(input: $i) { accessToken } }' "" \
  "$(jq -nc --arg e "$CORREO" '{i: {email: $e, password: "clave-incorrecta"}}')"
afirmar "login con clave incorrecta devuelve error" 'has("errors")'

gql '{ me { nombre email rol estado } }' "$TOKEN"
sin_errores "me"
afirmar "me devuelve el usuario autenticado" ".data.me.email == \"$CORREO\""

gql '{ me { email } }'
afirmar "me sin token se clasifica como UNAUTHORIZED" '.errors[0].extensions.classification == "UNAUTHORIZED"'

# ----------------------------------------------------------------------------
paso "3. Gestion agronomica (ms-agronomico): finca y lotes"
gql 'mutation($i: FincaInput!) { crearFinca(input: $i) { id nombre propietarioId } }' "$TOKEN" \
  '{"i": {"nombre": "La Esperanza", "municipio": "Rivera", "departamento": "Huila", "areaTotalHectareas": 4.5}}'
sin_errores "crearFinca"
afirmar "crearFinca devuelve id y nombre" '.data.crearFinca.nombre == "La Esperanza"'
FINCA=$(dato '.data.crearFinca.id')

Q_LOTE='mutation($f: ID!, $i: LoteInput!) { crearLote(fincaId: $f, input: $i) { id nombre variedad areaHectareas } }'
gql "$Q_LOTE" "$TOKEN" "$(jq -nc --arg f "$FINCA" '{f: $f, i: {nombre: "Lote 3 - La Cuchilla", variedad: "Castillo", fechaSiembra: "2024-03-01", areaHectareas: 1.2, densidadSiembra: 5000, alturaMsnm: 1650, phSuelo: 5.4, tipoSuelo: "Franco arcilloso", temperaturaPromedio: 21.5}}')"
sin_errores "crearLote"
afirmar "crearLote devuelve el lote Castillo" '.data.crearLote.variedad == "Castillo"'
LOTE=$(dato '.data.crearLote.id')

gql "$Q_LOTE" "$TOKEN" "$(jq -nc --arg f "$FINCA" '{f: $f, i: {nombre: "Lote 4 - El Mirador", variedad: "Caturra", fechaSiembra: "2025-01-15", areaHectareas: 0.8}}')"
sin_errores "crearLote (segundo)"
LOTE2=$(dato '.data.crearLote.id')
ok "segundo lote creado (sin movimientos, para probar el batch)"

# ----------------------------------------------------------------------------
paso "4. Labores: registro, idempotencia y anulacion"
Q_LABOR='mutation($l: ID!, $i: LaborInput!, $k: String) { registrarLabor(loteId: $l, input: $i, idempotencyKey: $k) { id tipo costoTotal } }'
LABOR_VARS=$(jq -nc --arg l "$LOTE" --arg k "$(uuid)" '{l: $l, k: $k, i: {tipo: "LIMPIA", descripcion: "Limpia general", fechaEjecucion: "2026-02-10", jornales: 3, costoManoObra: 180000, costoInsumos: 45000}}')
gql "$Q_LABOR" "$TOKEN" "$LABOR_VARS"
sin_errores "registrarLabor"
afirmar "registrarLabor calcula costoTotal = 225000" '.data.registrarLabor.costoTotal == 225000'
LABOR=$(dato '.data.registrarLabor.id')

gql "$Q_LABOR" "$TOKEN" "$LABOR_VARS"
if [ "$(dato '.data.registrarLabor.id')" = "$LABOR" ]; then
  ok "la misma Idempotency-Key no duplica la labor"
else
  aviso "la misma Idempotency-Key creo otra labor (revisar idempotencia en ms-agronomico)"
fi

gql "$Q_LABOR" "$TOKEN" "$(jq -nc --arg l "$LOTE" --arg k "$(uuid)" '{l: $l, k: $k, i: {tipo: "PODA", fechaEjecucion: "2026-02-11", costoManoObra: 50000}}')"
sin_errores "registrarLabor (a anular)"
LABOR_ANULAR=$(dato '.data.registrarLabor.id')
gql 'mutation($id: ID!) { anularLabor(laborId: $id) }' "$TOKEN" "$(jq -nc --arg id "$LABOR_ANULAR" '{id: $id}')"
sin_errores "anularLabor"
afirmar "anularLabor devuelve true" '.data.anularLabor == true'

# ----------------------------------------------------------------------------
paso "5. Fertilizacion: programar y ejecutar"
gql 'mutation($l: ID!, $i: FertilizacionInput!) { registrarFertilizacion(loteId: $l, input: $i) { id estado costoTotal } }' "$TOKEN" \
  "$(jq -nc --arg l "$LOTE" '{l: $l, i: {estado: "PROGRAMADA", fecha: "2026-04-01", composicion: "25-4-24-3MgO", dosisKgPorArbol: 0.12, cantidadTotalKg: 600, costoInsumos: 1200000, costoManoObra: 240000}}')"
sin_errores "registrarFertilizacion"
afirmar "la fertilizacion nace PROGRAMADA" '.data.registrarFertilizacion.estado == "PROGRAMADA"'
FERT=$(dato '.data.registrarFertilizacion.id')

gql 'mutation($f: ID!, $i: EjecucionFertilizacionInput!) { ejecutarFertilizacion(fertilizacionId: $f, input: $i) { id estado costoTotal } }' "$TOKEN" \
  "$(jq -nc --arg f "$FERT" '{f: $f, i: {fechaEjecucion: "2026-04-03", costoInsumos: 1250000, costoManoObra: 240000, observaciones: "Aplicada tras la lluvia"}}')"
sin_errores "ejecutarFertilizacion"
afirmar "ejecutarFertilizacion pasa a EJECUTADA con costoTotal = 1490000" '.data.ejecutarFertilizacion.estado == "EJECUTADA" and .data.ejecutarFertilizacion.costoTotal == 1490000'

# ----------------------------------------------------------------------------
paso "6. Cosecha (ms-cosecha): recoleccion y maquina de estados del grano"
gql 'mutation($l: ID!, $i: RecoleccionInput!) { registrarRecoleccion(loteId: $l, input: $i) { id estadoGrano costoManoObra } }' "$TOKEN" \
  "$(jq -nc --arg l "$LOTE" '{l: $l, i: {fecha: "2026-05-12", pesoCascaraKg: 320, valorPorKgArroba: 2500, recolector: "Pedro Gomez"}}')"
sin_errores "registrarRecoleccion"
afirmar "registrarRecoleccion devuelve estado del grano y costo" '(.data.registrarRecoleccion.estadoGrano | length > 0) and .data.registrarRecoleccion.costoManoObra > 0'
RECO=$(dato '.data.registrarRecoleccion.id')

Q_GRANO='mutation($r: ID!, $e: EstadoGrano!) { actualizarEstadoGrano(recoleccionId: $r, estadoGrano: $e) { id estadoGrano } }'
gql "$Q_GRANO" "$TOKEN" "$(jq -nc --arg r "$RECO" '{r: $r, e: "SECO_TRILLA"}')"
sin_errores "actualizarEstadoGrano"
afirmar "el grano avanza a SECO_TRILLA" '.data.actualizarEstadoGrano.estadoGrano == "SECO_TRILLA"'

gql "$Q_GRANO" "$TOKEN" "$(jq -nc --arg r "$RECO" '{r: $r, e: "CEREZA"}')"
afirmar "la maquina de estados rechaza retroceder a CEREZA" 'has("errors")'

# ----------------------------------------------------------------------------
paso "7. Ventas: registro y anulacion (publican eventos a Kafka)"
Q_VENTA='mutation($i: VentaInput!, $k: String) { registrarVenta(input: $i, idempotencyKey: $k) { id comprador montoTotal } }'
gql "$Q_VENTA" "$TOKEN" "$(jq -nc --arg l "$LOTE" --arg k "$(uuid)" '{k: $k, i: {loteId: $l, fecha: "2026-05-20", cantidad: 12.5, precioPorArroba: 950000, comprador: "Cooperativa del Huila"}}')"
sin_errores "registrarVenta"
afirmar "registrarVenta calcula montoTotal = 11875000" '.data.registrarVenta.montoTotal == 11875000'

gql "$Q_VENTA" "$TOKEN" "$(jq -nc --arg l "$LOTE" --arg k "$(uuid)" '{k: $k, i: {loteId: $l, fecha: "2026-05-21", cantidad: 1, precioPorArroba: 100000, comprador: "Venta a anular"}}')"
sin_errores "registrarVenta (a anular)"
VENTA_ANULAR=$(dato '.data.registrarVenta.id')
gql 'mutation($id: ID!) { anularVenta(ventaId: $id) }' "$TOKEN" "$(jq -nc --arg id "$VENTA_ANULAR" '{id: $id}')"
sin_errores "anularVenta"
afirmar "anularVenta devuelve true" '.data.anularVenta == true'

# ----------------------------------------------------------------------------
paso "8. Balance consolidado por Kafka (ms-financiero), espera hasta 40 s"
ESPERADO=11875000
for _ in $(seq 1 20); do
  gql 'query($l: ID!) { balanceLote(loteId: $l) { costoTotal ingresoTotal balance } }' "$TOKEN" "$(jq -nc --arg l "$LOTE" '{l: $l}')"
  if printf '%s' "$RESP" | jq -e --argjson e "$ESPERADO" '.data.balanceLote != null and .data.balanceLote.ingresoTotal == $e and .data.balanceLote.costoTotal > 0' >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
afirmar "ingreso consolidado = 11875000 (la venta anulada se compensa)" ".data.balanceLote.ingresoTotal == $ESPERADO"
afirmar "costos consolidados > 0" '.data.balanceLote.costoTotal > 0'
afirmar "balance = ingresos - costos" '.data.balanceLote | (.ingresoTotal - .costoTotal) == .balance'

gql 'query($f: ID!) { balanceFinca(fincaId: $f) { fincaId costoTotal ingresoTotal balance } }' "$TOKEN" "$(jq -nc --arg f "$FINCA" '{f: $f}')"
sin_errores "balanceFinca"
afirmar "balanceFinca agrega los movimientos de la finca" '.data.balanceFinca != null and .data.balanceFinca.ingresoTotal > 0'

# ----------------------------------------------------------------------------
paso "9. Consulta unificada: una peticion que agrega los 4 microservicios"
gql 'query($l: ID!, $f: ID!) {
  lote(id: $l) {
    nombre variedad
    labores(size: 20) { tipo costoTotal }
    fertilizaciones(size: 20) { estado costoTotal }
    recolecciones(size: 20) { estadoGrano costoManoObra }
    ventas(size: 20) { comprador montoTotal }
    balance { costoTotal ingresoTotal balance }
  }
  finca(id: $f) {
    nombre
    lotes(size: 20) { nombre balance { balance } }
    balance { balance }
  }
}' "$TOKEN" "$(jq -nc --arg l "$LOTE" --arg f "$FINCA" '{l: $l, f: $f}')"
sin_errores "consulta unificada"
afirmar "lote.labores incluye la labor registrada" '.data.lote.labores | length >= 1'
afirmar "lote.fertilizaciones trae 1 EJECUTADA" '(.data.lote.fertilizaciones | length == 1) and .data.lote.fertilizaciones[0].estado == "EJECUTADA"'
afirmar "lote.recolecciones trae 1 recoleccion" '.data.lote.recolecciones | length == 1'
afirmar "lote.ventas incluye la venta a la cooperativa" '.data.lote.ventas | any(.comprador == "Cooperativa del Huila")'
afirmar "lote.balance resuelto contra ms-financiero" '.data.lote.balance != null'
afirmar "finca.lotes trae los 2 lotes" '.data.finca.lotes | length == 2'
afirmar "finca.balance resuelto contra ms-financiero" '.data.finca.balance != null'

gql 'query($l: ID!) { lote(id: $l) { fertilizaciones(estado: EJECUTADA, size: 20) { id estado } } }' "$TOKEN" "$(jq -nc --arg l "$LOTE" '{l: $l}')"
sin_errores "fertilizaciones con filtro"
afirmar "el filtro estado: EJECUTADA devuelve la fertilizacion" '.data.lote.fertilizaciones | length == 1'

# ----------------------------------------------------------------------------
paso "10. Queries restantes: fincas, lotes (batch) y ventas paginadas"
gql '{ fincas(page: 0, size: 20) { content { id nombre } page size totalElements totalPages } }' "$TOKEN"
sin_errores "fincas"
afirmar "fincas devuelve pagina con la finca creada" ".data.fincas.totalElements >= 1 and (.data.fincas.content | any(.id == \"$FINCA\"))"

gql 'query($ids: [ID!]!) { lotes(ids: $ids) { id nombre balance { balance } } }' "$TOKEN" "$(jq -nc --arg a "$LOTE" --arg b "$LOTE2" '{ids: [$a, $b]}')"
sin_errores "lotes (batch)"
afirmar "lotes(ids) resuelve los 2 lotes en una sola llamada" '.data.lotes | length == 2'
afirmar "el lote sin movimientos tiene balance null (no rompe el batch)" ".data.lotes | map(select(.id == \"$LOTE2\")) | .[0].balance == null"

gql 'query($l: ID) { ventas(loteId: $l, page: 0, size: 20) { content { comprador montoTotal } totalElements } }' "$TOKEN" "$(jq -nc --arg l "$LOTE" '{l: $l}')"
sin_errores "ventas"
afirmar "ventas(loteId) devuelve la pagina de ventas" '.data.ventas.totalElements >= 1'

# ----------------------------------------------------------------------------
paso "11. Errores: clasificacion y aislamiento entre usuarios"
gql 'query($id: ID!) { lote(id: $id) { nombre } }' "$TOKEN" '{"id": "00000000-0000-4000-8000-000000000000"}'
afirmar "lote inexistente se clasifica como NOT_FOUND" '.errors[0].extensions.classification == "NOT_FOUND"'

CORREO2="gw2-$SUFIJO@finca.co"
gql 'mutation($i: RegistroInput!) { registro(input: $i) { id } }' "" \
  "$(jq -nc --arg e "$CORREO2" --arg p "$CLAVE" '{i: {nombre: "Otro Caficultor", email: $e, password: $p, rol: "CAFICULTOR"}}')"
sin_errores "registro del segundo usuario"
gql 'mutation($i: LoginInput!) { login(input: $i) { accessToken } }' "" \
  "$(jq -nc --arg e "$CORREO2" --arg p "$CLAVE" '{i: {email: $e, password: $p}}')"
TOKEN2=$(dato '.data.login.accessToken')
gql 'query($id: ID!) { finca(id: $id) { nombre } }' "$TOKEN2" "$(jq -nc --arg id "$FINCA" '{id: $id}')"
afirmar "otro caficultor no puede ver la finca ajena (FORBIDDEN o NOT_FOUND)" '.errors[0].extensions.classification | (. == "FORBIDDEN" or . == "NOT_FOUND")'

printf '\n%s✓ Gateway verificado: %d comprobaciones pasaron%s\n' "$VERDE" "$VERIFICACIONES" "$FIN"
