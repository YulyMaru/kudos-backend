# 19 · Manual de Puesta a Punto — UCO Kudos

## 1. URL públicas

| Componente | URL |
|---|---|
| Aplicación web (front-end) | https://uco-kudos-yuly.web.app |
| API (back-end) | https://kudos-backend-276396511796.us-west1.run.app |
| Salud de la API | https://kudos-backend-276396511796.us-west1.run.app/actuator/health |
| Repositorio back-end | https://github.com/YulyMaru/kudos-backend |
| Repositorio front-end | https://github.com/YulyMaru/kudos-frontend |
| Calidad (SonarQube Cloud) | https://sonarcloud.io/organizations/yulymaru/projects |

## 2. Usuarios de demostración

| Rol | Correo | Uso |
|---|---|---|
| Docente | `docente.demo@uco.edu.co` | Publica el Kudo |
| Estudiante | `yulymarulanda573@gmail.com` | Recibe el Kudo (correo, SMS y Push) |
| Administrador | `admin@uco.edu.co` | Administración y auditoría |

Las contraseñas no se publican en el repositorio; se entregan en la sustentación. Los usuarios de demostración se crean al arrancar el back-end a partir de variables de entorno (`DEMO_HABILITADA` y `KUDOS_DEMOSTRACION_USUARIOS_*`).

## 3. Prueba del flujo obligatorio "Publicar Kudo"

1. Abrir la aplicación en dos ventanas: una con el docente y otra (incógnito) con el estudiante.
2. El celular pasarela (Android con Traccar SMS Gateway) debe estar encendido, con internet y con el servicio activo.
3. El docente pulsa **Dar Kudo**, busca al estudiante, elige categoría, escribe el mensaje, adjunta una imagen y publica.
4. Resultado esperado:
   - El Kudo aparece en el feed (paso 1).
   - El estudiante recibe el correo (paso 2) y el SMS en su celular (paso 3, canal móvil; ver ADR-015).
   - La ventana del estudiante muestra el aviso Push sin recargar (paso 4).
   - Las entradas de auditoría quedan en `registro_auditoria` con el mismo `id_correlacion` (paso 5).

## 4. Ejecución local

### Requisitos
Java 25, Maven Wrapper (incluido), Node.js 20 o superior, y una base PostgreSQL (se usa la de Supabase o una local).

### Back-end (`kudos-backend`)
1. Abrir el proyecto en IntelliJ.
2. Configurar las variables de entorno de `KudosApplication`:

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-pooler>:5432/postgres?sslmode=require` |
| `DB_USUARIO` | `postgres.<proyecto>` |
| `DB_CONTRASENA` | contraseña de la base de datos |
| `JWT_SECRETO` | frase de al menos 32 caracteres |
| `ADMIN_CORREO` / `ADMIN_CONTRASENA` | administrador inicial (contraseña de 12 o más caracteres) |

3. Ejecutar `KudosApplication`. Flyway crea el esquema (V1 a V6) en el primer arranque.
4. Pruebas: `./mvnw verify` (las de integración requieren Docker para Testcontainers).

### Front-end (`kudos-frontend`)
```
npm install
npm start
```
Abrir http://localhost:4200 (usa `environment.development.ts`, que apunta a `http://localhost:8080`). Pruebas: `npm test`.

## 5. Infraestructura en la nube

| Servicio | Uso | Configuración |
|---|---|---|
| Supabase | PostgreSQL y almacenamiento de adjuntos (bucket privado `kudos-adjuntos`, API S3) | Pooler en modo sesión, región us-west-2 |
| Google Cloud Run | Back-end (contenedor) | Proyecto `uco-kudos-yuly`, región `us-west1`, 1 instancia mínima |
| Google Secret Manager | Secretos del back-end | `kudos-db-url`, `kudos-db-usuario`, `kudos-db-contrasena`, `kudos-jwt-secreto`, `kudos-admin-contrasena`, `kudos-smtp-usuario`, `kudos-smtp-contrasena`, `kudos-s3-clave-acceso`, `kudos-s3-clave-secreta`, `kudos-sms-token` |
| Artifact Registry | Imágenes Docker | Repositorio `kudos` |
| Firebase Hosting | Front-end | Proyecto `uco-kudos-yuly` |
| Brevo | Correo transaccional (SMTP) | `smtp-relay.brevo.com:587`, remitente verificado |
| Traccar SMS Gateway | Canal móvil por SMS (reemplaza a WhatsApp, ver ADR-015) | App de Play Store en un celular Android; API en la nube `https://www.traccar.org/sms/` con token |
| SonarQube Cloud | Análisis estático | Proyectos `YulyMaru_kudos-backend` y `YulyMaru_kudos-frontend` |

## 6. Despliegue automatizado

Cada *push* a `main` ejecuta `.github/workflows/ci-cd.yml` en cada repositorio: pruebas y cobertura → SonarQube Cloud (compuerta de calidad) → Trivy → despliegue (Cloud Run o Firebase). La autenticación con Google Cloud usa Workload Identity Federation, sin llaves guardadas.

**Variables de GitHub del back-end:** `SONAR_ORGANIZACION`, `SONAR_PROYECTO`, `GCP_PROYECTO`, `GCP_REGION`, `GCP_REPOSITORIO`, `GCP_PROVEEDOR_IDENTIDAD`, `GCP_CUENTA_DESPLIEGUE`, `GCP_CUENTA_EJECUCION`, `CLOUD_RUN_SERVICIO`, `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `CORS_ORIGENES`, `URL_FRONTEND`, `CORREO_REMITENTE`, `ADMIN_CORREO`. **Secreto:** `SONAR_TOKEN`.

**Variables de GitHub del front-end:** `SONAR_ORGANIZACION`, `SONAR_PROYECTO`, `FIREBASE_PROYECTO`, `GCP_PROVEEDOR_IDENTIDAD`, `GCP_CUENTA_FIREBASE`. **Secreto:** `SONAR_TOKEN`.

## 7. Cambios de configuración sin redesplegar

Se aplican con `gcloud run services update kudos-backend --region us-west1 --update-env-vars "..."`:

| Necesidad | Variable |
|---|---|
| Cambiar de proveedor de correo | `SMTP_HOST`, `SMTP_PUERTO` (+ secretos SMTP) |
| Dominios de correo aceptados en el registro | `REGISTRO_DOMINIOS` (por defecto `uco.edu.co`) |
| Usuarios de demostración | `DEMO_HABILITADA`, `KUDOS_DEMOSTRACION_USUARIOS_*` |

Un secreto se actualiza con `gcloud secrets versions add <nombre> --data-file=-` y se aplica al reiniciar el servicio.