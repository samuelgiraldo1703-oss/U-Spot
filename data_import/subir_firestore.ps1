# Sincroniza los 171 lugares de app/src/main/assets/places/places.json (incluyendo description,
# tags, lat, lng, figma y photo) hacia la coleccion "places" de Cloud Firestore (proyecto u-spot-app).
#
#   powershell -ExecutionPolicy Bypass -File data_import/subir_firestore.ps1

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$jsonPath = Join-Path $root 'app\src\main\assets\places\places.json'
$projectId = 'u-spot-app'

$catalog = [IO.File]::ReadAllText($jsonPath, [Text.Encoding]::UTF8) | ConvertFrom-Json
$places = $catalog.places
Write-Output "Preparando $($places.Count) lugares para sincronizar en Cloud Firestore ($projectId)..."

function ToFirestoreValue($val) {
    if ($null -eq $val) { return @{ nullValue = $null } }
    if ($val -is [bool]) { return @{ booleanValue = $val } }
    if ($val -is [int] -or $val -is [long]) { return @{ integerValue = "$val" } }
    if ($val -is [double] -or $val -is [float] -or $val -is [decimal]) { return @{ doubleValue = [double]$val } }
    if ($val -is [string]) { return @{ stringValue = $val } }
    if ($val -is [System.Collections.IEnumerable] -and -not ($val -is [string])) {
        $arr = @()
        foreach ($item in $val) { $arr += ,(ToFirestoreValue $item) }
        return @{ arrayValue = @{ values = $arr } }
    }
    $fields = @{}
    foreach ($prop in $val.PSObject.Properties) {
        $fields[$prop.Name] = ToFirestoreValue $prop.Value
    }
    return @{ mapValue = @{ fields = $fields } }
}

$writes = @()
foreach ($p in $places) {
    $docName = "projects/$projectId/databases/(default)/documents/places/$($p.id)"
    $fields = @{
        id          = ToFirestoreValue $p.id
        figma       = ToFirestoreValue $p.figma
        name        = ToFirestoreValue $p.name
        category    = ToFirestoreValue $p.category
        address     = ToFirestoreValue $p.address
        lat         = ToFirestoreValue $p.lat
        lng         = ToFirestoreValue $p.lng
        distanceM   = ToFirestoreValue ([int]$p.distanceM)
        distance    = ToFirestoreValue $p.distance
        walkMin     = ToFirestoreValue ([int]$p.walkMin)
        nequi       = ToFirestoreValue ([bool]$p.nequi)
        group       = ToFirestoreValue ([bool]$p.group)
        solo        = ToFirestoreValue ([bool]$p.solo)
        study       = ToFirestoreValue ([bool]$p.study)
        quiet       = ToFirestoreValue ([bool]$p.quiet)
        price       = ToFirestoreValue $p.price
        priceVaries = ToFirestoreValue ([bool]$p.priceVaries)
        hours       = ToFirestoreValue $p.hours
        hoursVaries = ToFirestoreValue ([bool]$p.hoursVaries)
        schedule    = ToFirestoreValue @($p.schedule)
        notes       = ToFirestoreValue @($p.notes)
        description = ToFirestoreValue ([string]$p.description)
        tags        = ToFirestoreValue @($p.tags)
        photo       = ToFirestoreValue $p.photo
    }
    $writes += @{
        update = @{
            name   = $docName
            fields = $fields
        }
    }
}

# Refrescar el access_token usando firebase-tools si esta disponible en cache de npx
$fbJs = Get-ChildItem "$env:LOCALAPPDATA\npm-cache\_npx\*\node_modules\firebase-tools\lib\bin\firebase.js" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($fbJs) {
    & node $fbJs.FullName projects:list | Out-Null
}

$headers = @{ 'Content-Type' = 'application/json; charset=utf-8' }
$fbConfigPath = Join-Path $env:USERPROFILE '.config\configstore\firebase-tools.json'
if (Test-Path $fbConfigPath) {
    $fbCfg = Get-Content $fbConfigPath -Raw | ConvertFrom-Json
    if ($fbCfg.tokens -and $fbCfg.tokens.access_token) {
        $headers['Authorization'] = "Bearer $($fbCfg.tokens.access_token)"
    }
}

$bodyJson = @{ writes = $writes } | ConvertTo-Json -Depth 12 -Compress
$bodyBytes = [Text.Encoding]::UTF8.GetBytes($bodyJson)
$commitUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:commit"

$resp = Invoke-RestMethod -Method Post -Uri $commitUrl -Headers $headers -Body $bodyBytes
Write-Output "Sincronizacion exitosa en Firestore: $($resp.writeResults.Count) documentos actualizados."
