# "ATLAS" - Sistema de Gestión de Mapa Docente FFHA

Servicio backend para automatizar la automatizar la gestión de trámites correspondientes a los docentes (designaciones, licencias, renuncias, subrogaciones), así como también realizar auditorías y brindar un acceso rápido a estadísticas e información consolidada.

## Tecnologías
- Java 17
- Spring Boot
- Maven
- PostgreSQL

## Requisitos 
- JDK 17
- Maven

## Cómo empezar

### Ejecución local con Maven

Requisitos:
- JDK 17
- PostgreSQL corriendo localmente con la base de datos `ATLAS` inicializada (usando `init-atlas-db.sql`).
- Archivo `src/main/resources/application-local.properties` con tu contraseña de Postgres (o configurada vía variable de entorno `SPRING_DATASOURCE_PASSWORD`).

```bash
./mvnw spring-boot:run
```

La aplicación se iniciará en `http://localhost:8081`.

