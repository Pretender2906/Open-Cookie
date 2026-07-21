$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$size = 512
$bmp = New-Object System.Drawing.Bitmap $size, $size
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::Transparent)

$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 140, 0))
$g.FillEllipse($brush, 0, 0, $size - 1, $size - 1)

$cx = $size / 2
$cy = $size / 2
$points = @(
    [System.Drawing.Point]::new([int]$cx, [int]($cy - 124)),
    [System.Drawing.Point]::new([int]($cx + 30), [int]($cy - 34)),
    [System.Drawing.Point]::new([int]($cx + 126), [int]($cy - 24)),
    [System.Drawing.Point]::new([int]($cx + 54), [int]($cy + 40)),
    [System.Drawing.Point]::new([int]($cx + 76), [int]($cy + 136)),
    [System.Drawing.Point]::new([int]$cx, [int]($cy + 84)),
    [System.Drawing.Point]::new([int]($cx - 76), [int]($cy + 136)),
    [System.Drawing.Point]::new([int]($cx - 54), [int]($cy + 40)),
    [System.Drawing.Point]::new([int]($cx - 126), [int]($cy - 24)),
    [System.Drawing.Point]::new([int]($cx - 30), [int]($cy - 34))
)
$g.FillPolygon([System.Drawing.Brushes]::White, $points)

$out = Join-Path $PSScriptRoot "..\android\website\icon.png"
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Wrote $out"
