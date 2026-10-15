# Muro de notas

Pizarra de post-its compartida: escribes una nota, eliges color, y todos la ven. Se pueden dar likes y borrar.

La funcionalidad es mínima **a propósito**: este repositorio es una plantilla para practicar el **despliegue** (Git, Docker y AWS) en DAW, no para aprender a programar la aplicación.

- **Backend:** Spring Boot 3 + Java 21 (API REST en `/api`)
- **Frontend:** un único `index.html` servido por Nginx
- **Base de datos:** PostgreSQL (contenedor en desarrollo, RDS en el laboratorio)

```
navegador ──► Nginx (frontend:80) ──/api/──► Spring Boot (backend:8080) ──► PostgreSQL
```

## Estructura

```
.
├── backend/                  API en Spring Boot
│   ├── src/main/java/...     código (notas, estado, errores)
│   ├── src/test/java/...     tests con H2 y MockMvc
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd        Maven Wrapper (no hace falta instalar Maven)
│   └── Dockerfile            imagen en dos etapas: compilar y ejecutar
├── frontend/
│   ├── index.html            la web completa (HTML + CSS + JS)
│   ├── nginx.conf            sirve la web y reenvía /api/ al backend
│   └── Dockerfile
├── docker-compose.yml        DESARROLLO: postgres + backend + frontend
├── docker-compose.lab.yml    LABORATORIO: backend + frontend (la BD es RDS)
└── .env.example              plantilla de variables de entorno
```

## Variables de entorno

Se definen en un fichero `.env` (cópialo de `.env.example`). El `.env` **nunca** se sube a Git.

| Variable      | Obligatoria | Por defecto | Para qué sirve |
|---------------|-------------|-------------|----------------|
| `DB_HOST`     | Sí          | —           | Servidor de PostgreSQL (en el lab, el endpoint de RDS) |
| `DB_PORT`     | No          | `5432`      | Puerto de PostgreSQL |
| `DB_NAME`     | No          | `muronotas` | Nombre de la base de datos |
| `DB_USER`     | No          | `postgres`  | Usuario de la base de datos |
| `DB_PASSWORD` | Sí          | —           | Contraseña del usuario |
| `DB_SSLMODE`  | No          | `require`   | `require` para RDS, `disable` para el postgres local |

Si falta `DB_HOST` o `DB_PASSWORD`, el backend no arranca y lo dice en el log:
`ERROR: falta la variable de entorno DB_HOST.`

En desarrollo, `docker-compose.yml` fija `DB_HOST=postgres` y `DB_SSLMODE=disable`, así que solo usa del `.env` el nombre, el usuario y la contraseña.

## API

| Método | Ruta                   | Qué hace |
|--------|------------------------|----------|
| GET    | `/api/notas`           | Lista las notas, las más recientes primero |
| POST   | `/api/notas`           | Crea una nota: `{"texto": "...", "color": "amarillo"}` (colores: amarillo, rosa, verde, azul; texto de 1 a 280 caracteres). 400 si no es válida |
| POST   | `/api/notas/{id}/like` | Suma un like. 404 si no existe |
| DELETE | `/api/notas/{id}`      | Borra la nota. 404 si no existe |
| GET    | `/api/estado`          | `{"api":"ok","bd":"ok","contenedor":"<hostname>","version":"1.0.0"}`. Si la BD falla responde igualmente (200) con `"bd":"error"` |

La web consulta `/api/estado` cada 10 segundos y lo muestra en la franja de arriba (verde si todo va bien, rojo si algo falla).

## Arrancar en desarrollo

Necesitas Docker con Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

Abre <http://localhost:8080>. Para pararlo: `Ctrl+C`, o `docker compose down` (añade `-v` si quieres borrar también los datos). **[POR VERIFICAR EN LAB LIMPIO]**

Solo se publica el puerto del frontend; postgres y el backend solo son accesibles dentro de la red interna de Docker.

### Tests del backend

No necesitan Docker ni PostgreSQL (usan H2 en memoria). Solo hace falta Java 21 o superior:

```bash
cd backend
./mvnw test        # en Windows: mvnw.cmd test
```

### Sobre la tabla de la base de datos

Hibernate crea la tabla `notas` al arrancar (`spring.jpa.hibernate.ddl-auto=update`). Es cómodo para aprender, pero **en un proyecto real se usaría Flyway o Liquibase** para versionar los cambios del esquema.

## Despliegue en el laboratorio

> Resumen. **La guía detallada, paso a paso, irá en T03.** Todo este apartado está **[POR VERIFICAR EN LAB LIMPIO]**.

1. Crear una base de datos PostgreSQL en **RDS** y anotar su endpoint, usuario y contraseña. Crear dentro la base de datos `muronotas` si RDS no la ha creado.
2. Lanzar una instancia **EC2** con Docker y Docker Compose instalados.
3. En el grupo de seguridad de RDS, permitir el puerto 5432 **solo desde el grupo de seguridad de la EC2**.
4. En la EC2, clonar este repositorio (rama `muro-notas`) y crear el `.env` a partir de `.env.example` con los datos de RDS (`DB_SSLMODE=require`).
5. Arrancar:
   ```bash
   docker compose -f docker-compose.lab.yml up -d --build
   ```
6. Comprobar los logs: `docker compose -f docker-compose.lab.yml logs -f backend`.

### Cómo acceder a la web en el lab

El frontend se publica solo en `127.0.0.1:80` de la EC2, así que **desde Internet no se ve**. Hay dos opciones:

**Opción A (recomendada): túnel SSH.** Desde tu ordenador:

```bash
ssh -i tu-clave.pem -L 8080:localhost:80 ec2-user@<IP-PUBLICA-EC2>
```

Mientras la sesión esté abierta, abre <http://localhost:8080> en tu navegador. (El usuario puede ser `ec2-user` o `ubuntu` según la AMI.)

**Opción B: abrir el puerto 80 solo a tu IP.** En el grupo de seguridad de la EC2, añade una regla de entrada HTTP (80) con origen *Mi IP* (nunca `0.0.0.0/0`). Además, en `docker-compose.lab.yml` cambia `"127.0.0.1:80:80"` por `"80:80"`, porque si no Docker seguirá escuchando solo en local. Después entra en `http://<IP-PUBLICA-EC2>`.

## Qué está verificado y qué no

- ✅ Verificado: el backend compila y los tests (`mvnw test`) pasan; sin `DB_HOST` o `DB_PASSWORD` el backend se niega a arrancar con un mensaje claro.
- **[POR VERIFICAR EN LAB LIMPIO]**: construcción de las imágenes Docker, `docker compose up` en desarrollo, la conexión a RDS con `sslmode=require`, el acceso por túnel SSH y por grupo de seguridad, y la web funcionando de extremo a extremo.
