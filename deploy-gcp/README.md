# FitTrack on Google Cloud Run

Layout: `deploy.ps1` (run this), `keycloak/Dockerfile`, `jib-snippet.xml` (optional pom config), `pubsub-option/`.

## What changed in the code (local runs still work)
- Gateway routes, gateway `WebClient`, activity-service `WebClient` read `USER_SERVICE_URL`, `ACTIVITY_SERVICE_URL`, `AI_SERVICE_URL`; defaults stay `lb://...` / `http://USER-SERVICE` for local Eureka use.
- `application-prod.yml` added to gateway, userservice, activityservice, aiservice (config-server values moved in, every value from an env var, `server.port: ${PORT}`).
- Eureka/Config Server are not deployed; env vars `SPRING_CLOUD_CONFIG_ENABLED=false`, `EUREKA_CLIENT_ENABLED=false`, `SPRING_CLOUD_DISCOVERY_ENABLED=false`, `SPRING_CLOUD_LOADBALANCER_ENABLED=false` switch them off.

## Before running
1. Create: Postgres for Keycloak and for user-service (Cloud SQL or Neon), Mongo Atlas (Network Access must allow 0.0.0.0/0, Cloud Run has no fixed IP), CloudAMQP free plan (or use the Pub/Sub option).
2. Install gcloud, JDK 21; `gcloud auth login`.
3. `.\deploy-gcp\deploy.ps1` from the project root; it prompts for secrets.
4. In Keycloak: import realm `fitness-oauth2`, client `oauth2-pkce-client`, add frontend URL to redirect URIs/web origins; update the frontend config to the gateway and Keycloak URLs.

## Memory tuning
512Mi services: `-Xmx320m` + metaspace 128m + code cache 48m + thread stacks leaves headroom. Keycloak 1GiB: `-Xmx650m`. Keep heap at ~60-65% of the limit.

## Caveats
- Nothing here was compiled or deployed; expect small fixes on first run.
- Backends are `--allow-unauthenticated`, so anyone can call them directly and forge `X-User-ID`. Demo only. To fix: make backends private and have the gateway attach Google ID tokens (not written yet).
- Keycloak: max 1 instance (local cache); first request after idle is slow (cold start).
- RabbitMQ mode needs ai-service `--min-instances 1 --no-cpu-throttling` (always billed). Pub/Sub mode lets it scale to zero, but the listener swallows exceptions, so failures are acked and not retried; rethrow in the push controller path if you want retries.
