# Environment variables (all optional locally - defaults point to localhost)

| Variable | Used by | Local default |
|---|---|---|
| CONFIG_SERVER_URL | all services | http://localhost:8888 |
| EUREKA_URL | services, gateway | http://localhost:8761/eureka/ |
| EUREKA_INSTANCE_HOSTNAME | services, gateway | localhost |
| MONGODB_URI | activity-service, ai-service | mongodb://localhost:27017/<db> (use Atlas URI when hosting) |
| RABBITMQ_HOST / PORT / USER / PASSWORD | activity-service, ai-service | localhost / 5672 / guest / guest |
| POSTGRES_URL / USER / PASSWORD | user-service | jdbc:postgresql://localhost:5432/fitness_user_db / postgres / (local docker pw) |
| DDL_AUTO | user-service | update |
| KEYCLOAK_URL | gateway | http://localhost:8181 |
| CORS_ALLOWED_ORIGINS | gateway (property cors.allowed-origins) | http://localhost:5173 |
| OPENROUTER_API_KEY | ai-service | none - required |
| OPENROUTER_API_URL / OPENROUTER_MODEL | ai-service | https://openrouter.ai/api/v1/chat/completions / nvidia/nemotron-3.5-super-vl:free |
| VITE_API_URL / VITE_KEYCLOAK_URL / VITE_REDIRECT_URI | frontend (build time) | localhost values |
