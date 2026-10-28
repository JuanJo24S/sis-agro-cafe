# Prueba de humo del flujo completo (version PowerShell para Windows).
# Uso:  .\scripts\prueba-humo.ps1
#
# Equivalente a prueba-humo.sh pero usando Invoke-RestMethod de PowerShell.
$ErrorActionPreference = 'Stop'

$ENV_FILE = Join-Path $PSScriptRoot '..\.env'

function puerto($nombre, $defecto) {
    $linea = Get-Content $ENV_FILE -ErrorAction SilentlyContinue |
        Select-String "^$nombre=(\d+)\s*$" |
        Select-Object -First 1
    if ($linea) {
        return $linea.Matches[0].Groups[1].Value
    }
    return $defecto
}

$AUTH       = if ($env:AUTH)       { $env:AUTH }       else { "http://localhost:$(puerto PUERTO_AUTH 18081)" }
$AGRO       = if ($env:AGRO)       { $env:AGRO }       else { "http://localhost:$(puerto PUERTO_AGRONOMICO 18082)" }
$COSECHA    = if ($env:COSECHA)    { $env:COSECHA }    else { "http://localhost:$(puerto PUERTO_COSECHA 18083)" }
$FINANCIERO = if ($env:FINANCIERO) { $env:FINANCIERO } else { "http://localhost:$(puerto PUERTO_FINANCIERO 18084)" }
$GATEWAY    = if ($env:GATEWAY)    { $env:GATEWAY }    else { "http://localhost:$(puerto PUERTO_GATEWAY 18080)" }

$CORREO = "humo-$(Get-Date -UFormat '%s' -Millisecond 0)@finca.co"
$CLAVE  = 'cafe12345'

function paso($msg) { Write-Host "`n> $msg" -ForegroundColor Yellow }

# ---------- 1. Registro ----------
paso "1. Registro de usuario ($CORREO)"
$body = @{ nombre = 'Prueba Humo'; email = $CORREO; password = $CLAVE; rol = 'CAFICULTOR' }
$reg = Invoke-RestMethod "$AUTH/api/v1/auth/registro" -Method Post -ContentType 'application/json' -Body ($body | ConvertTo-Json)
Write-Host "  id=$($reg.id) email=$($reg.email) rol=$($reg.rol)"

