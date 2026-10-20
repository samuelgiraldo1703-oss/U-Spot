# Deja la foto de cada lugar en app/src/main/assets/figma/img/place_<id>.jpg (900 px de ancho, JPEG),
# desde lo que bajo data_import/descargar_fotos.sh. Las "LOCAL:" de fotos_lugares.txt se copian de
# la foto que ya esta en assets. Despues hay que correr excel_to_json.ps1 para enlazarlas.
#
#   powershell -ExecutionPolicy Bypass -File data_import/reducir_fotos.ps1 <carpeta de descarga>
param([Parameter(Mandatory = $true)][string]$raw)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$img = Join-Path $root 'app\src\main\assets\figma\img'
$codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
$params = New-Object Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object Drawing.Imaging.EncoderParameter([Drawing.Imaging.Encoder]::Quality, [long]74)

$ok = 0; $missing = @()
foreach ($line in [IO.File]::ReadAllLines((Join-Path $PSScriptRoot 'fotos_lugares.txt'), [Text.Encoding]::UTF8)) {
    if ($line.StartsWith('#') -or -not $line.Trim()) { continue }
    $parts = $line.Split('|') | ForEach-Object { $_.Trim() }
    $id = $parts[0]; $path = $parts[1]
    $dest = Join-Path $img "place_$id.jpg"
    if ($path.StartsWith('LOCAL:')) { Copy-Item (Join-Path $img $path.Substring(6)) $dest -Force; $ok++; continue }
    $src = Join-Path $raw "$id.img"
    if (-not (Test-Path $src)) { $missing += $id; continue }
    $image = [Drawing.Image]::FromFile($src)
    try {
        # Respeta la orientacion EXIF (fotos de celular)
        if ($image.PropertyIdList -contains 0x0112) {
            switch ([int]$image.GetPropertyItem(0x0112).Value[0]) {
                3 { $image.RotateFlip('Rotate180FlipNone') } 6 { $image.RotateFlip('Rotate90FlipNone') } 8 { $image.RotateFlip('Rotate270FlipNone') }
            }
        }
        $w = [Math]::Min(900, $image.Width); $h = [int]($image.Height * $w / $image.Width)
        $bmp = New-Object Drawing.Bitmap $w, $h
        $g = [Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = 'HighQualityBicubic'; $g.Clear([Drawing.Color]::White); $g.DrawImage($image, 0, 0, $w, $h); $g.Dispose()
        $bmp.Save($dest, $codec, $params); $bmp.Dispose(); $ok++
    } finally { $image.Dispose() }
}
"$ok fotos listas en $img"
if ($missing) { "Faltan: $($missing -join ', ')" }
