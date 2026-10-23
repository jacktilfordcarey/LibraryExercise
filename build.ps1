param(
    [string]$Action = "build"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$classes = Join-Path $root "build\classes"
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$compiler = "javac"
$runtime = "java"
if ($env:JAVA_HOME) {
    $compiler = Join-Path $env:JAVA_HOME "bin\javac.exe"
    $runtime = Join-Path $env:JAVA_HOME "bin\java.exe"
}

$sourceDirectories = @("audit", "library", "loans", "reporting", "resources", "subscribers")
$mainSources = foreach ($directory in $sourceDirectories) {
    Get-ChildItem -Path (Join-Path $root $directory) -Filter "*.java" -File |
        ForEach-Object { $_.FullName }
}
& $compiler -encoding UTF-8 -d $classes $mainSources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if ($Action -eq "test") {
    $testSources = Get-ChildItem -Path (Join-Path $root "tests") -Filter "*.java" -File |
        ForEach-Object { $_.FullName }
    & $compiler -encoding UTF-8 -cp $classes -d $classes $testSources
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    & $runtime -cp $classes tests.LibraryTests
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if ($Action -eq "run") {
    & $runtime -cp $classes library.LibrarySimulator
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if ($Action -ne "build" -and $Action -ne "test" -and $Action -ne "run") {
    Write-Error "Usage: build.bat [build|test|run]"
    exit 2
}
