<#
 FitTrack -> Google Cloud Run. Run from the project root:  .\deploy-gcp\deploy.ps1
 Needs: gcloud (logged in), Maven/mvnw, JDK 21. NO Docker needed (Jib + Cloud Build).
 Secrets are asked at the prompt - nothing secret is stored in this file.
#>
# gcloud writes progress/info to stderr; do not treat that as fatal (PowerShell 5.1)
$ErrorActionPreference = "Continue"

# ---------- 0. settings ----------
$PROJECT   = Read-Host "GCP project id"
$REGION    = "asia-south1"                 # change if you like
$REPO      = "fittrack"
$REG       = "$REGION-docker.pkg.dev/$PROJECT/$REPO"
$JIB       = "com.google.cloud.tools:jib-maven-plugin:3.4.5:build"
$BASE      = "gcr.io/distroless/java21-debian12:nonroot"
$JVMFLAGS  = "-XX:TieredStopAtLevel=1,-Xms64m,-Xmx320m,-XX:MaxMetaspaceSize=128m,-XX:ReservedCodeCacheSize=48m,-Xss512k,-XX:+UseSerialGC"  # for 512Mi
$USE_RABBIT = $true      # $true = keep RabbitMQ (CloudAMQP) ; $false = see pubsub-option\README.md

