# Deploying FitTrack to one server (Docker Compose + free DuckDNS name)

Everything runs on one VPS. Caddy handles HTTPS and serves:
- `https://NAME.duckdns.org/`      -> React frontend
- `https://NAME.duckdns.org/api`   -> API gateway
- `https://NAME.duckdns.org/auth`  -> Keycloak

## 0. Before you start (on your PC)
1. **Rotate your DeepSeek key** (the old one was pasted in chat) and keep the new one for step 6.
2. **Delete the test user** `diag-test-user` from Keycloak and Postgres.
3. **Export the Keycloak realm**: open http://localhost:8181 admin console -> realm `fitness-oauth2`
   -> Realm settings -> Action (top right) -> **Partial export** -> include *clients* and *groups and roles* -> Export.
   Save the file as `deploy/keycloak-import/fitness-oauth2-realm.json`. (Users are not exported; people sign up again.)
4. Note your local Keycloak version: `docker ps` shows the image tag; put it in `KEYCLOAK_VERSION` later.

## 1. Build the jars (on your PC)
```
cd deploy
powershell -ExecutionPolicy Bypass -File .\build-jars.ps1
```
Each service must have a `target\*.jar` afterwards.

## 2. Create the server
- Ubuntu 24.04, **8 GB RAM recommended** (4 GB works only with swap and is slow). Providers such as Hetzner, DigitalOcean, Contabo.
- Open ports **22, 80, 443** in the provider's firewall.
- Note the server's public IP.

## 3. Free domain name
1. Go to https://www.duckdns.org and sign in. Create a subdomain, e.g. `fitness-yourname`.
2. Set its IP to your server's IP and save. Your DOMAIN is `fitness-yourname.duckdns.org`.

## 4. Prepare the server (SSH in)
```
curl -fsSL https://get.docker.com | sh
# optional but needed on 4 GB servers:
fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

## 5. Copy the project to the server (from your PC, in the folder ABOVE the project)
```
tar --exclude=node_modules --exclude=.idea -czf fitness.tgz fitnessmicroservicelocal1-main
scp fitness.tgz root@SERVER_IP:~
ssh root@SERVER_IP "tar xzf fitness.tgz"
```

## 6. Configure and start (on the server)
```
cd ~/fitnessmicroservicelocal1-main/deploy
cp .env.example .env
nano .env          # set DOMAIN, passwords, KEYCLOAK_VERSION, DEEPSEEK_API_KEY
docker compose up -d --build
docker compose ps
```
First start takes several minutes. Watch with `docker compose logs -f gateway` (Ctrl+C to leave).

## 7. Fix the Keycloak client for the new address
Open `https://DOMAIN/auth/admin`, sign in with KEYCLOAK_ADMIN / KEYCLOAK_ADMIN_PASSWORD, realm `fitness-oauth2`
-> Clients -> `oauth2-pkce-client`, then set:
- Valid redirect URIs: `https://DOMAIN/*`
- Valid post logout redirect URIs: `https://DOMAIN/*`
- Web origins: `https://DOMAIN`
Save. Then open `https://DOMAIN/`, click Sign up, and use the app.

## Useful commands
- Logs of one service: `docker compose logs --tail 100 -f ai-service`
- Restart one service: `docker compose restart gateway`
- Update after code changes: rebuild jars on PC, copy again (step 5), then `docker compose up -d --build`
- Stop everything: `docker compose down` (data stays in volumes; `down -v` DELETES data)
- Back up Postgres: `docker compose exec postgres pg_dump -U postgres fitness_user_db > backup.sql`
