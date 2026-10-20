# Convierte data_import/lugares.xlsx (una hoja por integrante, mismas columnas) en
# app/src/main/assets/places/places.json: la lista de lugares que muestra el Home.
#
#   powershell -ExecutionPolicy Bypass -File data_import/excel_to_json.ps1
#
# Columnas: Nombre, Categoria, Direccion, Distancia, Nequi, Rango Precios, Horario, Grupo, Solo, Notas.
# Reglas (decididas con el equipo):
#  - Nequi = SI -> "Acepta Nequi". Grupo = SI -> "Apto para grupo". Solo = SI -> "Ideal para persona sola".
#  - Solo = SI tambien da "Apto para estudiar" y "Ambiente tranquilo", salvo en Bares y Discotecas.
#    Librerias y Museos siempre son aptos para estudiar y de ambiente tranquilo.
#    Tejo, bolos y billares nunca lo son (ambiente animado), aunque digan Solo = SI.
#  - Precio u horario "No especificado": se marca como variable (segun el evento) y se da un rango
#    aproximado con la mediana de la categoria.
#  - "Horario detallado: ..." de las Notas se separa por dias; el resto de la nota va en "Mas info".
#  - Los lugares que ya tienen pantalla de Figma se mezclan con su fila del Excel ($figma).
#  - Los cines (Cine Colombia CityU, Cinemateca) van en Teatros, como en Figma.
# El script no usa letras con tilde en el codigo (PowerShell 5.1 lee los .ps1 sin BOM como ANSI).

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path -Parent $PSScriptRoot
$xlsx = Join-Path $PSScriptRoot 'lugares.xlsx'
$out = Join-Path $root 'app\src\main\assets\places\places.json'

function Plain([string]$s) {
    # Minusculas sin tildes, para comparar y para armar ids
    $d = $s.ToLowerInvariant().Normalize([Text.NormalizationForm]::FormD)
    -join ($d.ToCharArray() | Where-Object { [Globalization.CharUnicodeInfo]::GetUnicodeCategory($_) -ne 'NonSpacingMark' })
}

function Slug([string]$s) { ((Plain $s) -replace '[^a-z0-9]+', '-').Trim('-') }

# Lugar del Excel (nombre sin tildes) -> clave de su pantalla de Figma (HomePlace.key)
$figma = @{
    'wok cityu'                         = 'wok'
    'crepes & waffles la candelaria'    = 'crepes'
    'juan valdez eje ambiental'         = 'juanvaldez'
    'bbc bodega las aguas'              = 'bbc'
    'cine colombia cityu'               = 'cinecolombia'
    'cinemateca de bogota'              = 'cinemateca'
    'museo nacional de colombia'        = 'museonacional'
    'museo del oro'                     = 'museooro'
    'libreria lerner centro'            = 'lerner'
    'bar billar 414'                    = 'billar'
    'tejo ancestral'                    = 'tejo'
}

# Nombres corregidos para la app (nombre del Excel sin tildes -> nombre a mostrar). El id del lugar
# sigue saliendo del nombre del Excel, asi no cambian su foto ni sus comentarios.
$renames = @{
    "garcia marquez center 'the original'" = "Centro Garc$([char]0xED)a M$([char]0xE1)rquez"
    'quisco de la luz'                     = 'Quiosco de la Luz'
}

$categories = @{
    'restaurantes' = 'restaurantes'; 'cafes' = 'cafes'; 'bares' = 'bares'; 'teatros' = 'teatros'
    'ocio' = 'ocio'; 'museos' = 'museos'; 'discotecas' = 'discotecas'; 'librerias' = 'librerias'
}
$moveToTeatros = @('cine colombia cityu', 'cinemateca de bogota')

