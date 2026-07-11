param(
    [string]$GradleVersion = '8.8'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = $PSScriptRoot
$depsDir = Join-Path $root '.deps'
$distZip = Join-Path $depsDir "gradle-$GradleVersion-bin.zip"
$gradleHome = Join-Path $depsDir "gradle-$GradleVersion"
$gradleBat = Join-Path $gradleHome 'bin\gradle.bat'

function Ensure-Directory {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Download-File {
    param(
        [string]$Url,
        [string]$OutFile
    )

    if (-not (Test-Path $OutFile)) {
        Invoke-WebRequest -Uri $Url -OutFile $OutFile
    }
}

Ensure-Directory -Path $depsDir

if (-not (Test-Path $gradleBat)) {
    Download-File -Url "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $distZip
    Expand-Archive -Path $distZip -DestinationPath $depsDir -Force
}

Push-Location $root
try {
    & $gradleBat clean shadowJar --no-daemon
} finally {
    Pop-Location
}
