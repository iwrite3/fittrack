# FitTrack

A fitness tracker built as Spring Boot microservices. You type an activity in plain words ("20 min running"), and an AI model estimates the calories burned and writes a recommendation.

**Services:** `gateway` (entry point, checks login), `userservice` (PostgreSQL), `activityservice` (MongoDB, sends to RabbitMQ), `aiservice` (reads the queue, calls OpenRouter, MongoDB), `eureka` + `configserver` (local only), `fitness-app-frontend` (React + Vite). Login is handled by Keycloak.

## What you need
Java 21, Maven (or the included `mvnw`), Node 20+, Docker, and a free [OpenRouter](https://openrouter.ai) API key.

## Run it locally

### 1. Start the infrastructure (Docker)
```bash
docker run -d --name pg -p 5432:5432 -e POSTGRES_PASSWORD=choose-a-password -e POSTGRES_DB=fitness_user_db postgres:16
docker run -d --name mongo -p 27017:27017 mongo:7
docker run -d --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
docker run -d --name keycloak -p 8181:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.0 start-dev
```
The services expect Postgres user `postgres` and RabbitMQ `guest/guest`. Whatever Postgres password you choose, set the same value as `POSTGRES_PASSWORD` in the terminal where you start `userservice`.

### 2. Set up Keycloak (http://localhost:8181, admin / admin)
1. Create a realm named `fitness-oauth2`.
2. Realm settings → User profile → JSON editor: paste `keycloak-user-profile.json` (adds age, weight, height, gender). Turn on **User registration** under Realm settings → Login.
3. Create a client: ID `oauth2-pkce-client`, **Client authentication Off**, Standard flow On, Valid redirect URIs `http://localhost:5173/*`, Web origins `http://localhost:5173`, PKCE method `S256` (Advanced tab).
4. On the client's dedicated scope, add four **User Attribute** mappers (`age`, `weight`, `height`, `gender`), each with **Add to access token** on.

### 3. Start the services (one terminal each, in this order)
```bash
export OPENROUTER_API_KEY=your-key-here      # Windows PowerShell: $env:OPENROUTER_API_KEY="your-key-here"

cd eureka && ./mvnw spring-boot:run          # http://localhost:8761
cd configserver && ./mvnw spring-boot:run    # port 8888
cd userservice && ./mvnw spring-boot:run     # 8081
cd activityservice && ./mvnw spring-boot:run # 8082
cd aiservice && ./mvnw spring-boot:run       # 8083
cd gateway && ./mvnw spring-boot:run         # 8080
```
Start Eureka and the config server first and wait until they are up. If you change a file in `configserver`, rebuild and restart it (it serves its built copy).

### 4. Start the frontend
```bash
cd fitness-app-frontend
npm install
npm run dev        # http://localhost:5173
```
Open http://localhost:5173, register, add an activity, and wait about a minute for the AI result.

## Environment variables
| Variable | Used by | Default |
|---|---|---|
| `OPENROUTER_API_KEY` | aiservice | none (required) |
| `OPENROUTER_MODEL` | aiservice | a free NVIDIA model |
| `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | userservice | local Postgres |
| `MONGODB_URI` | activityservice, aiservice | local Mongo |
| `RABBITMQ_HOST` / `PORT` / `USER` / `PASSWORD` | activityservice, aiservice | localhost, guest |
| `KEYCLOAK_URL` | gateway | `http://localhost:8181` |
| `VITE_API_URL`, `VITE_KEYCLOAK_URL`, `VITE_REDIRECT_URI` | frontend | local ports |

More detail is in `ENV_VARIABLES.md`.

## Deploy to Google Cloud Run
See `deploy-gcp/` (`deploy.ps1`, `deploy-frontend.ps1`, `README.md`). It uses Jib and Cloud Build, so no local Docker is needed for the Java services. You provide your own project id, databases (Neon, MongoDB Atlas, CloudAMQP) and keys; none are stored in this repo.

## Known limits
- Backends on Cloud Run are deployed public for the demo, so the user-id header can be faked by anyone who finds a backend address. Make them private with gateway ID tokens for real use.
- A failed AI job is not retried.
