param([string]$Manifest = 'build/halloween-generated.json')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$gui = Join-Path $taskRoot 'mod/src/client/resources/assets/lunaeternal/textures/gui'
$destinationRoot = Join-Path $gui 'pokepad/themes/halloween'
$entries = Get-Content -LiteralPath (Join-Path $taskRoot $Manifest) -Raw | ConvertFrom-Json
foreach ($entry in $entries) {
    $source = Join-Path $gui $entry.original
    $relative = $entry.original -replace '^pokepad/', ''
    $destination = Join-Path $destinationRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
    $original = [Drawing.Image]::FromFile($source)
    $generated = [Drawing.Image]::FromFile($entry.generated)
    # Export only: artwork is designed by image_gen; retain exact native geometry and alpha.
    $bitmap = [Drawing.Bitmap]::new($original.Width,$original.Height,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([Drawing.Color]::Transparent)
        $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $attributes = [Drawing.Imaging.ImageAttributes]::new()
        $attributes.SetWrapMode([Drawing.Drawing2D.WrapMode]::TileFlipXY)
        try {
            $graphics.DrawImage($generated,[Drawing.Rectangle]::new(0,0,$original.Width,$original.Height),0,0,$generated.Width,$generated.Height,[Drawing.GraphicsUnit]::Pixel,$attributes)
            $bitmap.Save($destination,[Drawing.Imaging.ImageFormat]::Png)
        } finally { $attributes.Dispose() }
    } finally { $graphics.Dispose(); $bitmap.Dispose(); $generated.Dispose(); $original.Dispose() }
    if (Test-Path -LiteralPath ($source+'.mcmeta')) { Copy-Item -LiteralPath ($source+'.mcmeta') -Destination ($destination+'.mcmeta') }
}
Write-Output ('Exported '+$entries.Count+' actual Halloween PNG variants at original dimensions; originals untouched.')
