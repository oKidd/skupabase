param(
    [string]$Version = '0.1.0'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = $PSScriptRoot
$buildDir = Join-Path $root 'build'
$depsDir = Join-Path $root '.deps'
$classesDir = Join-Path $buildDir 'classes'
$stagingDir = Join-Path $buildDir 'staging'
$srcDir = Join-Path $root 'src\main\java'
$resDir = Join-Path $root 'src\main\resources'
$outJar = Join-Path $buildDir "skupabase-$Version.jar"
$javaBin = Split-Path -Parent (Get-Command javac).Source
$javacExe = Join-Path $javaBin 'javac.exe'
$jarExe = Join-Path $javaBin 'jar.exe'

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

function Resolve-PaperApi {
    $baseUrl = 'https://repo.papermc.io/repository/maven-snapshots/io/papermc/paper/paper-api/1.20.1-R0.1-SNAPSHOT'
    $metadataFile = Join-Path $depsDir 'paper-api-metadata.xml'
    Download-File -Url "$baseUrl/maven-metadata.xml" -OutFile $metadataFile

    [xml]$metadata = Get-Content $metadataFile -Raw
    $snapshotVersion = $metadata.metadata.versioning.snapshotVersions.snapshotVersion |
        Where-Object { $_.extension -eq 'jar' } |
        Select-Object -First 1 -ExpandProperty value

    if (-not $snapshotVersion) {
        $timestamp = $metadata.metadata.versioning.snapshot.timestamp
        $buildNumber = $metadata.metadata.versioning.snapshot.buildNumber
        $snapshotVersion = "1.20.1-R0.1-$timestamp-$buildNumber"
    }

    return [pscustomobject]@{
        Url  = "$baseUrl/paper-api-$snapshotVersion.jar"
        Path = Join-Path $depsDir "paper-api-$snapshotVersion.jar"
    }
}

Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue
Ensure-Directory -Path $buildDir
Ensure-Directory -Path $depsDir
Ensure-Directory -Path $classesDir
Ensure-Directory -Path $stagingDir

$paperApi = Resolve-PaperApi
$skriptJar = Join-Path $depsDir 'Skript-2.15.4.jar'
$postgresJar = Join-Path $depsDir 'postgresql-42.7.4.jar'

Download-File -Url $paperApi.Url -OutFile $paperApi.Path
Download-File -Url 'https://repo.skriptlang.org/releases/com/github/SkriptLang/Skript/2.15.4/Skript-2.15.4.jar' -OutFile $skriptJar
Download-File -Url 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar' -OutFile $postgresJar

$sources = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }

$classpath = @($paperApi.Path, $skriptJar, $postgresJar) -join ';'
& $javacExe --release 17 -encoding UTF8 -cp $classpath -d $classesDir @sources

Copy-Item -Path (Join-Path $classesDir '*') -Destination $stagingDir -Recurse -Force
Copy-Item -Path (Join-Path $resDir '*') -Destination $stagingDir -Recurse -Force

Push-Location $stagingDir
& $jarExe xf $postgresJar
& $jarExe --create --file $outJar -C $stagingDir .
Pop-Location

Write-Host "Built $outJar"