function Ask-Secret($label) {
  $s = Read-Host $label -AsSecureString
  [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
}
$KC_DB_URL    = Read-Host "Keycloak DB JDBC url (e.g. jdbc:postgresql://HOST:5432/keycloak)"
$KC_DB_USER   = Read-Host "Keycloak DB user"
$KC_DB_PASS   = Ask-Secret "Keycloak DB password"
$KC_ADMIN     = Read-Host "Keycloak bootstrap admin username"
$KC_ADMIN_PW  = Ask-Secret "Keycloak bootstrap admin password"
$PG_URL       = Read-Host "App Postgres JDBC url for user-service"
$PG_USER      = Read-Host "App Postgres user"
$PG_PASS      = Ask-Secret "App Postgres password"
$MONGO_ACT    = Ask-Secret "Mongo Atlas URI for activity-service (.../fitnessactivity)"
$MONGO_AI     = Ask-Secret "Mongo Atlas URI for ai-service (.../fitnessrecommendation)"
$RABBIT_URL   = if ($USE_RABBIT) { Ask-Secret "CloudAMQP url (amqps://user:pass@host/vhost)" } else { "" }
$OPENROUTER   = Ask-Secret "OpenRouter API key"
$FRONTEND     = Read-Host "Frontend origin for CORS (e.g. https://your-app.vercel.app)"

# helper: write a Cloud Run env-vars YAML file (avoids comma/quote escaping problems)
function New-EnvFile($name, $map) {
  $path = Join-Path $env:TEMP "$name.env.yaml"
  $lines = $map.GetEnumerator() | ForEach-Object { "$($_.Key): '$(($_.Value) -replace "'","''")'" }
  Set-Content -Path $path -Value $lines -Encoding utf8
  return $path
}
# Env that switches OFF Eureka / Config Server / load balancer
$COMMON = @{
  SPRING_PROFILES_ACTIVE            = "prod"
  SPRING_CLOUD_CONFIG_ENABLED       = "false"
  EUREKA_CLIENT_ENABLED             = "false"
  SPRING_CLOUD_DISCOVERY_ENABLED    = "false"
  SPRING_CLOUD_LOADBALANCER_ENABLED = "false"
}
function Url($svc) { gcloud run services describe $svc --region $REGION --project $PROJECT --format "value(status.url)" }

# ---------- 1. APIs + Artifact Registry ----------
gcloud config set project $PROJECT
gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com
gcloud artifacts repositories create $REPO --repository-format docker --location $REGION 2>&1 | Out-Host   # "already exists" is fine
gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet   # harmless; Jib below uses a token anyway

# ---------- 2. Keycloak (Cloud Build builds the Dockerfile, no local Docker) ----------
gcloud builds submit .\deploy-gcp\keycloak --tag "$REG/keycloak:latest"
$kc = New-EnvFile "keycloak" @{
  KC_DB_URL = $KC_DB_URL; KC_DB_USERNAME = $KC_DB_USER; KC_DB_PASSWORD = $KC_DB_PASS
  KC_BOOTSTRAP_ADMIN_USERNAME = $KC_ADMIN; KC_BOOTSTRAP_ADMIN_PASSWORD = $KC_ADMIN_PW
}
$kcDeploy = @("run","deploy","keycloak","--image","$REG/keycloak:latest","--region",$REGION,
  "--port","8080","--cpu","1","--memory","1Gi","--cpu-boost",
  "--min-instances","0","--max-instances","1",      # max 1: KC_CACHE=local means no clustering
  "--timeout","300","--allow-unauthenticated",
  "--startup-probe","tcpSocket.port=8080,initialDelaySeconds=0,periodSeconds=10,timeoutSeconds=5,failureThreshold=30",
  "--env-vars-file",$kc)
gcloud @kcDeploy
$KC_URL = Url "keycloak"
# KC_HOSTNAME must be the full public URL; add it now that we know it
gcloud run services update keycloak --region $REGION --update-env-vars "KC_HOSTNAME=$KC_URL"
Remove-Item $kc
Write-Host "Keycloak: $KC_URL  -> import your realm/client (fitness-oauth2 / oauth2-pkce-client) and add the frontend URL as redirect URI + web origin."

# ---------- 3. Jib builds straight to Artifact Registry ----------
$TOKEN = gcloud auth print-access-token
function Jib-Build($dir, $name) {
  Push-Location $dir
  $mvn = if (Test-Path .\mvnw.cmd) { ".\mvnw.cmd" } else { "mvn" }
  & $mvn -B -DskipTests package $JIB `
    "-Djib.from.image=$BASE" "-Djib.from.platforms=linux/amd64" `
    "-Djib.to.image=$REG/${name}:latest" `
    "-Djib.to.auth.username=oauth2accesstoken" "-Djib.to.auth.password=$TOKEN" `
    "-Djib.container.jvmFlags=$JVMFLAGS" "-Djib.container.ports=8080" `
    "-Djib.container.creationTime=USE_CURRENT_TIMESTAMP"
  if ($LASTEXITCODE -ne 0) { throw "Jib failed for $name" }
  Pop-Location
}
Jib-Build ".\userservice"     "user-service"
Jib-Build ".\aiservice"       "ai-service"
Jib-Build ".\activityservice" "activity-service"
Jib-Build ".\gateway"         "gateway"

# ---------- 4. Backends ----------
function Deploy($name, $envMap, $extra) {
  $f = New-EnvFile $name $envMap
  $args = @("run","deploy",$name,"--image","$REG/${name}:latest","--region",$REGION,
            "--port","8080","--cpu","1","--memory","512Mi","--cpu-boost","--max-instances","3",
            "--env-vars-file",$f) + $extra
  gcloud @args
  Remove-Item $f
}
# WARNING: --allow-unauthenticated lets anyone call a backend directly and fake the X-User-ID header.
# Fine for a demo; for real use make backends private and have the gateway send ID tokens (see README).
$open = @("--allow-unauthenticated","--min-instances","0")

Deploy "user-service" ($COMMON + @{ POSTGRES_URL=$PG_URL; POSTGRES_USER=$PG_USER; POSTGRES_PASSWORD=$PG_PASS }) $open
$USER_URL = Url "user-service"

# ai-service consumes RabbitMQ -> must stay alive with CPU always allocated
$aiExtra = if ($USE_RABBIT) { @("--allow-unauthenticated","--min-instances","1","--no-cpu-throttling") } else { $open }
Deploy "ai-service" ($COMMON + @{ MONGODB_URI=$MONGO_AI; RABBITMQ_URL=$RABBIT_URL; OPENROUTER_API_KEY=$OPENROUTER }) $aiExtra
$AI_URL = Url "ai-service"

Deploy "activity-service" ($COMMON + @{ MONGODB_URI=$MONGO_ACT; RABBITMQ_URL=$RABBIT_URL; USER_SERVICE_URL=$USER_URL }) $open
$ACT_URL = Url "activity-service"

# ---------- 5. Gateway (last: needs every URL) ----------
Deploy "gateway" ($COMMON + @{
    KEYCLOAK_URL=$KC_URL; USER_SERVICE_URL=$USER_URL; ACTIVITY_SERVICE_URL=$ACT_URL
    AI_SERVICE_URL=$AI_URL; CORS_ALLOWED_ORIGINS=$FRONTEND }) @("--allow-unauthenticated","--min-instances","0")
$GW = Url "gateway"

Write-Host "`nDONE`n Gateway : $GW`n Keycloak: $KC_URL`n Point the frontend API base URL at the gateway and Keycloak url at the Keycloak URL."
