# Run on your Windows PC from the deploy folder:  .\build-jars.ps1
# Builds the jar for each Java service so Docker only has to package them.
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Java\jdk-25' }
$modules = 'eureka','configserver','gateway','userservice','activityservice','aiservice'
foreach ($m in $modules) {
  Write-Host "=== Building $m ===" -ForegroundColor Magenta
  Push-Location "..\$m"
  .\mvnw.cmd -q -DskipTests clean package
  if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Build failed for $m" }
  Pop-Location
}
Write-Host "All jars built." -ForegroundColor Green
