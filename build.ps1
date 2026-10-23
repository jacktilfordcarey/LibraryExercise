param(
    [string]$Action = "build"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$classes = Join-Path $root "build\classes"
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$compiler = $null
$runtime = $null

if ($env:JAVA_HOME) {
    $javaHomeCompiler = Join-Path $env:JAVA_HOME "bin\javac.exe"
    if (Test-Path -LiteralPath $javaHomeCompiler) {
        $compiler = $javaHomeCompiler
        $runtime = Join-Path $env:JAVA_HOME "bin\java.exe"
    }
}

if (-not $compiler) {
    $pathCompiler = Get-Command "javac.exe" -ErrorAction SilentlyContinue
    if ($pathCompiler) {
        $compiler = $pathCompiler.Source
        $runtimeCommand = Get-Command "java.exe" -ErrorAction SilentlyContinue
        if ($runtimeCommand) {
            $runtime = $runtimeCommand.Source
        }
    }
}

if (-not $compiler) {
    $javaRoots = @(
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium")
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    $jdkHomes = foreach ($javaRoot in $javaRoots) {
        Get-ChildItem -LiteralPath $javaRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\javac.exe") }
    }
    $jdkHome = $jdkHomes | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdkHome) {
        $compiler = Join-Path $jdkHome.FullName "bin\javac.exe"
        $runtime = Join-Path $jdkHome.FullName "bin\java.exe"
    }
}

if (-not $compiler -or -not $runtime -or -not (Test-Path -LiteralPath $compiler) -or
        -not (Test-Path -LiteralPath $runtime)) {
    Write-Error "A Java JDK is required. Install JDK 8 or newer, add its bin folder to PATH, or set JAVA_HOME."
    exit 1
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