# ---------- 2. Login ----------
paso "2. Login (RF01)"
$loginBody = @{ email = $CORREO; password = $CLAVE }
$token = (Invoke-RestMethod "$AUTH/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body ($loginBody | ConvertTo-Json)).accessToken
Write-Host "  JWT obtenido ($($token.Length) caracteres)"
$headers = @{ Authorization = "Bearer $token" }

# ---------- 3. Finca y lote ----------
paso "3. Finca y lote (RF02)"
$fincaBody = @{ nombre = 'La Esperanza'; municipio = 'Rivera'; departamento = 'Huila'; areaTotalHectareas = 4.5 }
$finca = Invoke-RestMethod "$AGRO/api/v1/fincas" -Method Post -ContentType 'application/json' -Headers $headers -Body ($fincaBody | ConvertTo-Json)
$FINCA_ID = $finca.id

$loteBody = @{
    nombre           = 'Lote 3 - La Cuchilla'
    variedad         = 'Castillo'
    fechaSiembra     = '2024-03-01'
    areaHectareas    = 1.2
    densidadSiembra  = 5000
    alturaMsnm       = 1650
    phSuelo          = 5.4
    tipoSuelo        = 'Franco arcilloso'
    temperaturaPromedio = 21.5
}
$lote = Invoke-RestMethod "$AGRO/api/v1/fincas/$FINCA_ID/lotes" -Method Post -ContentType 'application/json' -Headers $headers -Body ($loteBody | ConvertTo-Json)
$LOTE_ID = $lote.id
Write-Host "  finca=$FINCA_ID  lote=$LOTE_ID"

# ---------- 4. Labor cultural ----------
paso "4. Labor cultural -> publica CostoRegistrado (RF03)"
$laborBody = @{
    tipo            = 'LIMPIA'
    descripcion     = 'Limpia general del lote'
    fechaEjecucion  = '2026-02-10'
    jornales        = 3
    costoManoObra   = 180000
    costoInsumos    = 45000
}
$labor = Invoke-RestMethod "$AGRO/api/v1/lotes/$LOTE_ID/labores" -Method Post -ContentType 'application/json' -Headers $headers -Body ($laborBody | ConvertTo-Json)
Write-Host "  tipo=$($labor.tipo) costoTotal=$($labor.costoTotal)"

# ---------- 5. Fertilizacion ----------
paso "5. Fertilizacion programada y su ejecucion (RF04)"
$fertBody = @{
    estado          = 'PROGRAMADA'
    fecha           = '2026-04-01'
    composicion     = '25-4-24-3MgO'
    dosisKgPorArbol = 0.12
    cantidadTotalKg = 600
    costoInsumos    = 1200000
    costoManoObra   = 240000
}
$fert = Invoke-RestMethod "$AGRO/api/v1/lotes/$LOTE_ID/fertilizaciones" -Method Post -ContentType 'application/json' -Headers $headers -Body ($fertBody | ConvertTo-Json)
$FERT_ID = $fert.id

$ejecBody = @{
    fechaEjecucion  = '2026-04-03'
    costoInsumos    = 1250000
    costoManoObra   = 240000
    observaciones   = 'Aplicada tras la lluvia'
}
$fert2 = Invoke-RestMethod "$AGRO/api/v1/fertilizaciones/$FERT_ID/ejecucion" -Method Post -ContentType 'application/json' -Headers $headers -Body ($ejecBody | ConvertTo-Json)
Write-Host "  estado=$($fert2.estado) costoTotal=$($fert2.costoTotal)"

# ---------- 6. Recoleccion ----------
paso "6. Recoleccion y avance del estado del grano (RF05)"
$recoBody = @{
    fecha           = '2026-05-12'
    pesoCascaraKg   = 320
    valorPorKgArroba = 2500
    recolector      = 'Pedro Gomez'
}
$reco = Invoke-RestMethod "$COSECHA/api/v1/lotes/$LOTE_ID/recolecciones" -Method Post -ContentType 'application/json' -Headers $headers -Body ($recoBody | ConvertTo-Json)
$RECO_ID = $reco.id

$estadoBody = @{ estadoGrano = 'SECO_TRILLA' }
$reco2 = Invoke-RestMethod "$COSECHA/api/v1/recolecciones/$RECO_ID/estado" -Method Patch -ContentType 'application/json' -Headers $headers -Body ($estadoBody | ConvertTo-Json)
Write-Host "  estadoGrano=$($reco2.estadoGrano) costoManoObra=$($reco2.costoManoObra)"

# ---------- 7. Venta ----------
paso "7. Venta -> publica IngresoRegistrado (RF06)"
$ventaBody = @{
    loteId          = $LOTE_ID
    fecha           = '2026-05-20'
    cantidad        = 12.5
    precioPorArroba = 950000
    comprador       = 'Cooperativa del Huila'
}
$venta = Invoke-RestMethod "$COSECHA/api/v1/ventas" -Method Post -ContentType 'application/json' -Headers $headers -Body ($ventaBody | ConvertTo-Json)
Write-Host "  comprador=$($venta.comprador) montoTotal=$($venta.montoTotal)"

# ---------- 8. Balance ----------
paso "8. Balance consolidado por Kafka (RF07, RNF01)"
$balance = $null
for ($i = 1; $i -le 15; $i++) {
    try {
        $balance = Invoke-RestMethod "$FINANCIERO/api/v1/balance/lotes/$LOTE_ID" -Headers $headers
        if ($balance) { break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (-not $balance) {
    Write-Host "El balance no llego; revisa 'docker compose logs ms-financiero'" -ForegroundColor Red
    exit 1
}
$balance | ConvertTo-Json -Depth 3
Write-Host "`nBalance de la finca:"
$balanceFinca = Invoke-RestMethod "$FINANCIERO/api/v1/balance/fincas/$FINCA_ID" -Headers $headers
$balanceFinca | ConvertTo-Json -Depth 3

# ---------- 9. Gateway GraphQL ----------
paso "9. Consulta unificada por el Gateway GraphQL (RNF03)"
$query = @{
    query = "{ lote(id: `"$LOTE_ID`") { nombre variedad labores { tipo costoTotal } fertilizaciones { estado costoTotal } ventas { comprador montoTotal } balance { costoTotal ingresoTotal balance } } }"
}
$graphql = Invoke-RestMethod "$GATEWAY/graphql" -Method Post -ContentType 'application/json' -Headers $headers -Body ($query | ConvertTo-Json)
$graphql | ConvertTo-Json -Depth 10

Write-Host "`n$([char]0x2713) Flujo completo verificado" -ForegroundColor Green
