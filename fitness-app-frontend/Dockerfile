# ---- build the React (Vite) app ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Vite bakes these in at build time
ARG VITE_API_URL
ARG VITE_KEYCLOAK_URL
ARG VITE_REDIRECT_URI
ENV VITE_API_URL=$VITE_API_URL VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL VITE_REDIRECT_URI=$VITE_REDIRECT_URI
RUN npm run build

# ---- serve with nginx (listens on 8080 as Cloud Run expects) ----
FROM nginxinc/nginx-unprivileged:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
