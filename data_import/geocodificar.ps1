# Geocodifica los lugares de places.json para que Spotty calcule distancias reales desde el GPS o
# el barrio del usuario. Todo con OpenStreetMap (gratis):
#
#   powershell -ExecutionPolicy Bypass -File data_import\geocodificar.ps1
#
# 1. Dirección → cruce de calles. "Cl. 12B #2-16" está sobre la Calle 12B a la altura de la
#    Carrera 2: se toma el cruce de esas dos vías en el mapa de calles del centro (Overpass,
#    se descarga una vez a osm_calles_centro.json). Nominatim no entiende placas colombianas.
# 2. Nombre del lugar en Nominatim (museos, teatros y cadenas suelen estar en OSM): si cae a menos
#    de 250 m del cruce, se usa porque es más preciso.
# 3. Si nada sirve, el lugar queda "REVISAR" y se corrige en coordenadas_manual.csv
#    (id,lat,lng,nota), que siempre manda.
#
# Resultado: data_import\coordenadas.csv, que excel_to_json.ps1 mezcla en places.json.

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Web.Extensions
$inv = [Globalization.CultureInfo]::InvariantCulture

$root = Split-Path -Parent $PSScriptRoot
$placesPath = Join-Path $root 'app\src\main\assets\places\places.json'
$streetsPath = Join-Path $PSScriptRoot 'osm_calles_centro.json'
$outPath = Join-Path $PSScriptRoot 'coordenadas.csv'
$manualPath = Join-Path $PSScriptRoot 'coordenadas_manual.csv'
$userAgent = 'U-Spot-student-app/1.0 (proyecto educativo, geocodificacion unica)'

# Universidad Jorge Tadeo Lozano según OSM: origen de las distancias del Excel.
$UTADEO = @(4.6069421, -74.0679263)

function Distance-M([double]$lat1, [double]$lng1, [double]$lat2, [double]$lng2) {
    $k = [Math]::PI / 180
    $x = ($lng2 - $lng1) * $k * [Math]::Cos(($lat1 + $lat2) / 2 * $k)
    $y = ($lat2 - $lat1) * $k
    [int](6371000 * [Math]::Sqrt($x * $x + $y * $y))
}

