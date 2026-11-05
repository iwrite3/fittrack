# ---- Stage 1: build-time options baked into an optimized Keycloak ----
FROM quay.io/keycloak/keycloak:26.0 AS builder
ENV KC_DB=postgres \
    KC_CACHE=local \
    KC_HEALTH_ENABLED=true \
    KC_METRICS_ENABLED=false
RUN /opt/keycloak/bin/kc.sh build

# ---- Stage 2: runtime image ----
FROM quay.io/keycloak/keycloak:26.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENV KC_HTTP_ENABLED=true \
    KC_PROXY_HEADERS=xforwarded \
    KC_HOSTNAME_STRICT=false \
    KC_DB_POOL_INITIAL_SIZE=1 \
    KC_DB_POOL_MIN_SIZE=1 \
    KC_DB_POOL_MAX_SIZE=5 \
    JAVA_OPTS_APPEND="-Xms512m -Xmx650m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Cloud Run sends traffic to $PORT (we set --port 8080). DB url/user/password,
# KC_HOSTNAME and admin bootstrap credentials come from env vars at deploy time.
ENV KC_HTTP_PORT=8080
EXPOSE 8080
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