# ---- Lectura del .xlsx (es un zip de XML) ----
$zip = [IO.Compression.ZipFile]::OpenRead($xlsx)
function ReadEntry($name) {
    $e = $zip.GetEntry($name); if (-not $e) { return $null }
    $r = New-Object IO.StreamReader($e.Open(), [Text.Encoding]::UTF8); try { [xml]$r.ReadToEnd() } finally { $r.Close() }
}
$ns = @{ m = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main' }
$shared = @()
$sst = ReadEntry 'xl/sharedStrings.xml'
if ($sst) { $shared = @(Select-Xml -Xml $sst -XPath '//m:si' -Namespace $ns | ForEach-Object { $_.Node.InnerText }) }
$book = ReadEntry 'xl/workbook.xml'
$sheetNames = @(Select-Xml -Xml $book -XPath '//m:sheet' -Namespace $ns | ForEach-Object { $_.Node.name })

$rows = @()
for ($i = 1; $i -le $sheetNames.Count; $i++) {
    $sheet = ReadEntry "xl/worksheets/sheet$i.xml"
    foreach ($r in (Select-Xml -Xml $sheet -XPath '//m:sheetData/m:row' -Namespace $ns | ForEach-Object { $_.Node })) {
        if ([int]$r.r -eq 1) { continue }
        $cells = @{}
        foreach ($c in $r.c) {
            $v = $c.InnerText
            if ($c.t -eq 's') { $v = $shared[[int]$c.v] }
            $cells[($c.r -replace '\d', '')] = ($v -replace '\s+', ' ').Trim()
        }
        if (-not $cells['A']) { continue }
        $rows += [pscustomobject]@{
            sheet = $sheetNames[$i - 1]; name = $cells['A']; category = $cells['B']; address = $cells['C']
            distance = $cells['D']; nequi = $cells['E']; price = $cells['F']; hours = $cells['G']
            group = $cells['H']; solo = $cells['I']; notes = $cells['J']
        }
    }
}
$zip.Dispose()

# ---- Utilidades de precio y horario ----
function Minutes([string]$t) {
    if ($t -notmatch '^(\d{1,2})(?::(\d{2}))?(am|pm)$') { return $null }
    $h = [int]$Matches[1] % 12; if ($Matches[3] -eq 'pm') { $h += 12 }
    $h * 60 + [int]$Matches[2]
}
function Clock([int]$m) {
    $m = $m % 1440; $h = [math]::Floor($m / 60); $mm = $m % 60
    $suffix = if ($h -ge 12) { 'pm' } else { 'am' }
    $h12 = $h % 12; if ($h12 -eq 0) { $h12 = 12 }
    if ($mm) { '{0}:{1:00}{2}' -f $h12, $mm, $suffix } else { "$h12$suffix" }
}
function FirstRange([string]$hours) {
    # "Lun-Sab 12pm-3am, Dom cerrado" / "12pm-11pm" -> apertura y cierre (minutos) del primer tramo
    if ($hours -match '(\d{1,2}(?::\d{2})?(?:am|pm))-(\d{1,2}(?::\d{2})?(?:am|pm))') {
        $a = Minutes $Matches[1]; $b = Minutes $Matches[2]
        if ($b -le $a) { $b += 1440 }
        return @($a, $b)
    }
    $null
}
function PriceRange([string]$p) {
    if ($p -match '^\$(\d+(?:\.\d+)?)k - \$(\d+(?:\.\d+)?)k$') { return @([double]$Matches[1], [double]$Matches[2]) }
    $null
}
function Median($values) {
    $s = @($values | Sort-Object); if ($s.Count -eq 0) { return $null }
    $s[[math]::Floor(($s.Count - 1) / 2)]
}
function Kilo([double]$k) { if ($k -eq [math]::Floor($k)) { "`$$([int]$k)k" } else { "`$${k}k" } }

function CategoryOf($row) {
    $n = Plain $row.name
    if ($moveToTeatros -contains $n) { return 'teatros' }
    $c = $categories[(Plain $row.category)]
    if (-not $c) { throw "Categoria desconocida: $($row.category) ($($row.name))" }
    $c
}

# Medianas por categoria (para precio / horario "No especificado")
$estimates = @{}
foreach ($g in ($rows | Group-Object { CategoryOf $_ })) {
    $prices = @($g.Group | ForEach-Object { PriceRange $_.price } | Where-Object { $_ })
    $ranges = @($g.Group | ForEach-Object { , (FirstRange $_.hours) } | Where-Object { $_ })
    $lo = Median ($g.Group | ForEach-Object { $p = PriceRange $_.price; if ($p) { $p[0] } })
    $hi = Median ($g.Group | ForEach-Object { $p = PriceRange $_.price; if ($p) { $p[1] } })
    $open = [math]::Round((Median ($ranges | ForEach-Object { $_[0] })) / 30) * 30
    $close = [math]::Round((Median ($ranges | ForEach-Object { $_[1] })) / 30) * 30
    $estimates[$g.Name] = @{
        price = "$(Kilo ([math]::Round($lo / 5) * 5)) - $(Kilo ([math]::Round($hi / 5) * 5))"
        hours = "$(Clock $open)-$(Clock $close)"
    }
}

# ---- Lugares ----
$dayWords = '^(Lun|Mar|Mi|Jue|Vie|S|Dom)'
$places = foreach ($row in $rows) {
    $plainName = Plain $row.name
    $cat = CategoryOf $row
    $figmaKey = $figma[$plainName]

    $meters = if ($row.distance -match '^([\d.]+)\s*km$') { [int]([double]$Matches[1] * 1000) }
              elseif ($row.distance -match '^(\d+)\s*m$') { [int]$Matches[1] }
              else { throw "Distancia rara: $($row.distance) ($($row.name))" }
    $distance = if ($meters -ge 1000) { ('{0:0.#} km' -f ($meters / 1000)).Replace(',', '.') } else { "$meters m" }

    $solo = $row.solo -eq 'SI'
    $calm = ($cat -in @('librerias', 'museos')) -or ($solo -and $cat -notin @('bares', 'discotecas'))
    if ($plainName -match "tejo|bolos|bolera|billar") { $calm = $false }

    $priceVaries = $row.price -match '^No espec'
    $hoursVaries = $row.hours -match '^No espec'

    # Notas: "Horario detallado: Lun 9am-8pm, Mar-Sab 8am-8pm" -> filas por dia; lo demas, frases sueltas
    $schedule = @(); $notes = @()
    if ($row.notes) {
        $text = $row.notes
        if ($text -match '(?i)horario detallado:\s*(.+)$') {
            $detail = $Matches[1].Trim().TrimEnd('.')
            $text = $text.Substring(0, $text.IndexOf($Matches[0])).Trim()
            foreach ($part in ($detail -split ',\s*')) {
                if ($part -match '^(\S+)\s+(.+)$') {
                    $days = $Matches[1]; $when = $Matches[2]
                    if ($days -match $dayWords) { $schedule += [ordered]@{ days = $days; hours = $when } }
                }
            }
        }
        foreach ($s in ($text -split '\.\s*')) {
            $s = $s.Trim()
            if ($s) { $notes += ($s.Substring(0, 1).ToUpper() + $s.Substring(1)) }
        }
    }

    [ordered]@{
        id            = if ($figmaKey) { $figmaKey } else { Slug $row.name }
        figma         = $figmaKey
        name          = $(if ($renames[$plainName]) { $renames[$plainName] } else { $row.name })
        category      = $cat
        address       = $row.address
        distanceM     = $meters
        distance      = $distance
        walkMin       = [int][math]::Max(1, [math]::Ceiling($meters / 75))
        nequi         = $row.nequi -eq 'SI'
        group         = $row.group -eq 'SI'
        solo          = $solo
        study         = $calm
        quiet         = $calm
        price         = if ($priceVaries) { $estimates[$cat].price } else { $row.price }
        priceVaries   = $priceVaries
        hours         = if ($hoursVaries) { $estimates[$cat].hours } else { $row.hours }
        hoursVaries   = $hoursVaries
        schedule      = $schedule
        notes         = $notes
        # Foto propia (data_import/reducir_fotos.ps1); sin ella la app usa la de la categoria
        photo         = $(if (-not $figmaKey -and (Test-Path (Join-Path $root "app\src\main\assets\figma\img\place_$(Slug $row.name).jpg"))) { "place_$(Slug $row.name).jpg" } else { $null })
    }
}

$dupes = $places | Group-Object { $_.id } | Where-Object Count -gt 1
if ($dupes) { throw "Ids repetidos: $(($dupes | ForEach-Object Name) -join ', ')" }
$missing = $figma.Values | Where-Object { $_ -notin ($places | ForEach-Object { $_.figma }) }
if ($missing) { throw "Lugares de Figma sin fila en el Excel: $($missing -join ', ')" }

# Orden: por categoria y distancia (lo mas cerca primero)
$places = @($places | Sort-Object { $_.category }, { $_.distanceM }, { $_.name })
New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
$json = ConvertTo-Json @{ version = 1; places = $places } -Depth 6 -Compress
[IO.File]::WriteAllText($out, $json, (New-Object Text.UTF8Encoding($false)))

"$($places.Count) lugares -> $out"
$places | Group-Object { $_.category } | ForEach-Object { "  $($_.Name): $($_.Count)" }
"Estimados:"; $estimates.GetEnumerator() | Sort-Object Name | ForEach-Object { "  $($_.Name): $($_.Value.price) | $($_.Value.hours)" }
