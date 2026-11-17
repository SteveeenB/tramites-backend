# TP-154 (SC02-T6): imagen del backend para el despliegue institucional.
# Toda la configuración llega por variables de entorno (ver .env.example y
# DEPLOY.md); la imagen no contiene credenciales.

# ── Stage 1: compilar ──────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: imagen final liviana ──────────────────────────────
FROM eclipse-temurin:17.0.15_6-jre-jammy

# curl solo para el HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios: el proceso no corre como root
RUN groupadd --system --gid 10001 tramites \
    && useradd --system --uid 10001 --gid tramites --no-create-home --shell /usr/sbin/nologin tramites

WORKDIR /app
# Sin --chmod para no depender de BuildKit: el JAR queda de root con 0644,
# legible pero no modificable por el usuario tramites.
COPY --from=build --chown=root:root /app/target/*.jar app.jar

USER tramites

# JVM para un contenedor de 512 MB (plan gratis de Render). Con
# MaxRAMPercentage=75 el heap se llevaba 384 MB y, sumando metaspace, code
# cache e hilos, el proceso pasaba de 512 MB y Render lo mataba. Con más
# memoria se puede sobrescribir JAVA_OPTS (ver DEPLOY.md, sección 3).
ENV PORT=8080 \
    JAVA_OPTS="-Xmx256m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080

# El arranque de Spring tarda; 90 s de gracia antes de contar fallos
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT}/api/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
