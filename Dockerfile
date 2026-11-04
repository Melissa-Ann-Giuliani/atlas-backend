# syntax=docker/dockerfile:1

# ---- Build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Dependencias primero: esta capa queda en caché hasta que cambie el pom.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios; /data/uploads es el volumen de archivos adjuntos.
RUN addgroup -S atlas && adduser -S atlas -G atlas \
    && mkdir -p /data/uploads && chown -R atlas:atlas /data
COPY --from=build --chown=atlas:atlas /workspace/target/*.jar app.jar
USER atlas

# file.upload-dir (las dos formas, por cómo Spring mapea variables con guion).
ENV FILE_UPLOADDIR=/data/uploads \
    FILE_UPLOAD_DIR=/data/uploads
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
