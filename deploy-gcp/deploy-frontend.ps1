# Builds the React app in Cloud Build and hosts it on Cloud Run. Run from the project root.
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$PROJECT = "fitnessmicroservice-f8902"; $REGION = "asia-south1"
$IMAGE = "$REGION-docker.pkg.dev/$PROJECT/fittrack/frontend:latest"
$KC = "https://keycloak-378228301246.asia-south1.run.app"
$GW = "https://gateway-378228301246.asia-south1.run.app"
# Cloud Run URL of the frontend is predictable: https://frontend-<project-number>.<region>.run.app
$SITE = "https://frontend-378228301246.$REGION.run.app"

gcloud builds submit .\fitness-app-frontend --config .\fitness-app-frontend\cloudbuild.yaml `
  --substitutions "_IMAGE=$IMAGE,_API_URL=$GW/api,_KC_URL=$KC,_SITE_URL=$SITE"
gcloud run deploy frontend --image $IMAGE --region $REGION --port 8080 `
  --cpu 1 --memory 256Mi --min-instances 0 --max-instances 3 --allow-unauthenticated
# let the browser app call the gateway from the new origin
gcloud run services update gateway --region $REGION --update-env-vars "^@^CORS_ALLOWED_ORIGINS=$SITE,http://localhost:5173"
Write-Host "`nSite: $SITE`nNow add $SITE/* to Keycloak client Valid redirect URIs and $SITE to Web origins."
