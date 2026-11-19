param([Parameter(Mandatory=$true)][ValidateRange(1,2147483647)][int]$BlockSeconds)
# Copies are compiled under TEMP. This script never edits the submitted UserState.
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
$jdkDir = $env:JAVA_HOME
if (-not $jdkDir -or -not (Test-Path (Join-Path $jdkDir 'bin\javac.exe'))) {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $jdkDir = Split-Path (Split-Path $compiler.Source -Parent) -Parent }
    else { $jdkDir = Read-Host 'Paste the JDK 25 folder shown in IntelliJ Project Structure (without quotes)' }
}
$javac = Join-Path $jdkDir 'bin\javac.exe'
$java = Join-Path $jdkDir 'bin\java.exe'
if (-not (Test-Path $javac)) { throw 'JDK folder does not contain bin\javac.exe.' }
$fxRoot = Join-Path $env:USERPROFILE '.m2\repository\org\openjfx'
$fxJars = @(Get-ChildItem $fxRoot -Recurse -Filter '*-21.0.6-win.jar' -ErrorAction SilentlyContinue)
if ($fxJars.Count -lt 4) { throw 'Open pom.xml in IntelliJ and reload Maven dependencies first.' }
$fxClasspath = ($fxJars.FullName -join ';')
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ('gcm-lab3-' + [guid]::NewGuid())
try {
    foreach ($variant in @('synchronized', 'unsynchronized')) {
        $sourceDir = Join-Path $tempDir "$variant\src"
        $classesDir = Join-Path $tempDir "$variant\classes"
        New-Item -ItemType Directory -Force $sourceDir, $classesDir | Out-Null
        Copy-Item (Join-Path $projectDir 'src\main\java\com\example\gcm\*.java') $sourceDir
        if ($variant -eq 'unsynchronized') {
            $stateFile = Join-Path $sourceDir 'UserState.java'
            $source = [System.IO.File]::ReadAllText($stateFile).Replace('synchronized ', '')
            [System.IO.File]::WriteAllText($stateFile, $source)
        }
        $sources = @(Get-ChildItem $sourceDir -Filter '*.java' | ForEach-Object { $_.FullName })
        & $javac -cp $fxClasspath -d $classesDir @sources
        if ($LASTEXITCODE -ne 0) { throw 'Experiment compilation failed. Check that this is JDK 25.' }
        Write-Output "VARIANT: $variant"
        & $java -cp "$classesDir;$fxClasspath" com.example.gcm.ConcurrencyDemo $BlockSeconds
        if ($LASTEXITCODE -ne 0) { throw 'Experiment execution failed.' }
    }
} finally {
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
