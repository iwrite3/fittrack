# Re-deploys ONLY Keycloak (longer startup allowance) and then points the gateway at it.
$PROJECT = "fitnessmicroservice-f8902"; $REGION = "asia-south1"
$REG = "$REGION-docker.pkg.dev/$PROJECT/fittrack"
function Ask-Secret($l){ $s=Read-Host $l -AsSecureString; [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)) }
$DBURL = Read-Host "Keycloak DB JDBC url"
$DBUSER = Read-Host "Keycloak DB user"
$DBPASS = Ask-Secret "Keycloak DB password"
$ADMIN = Read-Host "Keycloak admin username"
$ADMINPW = Ask-Secret "Keycloak admin password"

$f = Join-Path $env:TEMP "kc.env.yaml"
@("KC_DB_URL: '$DBURL'","KC_DB_USERNAME: '$DBUSER'","KC_DB_PASSWORD: '$DBPASS'",
  "KC_BOOTSTRAP_ADMIN_USERNAME: '$ADMIN'","KC_BOOTSTRAP_ADMIN_PASSWORD: '$ADMINPW'",
  "KC_LOG_LEVEL: 'INFO'") | Set-Content $f -Encoding utf8

gcloud run deploy keycloak --image "$REG/keycloak:latest" --region $REGION --port 8080 `
  --cpu 1 --memory 1Gi --cpu-boost --min-instances 0 --max-instances 1 --timeout 300 `
  --allow-unauthenticated --no-cpu-throttling `
  --startup-probe "tcpSocket.port=8080,initialDelaySeconds=0,periodSeconds=10,timeoutSeconds=5,failureThreshold=90" `
  --env-vars-file $f
Remove-Item $f

$KC = gcloud run services describe keycloak --region $REGION --format "value(status.url)"
if (-not $KC) { Write-Host "Keycloak still not up. Run: gcloud run services logs read keycloak --region $REGION --limit 100"; return }
gcloud run services update keycloak --region $REGION --update-env-vars "KC_HOSTNAME=$KC" --no-cpu-throttling
gcloud run services update gateway --region $REGION --update-env-vars "KEYCLOAK_URL=$KC"
Write-Host "Keycloak: $KC"