# --- Mapa de calles ---------------------------------------------------------------------------
if (-not (Test-Path $streetsPath)) {
    Write-Host 'Descargando calles del centro (Overpass)...'
    $q = '[out:json][timeout:90];way["highway"]["name"](4.570,-74.095,4.640,-74.035);out tags geom;'
    Invoke-WebRequest -Uri 'https://overpass.kumi.systems/api/interpreter' -Method Post -Body @{ data = $q } `
        -UserAgent $userAgent -OutFile $streetsPath -TimeoutSec 120 -UseBasicParsing
}
$ser = New-Object System.Web.Script.Serialization.JavaScriptSerializer
$ser.MaxJsonLength = [int]::MaxValue
$osm = $ser.DeserializeObject([IO.File]::ReadAllText($streetsPath, [Text.Encoding]::UTF8))

# nombre normalizado ("calle 12b", "carrera 7", "avenida jimenez") → lista de polilíneas
$streets = @{}
function Norm-Name([string]$n) {
    $n = $n.ToLowerInvariant().Normalize([Text.NormalizationForm]::FormD) -replace '\p{Mn}', ''
    $n = $n -replace '^avenida (calle|carrera)', '$1'
    ($n -replace '\s+', ' ').Trim()
}
foreach ($w in $osm['elements']) {
    if (-not $w['geometry']) { continue }
    $key = Norm-Name $w['tags']['name']
    $pts = @($w['geometry'] | ForEach-Object { , @([double]$_['lat'], [double]$_['lon']) })
    if (-not $streets.ContainsKey($key)) { $streets[$key] = New-Object System.Collections.Generic.List[object] }
    $streets[$key].Add($pts)
}
# Alias de vías con nombre propio
$alias = @{ 'avenida el dorado' = 'calle 26'; 'calle 26' = 'avenida el dorado' }

function Get-Street([string]$name) {
    $n = Norm-Name $name
    foreach ($c in @($n, $alias[$n]) + @(($n -replace ' bis.*$', ''), ($n -replace '(\d+)[a-z]( |$)', '$1$2'))) {
        if ($c -and $streets.ContainsKey($c)) { return , $streets[$c] }  # la coma evita que PowerShell desarme la lista
    }
    $null
}

# Punto donde dos vías se cruzan o pasan más cerca: cada vértice de una contra cada tramo de la
# otra, en metros planos (a esta escala la curvatura no importa). Devuelve lat, lng, separación.
$mLat = 111320.0; $mLng = 111320.0 * [Math]::Cos(4.6 * [Math]::PI / 180)
function Closest-On-Line([double[]]$p, [object]$line) {
    $best = $null; $bestD = [double]::MaxValue
    for ($i = 0; $i -lt $line.Count - 1; $i++) {
        $ax = $line[$i][1] * $mLng; $ay = $line[$i][0] * $mLat
        $bx = $line[$i + 1][1] * $mLng; $by = $line[$i + 1][0] * $mLat
        $px = $p[1] * $mLng; $py = $p[0] * $mLat
        $dx = $bx - $ax; $dy = $by - $ay; $len = $dx * $dx + $dy * $dy
        $t = if ($len -gt 0) { [Math]::Max(0, [Math]::Min(1, (($px - $ax) * $dx + ($py - $ay) * $dy) / $len)) } else { 0 }
        $cx = $ax + $t * $dx; $cy = $ay + $t * $dy
        $d = [Math]::Sqrt(($px - $cx) * ($px - $cx) + ($py - $cy) * ($py - $cy))
        if ($d -lt $bestD) { $bestD = $d; $best = @((($cy + $py) / 2 / $mLat), (($cx + $px) / 2 / $mLng)) }
    }
    if ($best) { @($best[0], $best[1], $bestD) } else { $null }
}
function Cross([object]$a, [object]$b) {
    $best = $null
    foreach ($pair in @(@($a, $b), @($b, $a))) {
        foreach ($la in $pair[0]) { foreach ($p in $la) { foreach ($lb in $pair[1]) {
            $c = Closest-On-Line $p $lb
            if ($c -and (-not $best -or $c[2] -lt $best[2])) { $best = $c }
        } } }
    }
    if ($best) { @($best[0], $best[1], [int]$best[2]) } else { $null }
}

# "Cl. 12B #2-16 Local 102" → @('Calle 12B', 'Carrera 2'); "Cl. 21 #4A-30 Este" → Carrera 4A Este
function Parse-Address([string]$a) {
    $a = ($a -split '/')[0] -replace '\s+Local.*$', ''
    if ($a -notmatch '^(Cl|Cra|Ak|Ac|Dg|Tv|Av)\.\s*(.+?)\s*#\s*(\d+[A-Z]?(\s*Bis)?)\s*-\s*\d+\w*\s*(Este|Sur)?') { return $null }
    $kind = $Matches[1]; $main = $Matches[2].Trim(); $num = $Matches[3]; $side = $Matches[5]
    switch ($kind) {
        { $_ -in 'Cl', 'Ac', 'Dg' } { $first = @{ Cl = 'Calle'; Ac = 'Calle'; Dg = 'Diagonal' }[$kind]; $crossType = 'Carrera' }
        { $_ -in 'Cra', 'Ak', 'Tv' } { $first = @{ Cra = 'Carrera'; Ak = 'Carrera'; Tv = 'Transversal' }[$kind]; $crossType = 'Calle' }
        'Av' { $first = 'Avenida'; $crossType = 'Carrera' }
    }
    $mainName = "$first $main"
    # "#0-76": no existe la Carrera 0; la placa queda junto a la Carrera 1 (o la 1 Este).
    if ($num -eq '0') { $num = '1' }
    $crossName = "$crossType $num" + $(if ($side) { " $side" } else { '' })
    # Vía siguiente ("#6-53" queda entre la Carrera 6 y la 7): respaldo si la primera no llega.
    $nextName = "$crossType $([int]($num -replace '\D.*$', '') + 1)" + $(if ($side) { " $side" } else { '' })
    @($mainName, $crossName, $nextName)
}

$lastCall = [DateTime]::MinValue
$cachePath = Join-Path $PSScriptRoot 'osm_nominatim_cache.xml'
$cache = if (Test-Path $cachePath) { Import-Clixml $cachePath } else { @{} }
function Search-Name([string]$q) {
    if ($cache.ContainsKey($q)) { return $cache[$q] }
    $r = Search-Name-Online $q
    $cache[$q] = $r
    $r
}
function Search-Name-Online([string]$q) {
    $wait = 1100 - ([DateTime]::Now - $script:lastCall).TotalMilliseconds
    if ($wait -gt 0) { Start-Sleep -Milliseconds $wait }
    $script:lastCall = [DateTime]::Now
    $url = 'https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&countrycodes=co&bounded=1' +
           '&viewbox=-74.095,4.640,-74.035,4.570&q=' + [Uri]::EscapeDataString($q)
    try { @(Invoke-RestMethod -Uri $url -Headers @{ 'User-Agent' = $userAgent; 'Accept-Language' = 'es' } -TimeoutSec 30) }
    catch { Write-Host "  nominatim: $($_.Exception.Message)"; @() }
}

# --- Lugares ----------------------------------------------------------------------------------
$places = $ser.DeserializeObject([IO.File]::ReadAllText($placesPath, [Text.Encoding]::UTF8))['places']
$manual = @{}
if (Test-Path $manualPath) { Import-Csv -Encoding UTF8 $manualPath | ForEach-Object { $manual[$_.id] = $_ } }

$rows = New-Object System.Collections.Generic.List[object]
foreach ($p in $places) {
    $id = $p['id']; $excelM = [int]$p['distanceM']
    $lat = $null; $lng = $null; $fuente = ''; $detalle = ''

    if ($manual.ContainsKey($id)) {
        $lat = [double]::Parse($manual[$id].lat, $inv); $lng = [double]::Parse($manual[$id].lng, $inv)
        $fuente = 'manual'; $detalle = $manual[$id].nota
    } else {
        $grid = $null
        $parts = Parse-Address $p['address']
        if ($parts) {
            $s1 = Get-Street $parts[0]; $s2 = Get-Street $parts[1]
            if ($s1 -and $s2) {
                $c = Cross $s1 $s2
                if ($c -and $c[2] -le 200) { $grid = $c; $detalle = "$($parts[0]) x $($parts[1]) (separación $($c[2]) m)" }
                else {
                    $s3 = Get-Street $parts[2]
                    $c3 = if ($s3) { Cross $s1 $s3 } else { $null }
                    if ($c3 -and $c3[2] -le 60) { $grid = $c3; $detalle = "$($parts[0]) x $($parts[2]) (vía siguiente)" }
                    else { $detalle = "sin cruce: $($parts[0]) x $($parts[1]) (más cerca: $(if ($c) { $c[2] }) m)" }
                }
            } else { $detalle = "vía no encontrada: $($parts[0]) / $($parts[1])" }
        } else { $detalle = 'dirección sin placa' }

        $hits = Search-Name "$($p['name']), Bogotá"
        $poi = $null
        foreach ($h in $hits) {
            $hl = [double]::Parse("$($h.lat)", $inv); $hn = [double]::Parse("$($h.lon)", $inv)
            if ($grid) { if ((Distance-M $hl $hn $grid[0] $grid[1]) -le 250) { $poi = @($hl, $hn, $h.display_name); break } }
            else { $poi = @($hl, $hn, $h.display_name); break }
        }
        if ($poi) { $lat = $poi[0]; $lng = $poi[1]; $fuente = $(if ($grid) { "nombre" } else { "solo_nombre" }); $detalle = $poi[2] }
        elseif ($grid) { $lat = $grid[0]; $lng = $grid[1]; $fuente = 'cruce' }
    }

    if ($lat -ne $null) {
        $d = Distance-M $UTADEO[0] $UTADEO[1] $lat $lng
        # El cruce se acepta si las vías se tocan (≤ 60 m); si solo pasan cerca (≤ 200 m) se revisa a mano.
        # Las distancias del Excel NO sirven para validar: muchas son un 700 genérico.
        $estado = if (($fuente -in @("manual", "nombre")) -or ($fuente -eq "cruce" -and $grid[2] -le 60)) { "ok" } else { "REVISAR" }
        $rows.Add([pscustomobject]@{ id = $id; lat = $lat.ToString('0.000000', $inv); lng = $lng.ToString('0.000000', $inv)
            fuente = $fuente; metros_utadeo = $d; metros_excel = $excelM; estado = $estado; direccion = $p['address']; detalle = $detalle })
    } else {
        $rows.Add([pscustomobject]@{ id = $id; lat = ''; lng = ''; fuente = ''; metros_utadeo = ''; metros_excel = $excelM
            estado = 'REVISAR'; direccion = $p['address']; detalle = $detalle })
    }
    Write-Host ("{0,-8} {1,-8} {2}" -f $rows[-1].estado, $rows[-1].fuente, $id)
}

$cache | Export-Clixml $cachePath
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $outPath
$bad = @($rows | Where-Object { $_.estado -ne 'ok' })
Write-Host ''
Write-Host "Listo: $($rows.Count) lugares, $($rows.Count - $bad.Count) ok, $($bad.Count) por revisar -> $outPath"
