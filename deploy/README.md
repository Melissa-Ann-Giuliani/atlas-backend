# Despliegue de Atlas en talampaya

Cada push a `main` se publica solo, igual que MisCopias:

```
push a main ──► GitHub Actions (tests + imagen) ──► GHCR ──► runner de talampaya: docker compose pull + up -d
```

- **atlas-backend** (`.github/workflows/deploy.yml`): tests, imagen, copia `docker-compose.yml` e
  `init-atlas-db.sql` a `/opt/atlas`, escribe `/opt/atlas/.env` desde GitHub Secrets y reinicia `db` + `backend`.
- **atlas-frontend** (`.github/workflows/deploy.yml`): imagen (con la URL pública incrustada) y reinicio de `frontend`.
- En los pull requests a `main` corren los tests y se compila la imagen, sin publicar ni desplegar.
- Un único dominio (`atlas.ffha.unsj.edu.ar`): el nginx del frontend reenvía `/api/` al backend.
- Límites de memoria: `backend` 768 MB, `db` 384 MB, `frontend` 64 MB.

## Base de datos

La primera vez que se crea el volumen, Postgres ejecuta `init-atlas-db.sql`: crea el esquema **y carga
datos de prueba**, incluidos usuarios con claves conocidas (por ejemplo `admin` / `admin123`, Administrador Global).

- **Cambiar la clave de esos usuarios apenas se levante**, o darlos de baja.
- El script solo corre con el volumen vacío. Cambios posteriores en `init-atlas-db.sql` **no se aplican solos**:
  para un sistema en producción conviene pasar el esquema a migraciones (Flyway) y separar los datos de prueba.
- Para empezar de cero (se pierde todo): `docker compose down -v` y volver a correr el deploy del backend.

## Puesta en marcha (una sola vez)

### 1. Carpeta

```bash
sudo mkdir -p /opt/atlas
sudo chown gh-runner: /opt/atlas
```

### 2. Runners (uno por repo)

En cada repo: **Settings → Actions → Runners → New self-hosted runner → Linux x64**. Como `gh-runner`:

```bash
sudo -iu gh-runner
mkdir runner-atlas-backend && cd runner-atlas-backend     # runner-atlas-frontend para el otro
# ... curl, shasum y tar que muestra GitHub ...
./config.sh --url https://github.com/areadesarrollosffha-maxi/atlas-backend \
            --token <TOKEN> --name talampaya-atlas-backend --labels talampaya --unattended
exit
sudo bash -c 'cd /home/gh-runner/runner-atlas-backend && ./svc.sh install gh-runner && ./svc.sh start && ./svc.sh status'
```

### 3. Secrets y variables

**atlas-backend → Settings → Secrets and variables → Actions**

| Tipo | Nombre | Valor |
|---|---|---|
| Secret | `DB_PASSWORD` | `openssl rand -hex 32` |
| Secret | `JWT_SECRET` | `openssl rand -hex 32` |
| Variable | `PROXY_NETWORK` | `nginx-proxy_net_mrbs` |
| Variable | `DOMAIN` | opcional, por defecto `atlas.ffha.unsj.edu.ar` |
| Variable | `LETSENCRYPT_HOST` | solo si hay acme-companion |
| Variable / Secret | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` / `MAIL_PASSWORD` | opcionales, para los correos de recuperación de clave |

**atlas-frontend → Settings → Secrets and variables → Actions**

| Tipo | Nombre | Valor |
|---|---|---|
| Variable | `PUBLIC_URL` | URL con la que se entra al sitio, sin `/` final. Por defecto `https://atlas.ffha.unsj.edu.ar`. Para probar por IP: `http://<IP>:8082` |

`PUBLIC_URL` se incrusta al compilar: si cambia, volver a correr el deploy del frontend.

### 4. Primer despliegue

1. atlas-backend: **Actions → Deploy → Run workflow** (o merge a `main`).
2. atlas-frontend: lo mismo.

### Probar por IP antes de tener el dominio

```bash
cd /opt/atlas
sudo tee docker-compose.override.yml >/dev/null <<'EOF'
services:
  frontend:
    ports:
      - "8082:80"
EOF
sudo docker compose up -d frontend
```

Con `PUBLIC_URL=http://<IP>:8082` en atlas-frontend. Al terminar: `sudo rm docker-compose.override.yml && sudo docker compose up -d frontend`.

## Operación

```bash
cd /opt/atlas
sudo docker compose ps
sudo docker compose logs -f backend
sudo docker compose exec -T db pg_dump -U atlas ATLAS | gzip > /ruta/respaldo/atlas-$(date +%F).sql.gz
```
