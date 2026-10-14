# ===============================
# Etapa 1: Compilación (Build)
# ===============================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Descarga de dependencias en capa cacheable
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilar el empaquetado JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ===============================
# Etapa 2: Imagen final liviana (Runtime)
# ===============================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios root por seguridad
RUN addgroup -S atlas && adduser -S atlas -G atlas
USER atlas

# Copiar el jar compilado desde la etapa anterior
COPY --from=build --chown=atlas:atlas /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
