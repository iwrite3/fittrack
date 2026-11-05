# Rebuild ONLY ai-service with Jib and redeploy it (keeps its existing env vars).
$PROJECT = "fitnessmicroservice-f8902"; $REGION = "asia-south1"
$REG = "$REGION-docker.pkg.dev/$PROJECT/fittrack"
$TOKEN = gcloud auth print-access-token
Push-Location .\aiservice
$mvn = if (Test-Path .\mvnw.cmd) { ".\mvnw.cmd" } else { "mvn" }
& $mvn -B -DskipTests package com.google.cloud.tools:jib-maven-plugin:3.4.5:build `
  "-Djib.from.image=gcr.io/distroless/java21-debian12:nonroot" "-Djib.from.platforms=linux/amd64" `
  "-Djib.to.image=$REG/ai-service:latest" `
  "-Djib.to.auth.username=oauth2accesstoken" "-Djib.to.auth.password=$TOKEN" `
  "-Djib.container.jvmFlags=-XX:TieredStopAtLevel=1,-Xms64m,-Xmx320m,-XX:MaxMetaspaceSize=128m,-XX:ReservedCodeCacheSize=48m,-Xss512k,-XX:+UseSerialGC" `
  "-Djib.container.ports=8080" "-Djib.container.creationTime=USE_CURRENT_TIMESTAMP"
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Jib failed" }
Pop-Location
gcloud run deploy ai-service --image "$REG/ai-service:latest" --region $REGION
