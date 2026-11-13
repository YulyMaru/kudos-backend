# 14 · Architecture Building Blocks (ABB) y Solution Building Blocks (SBB) — UCO Kudos

## 1. Architecture Building Blocks (ABB)


| ID | ABB | Responsabilidad | Driver | Justificación |
|---|---|---|---|---|
| ABB-01 | Gestión de Identidad | Registrar usuarios, roles y su ubicación en la estructura organizacional (facultad, programa). | DRV-008 | La plataforma tiene siete roles con reglas de unicidad (un rector, un coordinador académico activo) y de alcance que deben cumplirse siempre. |
| ABB-02 | Autenticación | Verificar quién es el usuario y emitir credenciales de sesión. | DRV-005 | Todo el contenido es institucional; no hay acceso anónimo. |
| ABB-03 | Autorización | Decidir qué puede hacer cada rol y sobre qué alcance. | DRV-005, DRV-008 | Un coordinador de programa solo administra su programa; el administrador del sistema no publica Kudos en nombre de otros. |
| ABB-04 | Persistencia | Guardar de forma consistente usuarios, Kudos, categorías, notificaciones pendientes y auditoría. | DRV-002, DRV-008 | El Kudo, sus notificaciones y su auditoría deben guardarse en una sola transacción. |
| ABB-05 | Gestión documental y de archivos | Recibir, validar, almacenar y servir los adjuntos de un Kudo. | DRV-004 | Los adjuntos pueden traer contenido malicioso y no deben perderse cuando la instancia se reinicia. |
| ABB-06 | Notificación por correo | Enviar al destinatario un correo con el Kudo recibido. | DRV-003 | Canal obligatorio del flujo Publicar Kudo. |
| ABB-07 | Notificación móvil | Enviar al celular del destinatario un mensaje (WhatsApp o SMS). | DRV-003 | Canal obligatorio del flujo Publicar Kudo. |
| ABB-08 | Notificación Push en tiempo real | Avisar al destinatario conectado sin que tenga que recargar. | DRV-001, DRV-003 | Canal obligatorio del flujo Publicar Kudo. |
| ABB-09 | Mensajería asíncrona | Desacoplar la publicación del envío de notificaciones, con reintentos y sin duplicados. | DRV-003 | Un proveedor caído no debe impedir publicar ni perder la notificación. |
| ABB-10 | Auditoría | Registrar quién hizo qué y cuándo, sin posibilidad de alterarlo. | DRV-006, DRV-008 | Exigida por el flujo Publicar Kudo y por el control institucional. |
| ABB-11 | Logging | Producir registros estructurados y correlacionables. | DRV-006 | Diagnosticar fallos en producción sin acceso al servidor. |
| ABB-12 | Observabilidad | Exponer salud, métricas y trazas de la aplicación. | DRV-006 | Saber si las notificaciones salen y cuánto tarda el feed. |
| ABB-13 | Gestión de configuración y secretos | Separar la configuración y las credenciales del código. | DRV-005, DRV-007 | El mismo artefacto debe correr en local, pruebas y producción. |
| ABB-14 | Consulta de alta lectura | Servir el feed y los datos de referencia con baja latencia. | DRV-002 | Se lee mucho más de lo que se publica. |
| ABB-15 | Canal web (interfaz de usuario) | Permitir el uso desde el celular y el computador. | DRV-001 | La comunidad usa la plataforma principalmente desde el celular. |
| ABB-16 | Cómputo y despliegue | Ejecutar el back-end y publicar el front-end en una URL pública. | DRV-007, DRV-009 | La solución debe estar desplegada para la sustentación. |
| ABB-17 | DevSecOps | Compilar, probar, analizar, escanear y desplegar de forma automática. | DRV-007 | CI, CD, Sonar y Trivy son obligatorios. |
| ABB-18 | Pruebas automatizadas | Verificar el comportamiento y las reglas de arquitectura en cada cambio. | DRV-007 | Exigido por la pirámide de pruebas. |

## 2. Solution Building Blocks (SBB) seleccionados

| ABB | SBB seleccionado | Alternativas descartadas | ADR |
|---|---|---|---|
| ABB-01 Gestión de Identidad | Módulo propio del back-end (modelo `Usuario`, reglas de dominio) sobre PostgreSQL, con índices únicos parciales | Keycloak, Supabase Auth, Firebase Authentication | ADR-008 |
| ABB-02 Autenticación | Spring Security + JWT HS256 + *refresh token* + BCrypt | Keycloak (OIDC), Firebase Authentication, sesiones con estado | ADR-008 |
| ABB-03 Autorización | Spring Security (RBAC) + reglas de alcance en el dominio | Keycloak Authorization Services, políticas en la base de datos (RLS) | ADR-008 |
| ABB-04 Persistencia | PostgreSQL en Supabase + Spring Data JPA + Flyway | MySQL, MongoDB Atlas, Firestore | ADR-003 |
| ABB-05 Archivos | Supabase Storage (bucket privado, API S3) + detección por firma (*magic bytes*) + SHA-256 | Google Cloud Storage, Cloudinary, guardar en la base de datos, disco del contenedor | ADR-007 |
| ABB-06 Correo | Spring Mail sobre SMTP de Brevo | SendGrid, Amazon SES, SMTP de Gmail | ADR-005 |
| ABB-07 Notificación móvil | SMS con Traccar SMS Gateway | WhatsApp Cloud API (rechazado), Twilio, SMSGate, Brevo SMS, Telegram | ADR-006 (rechazado), ADR-015 |
| ABB-08 Push | WebSocket + STOMP (Spring) y RxStomp (Angular) | Server-Sent Events, Firebase Cloud Messaging, consulta periódica (*polling*) | ADR-004 |
| ABB-09 Mensajería | Transactional Outbox en PostgreSQL + procesador programado con `FOR UPDATE SKIP LOCKED` y *backoff* | RabbitMQ, Google Pub/Sub, Apache Kafka | ADR-002 |
| ABB-10 Auditoría | Tabla `registro_auditoria` de solo inserción, protegida con *trigger*, alimentada por eventos de dominio | Hibernate Envers, solo logs | ADR-013 |
| ABB-11 Logging | SLF4J + Logback con salida JSON (formato Logstash), recogida por Cloud Logging | Log4j2, ELK propio | ADR-012 |
| ABB-12 Observabilidad | Spring Boot Actuator + Micrometer + OpenTelemetry, visualizado en Google Cloud Observability | Prometheus + Grafana, Datadog, New Relic | ADR-012 |
| ABB-13 Configuración y secretos | Variables de entorno + Secret Manager (producción) + GitHub Secrets (pipeline) | Spring Cloud Config, HashiCorp Vault, archivos `.env` en el repositorio | ADR-010, ADR-011 |
| ABB-14 Alta lectura | CQRS ligero + paginación por cursor + índices + caché Caffeine para categorías | Redis, réplicas de lectura, paginación por desplazamiento (*offset*) | ADR-009 |
| ABB-15 Canal web | Angular, Mobile First, PWA | React, Flutter, aplicación nativa | ADR-014 |
| ABB-16 Cómputo y despliegue | Docker + Google Cloud Run (back-end); Firebase Hosting (front-end) | Render, Railway, Azure App Service, máquina virtual | ADR-010 |
| ABB-17 DevSecOps | GitHub Actions + SonarQube Cloud + Trivy + JaCoCo | GitLab CI, Jenkins, Snyk | ADR-011 |
| ABB-18 Pruebas | JUnit 5 + Mockito + Testcontainers + ArchUnit (back-end); pruebas de Angular (front-end); Playwright (extremo a extremo) | H2 en memoria, Selenium, Cypress | ADR-011 |

## 3. Evaluación de los SBB

Criterios exigidos por el enunciado: costo, complejidad, escalabilidad, riesgos, mantenibilidad y evolución futura.

| SBB | Costo | Complejidad | Escalabilidad | Riesgos | Mantenibilidad | Evolución futura |
|---|---|---|---|---|---|---|
| Identidad propia + Spring Security + JWT | $0 | Media: se implementan emisión, refresco y bloqueo | Alta: JWT sin estado, cualquier instancia valida | Manejo incorrecto del secreto; se mitiga con Secret Manager y rotación | Alta: reglas de rol en el dominio, probadas | Migrable a OIDC institucional (Microsoft Entra ID) cambiando el adaptador `EmisorTokens` |
| PostgreSQL (Supabase) + JPA + Flyway | $0 en capa gratuita | Baja: tecnología conocida | Media: vertical en el plan pago; lecturas resueltas con índices y cursor | Pausa del proyecto gratuito por inactividad | Alta: esquema versionado con migraciones | Réplicas de lectura o PostgreSQL propio sin cambiar el núcleo |
| Supabase Storage (S3) | $0 en capa gratuita | Baja: API S3 estándar | Alta | Exposición de archivos; se mitiga con bucket privado y enlaces firmados de corta vigencia | Alta: un solo adaptador | Cualquier almacenamiento compatible con S3 (AWS, GCS, MinIO) cambiando configuración |
| Brevo SMTP | $0 con límite diario | Baja: SMTP estándar | Media: el límite diario lo controla el outbox | Correo marcado como spam; se mitiga con remitente verificado | Alta | Cualquier proveedor SMTP cambiando variables |
| WhatsApp Cloud API | Número de prueba gratuito; mensajes de plantilla con costo en producción | Media: plantilla aprobada y número verificado | Alta | Plantilla no aprobada a tiempo; se mitiga con Twilio como alternativa y con `NO_APLICA` sin celular verificado | Alta: aislado tras `CanalNotificacion` | Más plantillas o canales sin tocar el caso de uso |
| WebSocket + STOMP | $0 | Media: autenticación JWT en el `CONNECT` | Media: con varias instancias requiere un *broker* externo | Conexiones cortadas por el balanceador; se mitiga con reconexión en RxStomp | Alta | *Broker* externo (RabbitMQ STOMP) o FCM para usuarios desconectados |
| Transactional Outbox | $0, reutiliza PostgreSQL | Media | Media: `SKIP LOCKED` permite varias instancias sin duplicar envíos | Tabla que crece; se mitiga con limpieza periódica | Alta: un solo procesador para los tres canales | Publicar el outbox hacia Pub/Sub o Kafka si crece la carga |
| Auditoría append-only | $0 | Baja | Alta: solo inserciones | Volumen alto a largo plazo | Alta | Exportar a BigQuery para analítica |
| Actuator + Micrometer + OpenTelemetry + Cloud Observability | $0 en capa gratuita | Media | Alta | Costo por exceso de trazas; se mitiga con muestreo del 10 % | Alta: estándar abierto | Cambiar de proveedor de observabilidad sin tocar el código (OTLP) |
| Variables de entorno + Secret Manager | $0 en capa gratuita | Baja | Alta | Secreto filtrado en el repositorio; se mitiga con Trivy (*secret scanning*) | Alta | Rotación automática de secretos |
| CQRS ligero + cursor + Caffeine | $0 | Baja | Alta: costo constante por página | Caché desactualizada; se mitiga con expiración de 5 minutos | Alta | Redis si hay varias instancias |
| Angular PWA | $0 | Media | Alta: archivos estáticos en CDN | Curva de aprendizaje | Alta: TypeScript tipado y componentes | Instalación en el celular sin tienda de aplicaciones |
| Cloud Run + Firebase Hosting | $0 en capa gratuita | Baja | Alta: escala automáticamente según la demanda | Arranque en frío; se mitiga con una instancia mínima durante la evaluación | Alta: despliegue por contenedor | Cualquier plataforma de contenedores (GKE, Azure Container Apps) |
| GitHub Actions + SonarQube Cloud + Trivy | $0 en repositorios públicos | Baja | Alta | Falsos positivos que bloquean el despliegue; se gestionan en Sonar | Alta: flujos versionados en el repositorio | Más etapas (DAST, firma de imágenes) |
| JUnit + Testcontainers + ArchUnit + Playwright | $0 | Media | — | Pruebas lentas con contenedores; se separan unitarias e integración | Alta: las reglas de arquitectura fallan el *build* si se rompen | Pruebas de carga y contrato |

## 4. Trazabilidad ABB → SBB → código

| ABB | Puerto (núcleo) | Adaptador o componente | Estado en la línea base |
|---|---|---|---|
| ABB-01 Gestión de Identidad | `RepositorioUsuarios`, `CrearAdministradorInicialPuerto`, `RegistrarUsuarioPuerto`, `VerificarContactoPuerto`, `DecidirSolicitudPuerto`, `AdministrarEstructuraPuerto`, `GestionarVinculosPuerto` | Adaptador JPA de usuarios, `persistencia/jdbc` (solicitudes, códigos, estructura, vínculos), `InicializadorAdministrador`, índices únicos parciales (migraciones V1 y V5) | Implementado |
| ABB-02 Autenticación | `EmisorTokens`, `CodificadorContrasenas`, `RepositorioCuentas`, `RepositorioTokensRefresco`, `CerrarSesionPuerto`, `RestablecerContrasenaPuerto` | `EmisorTokensJwt`, `CodificadorContrasenasBCrypt`, `AutenticacionControlador`, `CuentaControlador` | Implementado |
| ABB-03 Autorización | Reglas del `dominio` (`ReglaAprobacion`, `ReglaModeracion`, `ReglaRegistro`) | `SeguridadConfiguracion`, `AutenticacionStompInterceptor` | Implementado |
| ABB-04 Persistencia | `RepositorioKudos`, `RepositorioCategorias`, `RepositorioReplicas`, `RepositorioCalificaciones`, `RepositorioModeracion`, `GestorTransacciones` | `persistencia/adaptador`, `mapeador`, `repositorio`, `entidad` (JPA) y `persistencia/jdbc` (SQL directo); migraciones V1–V5 | Implementado |
| ABB-05 Archivos | `AlmacenamientoArchivos`, `DetectorTipoArchivo`, `EnlacesArchivos` | `AlmacenamientoArchivosS3`, `EnlacesArchivosS3`, `AlmacenamientoArchivosLocal` (solo desarrollo), `DetectorTipoArchivoPorFirma` | Implementado; en producción se usa `ALMACENAMIENTO_TIPO=s3` |
| ABB-06 Correo | `CanalNotificacion` | `CanalCorreoSmtp` | Implementado |
| ABB-07 Notificación móvil | `CanalNotificacion` | `CanalSmsTraccar` (canal `SMS`) | Implementado |
| ABB-08 Push | `CanalNotificacion` | `CanalPushWebSocket`, `WebSocketConfiguracion` | Implementado en el back-end |
| ABB-09 Mensajería | `RepositorioOutbox`, `ProcesarNotificacionesPuerto` | `ProcesarNotificacionesCasoUso`, `ProcesadorOutboxProgramado`, `PoliticaReintentos` | Implementado |
| ABB-10 Auditoría | `RegistroAuditoria` | `RegistroAuditoriaAdaptador`, *trigger* de solo inserción | Implementado |
| ABB-11 Logging | — | SLF4J + Logback con JSON (formato Logstash) en el perfil `cloud` (`application-cloud.properties`), con `traceId` y `spanId` | Implementado |
| ABB-12 Observabilidad | — | Actuator (`health`, `info`, `metrics`), `MetricasOutbox`, `RegistroAuditoriaConMetricas`, `ObservabilidadConfiguracion` | Implementado; exportación OTLP se activa por variables en Cloud Run |
| ABB-13 Configuración | — | `application.properties` con `${...}`, clases `Propiedades*` | Implementado |
| ABB-14 Alta lectura | `ConsultarFeedPuerto`, `ConsultarKudoPuerto`, `ConsultarCategoriasPuerto`, `BuscarUsuariosPuerto`, `ConsultarReplicasPuerto`, `ConsultasKudos`, `ConsultasCategorias`, `ConsultasUsuarios`, `ConsultasConversacion` | `persistencia/consulta`, índice parcial del feed sin vetados (V5), `KudoConsultaControlador`, `CategoriaControlador`, `UsuarioControlador`, `ConversacionControlador`, caché `categorias-activas` | Implementado |
| ABB-15 Canal web | — | Repositorio `kudos-frontend` (Angular, `core`, `features`, `notificaciones`, `shared`) | En construcción |
| ABB-16 Cómputo y despliegue | — | `Dockerfile` multietapa con usuario sin privilegios; Cloud Run; Firebase Hosting | Dockerfile implementado; despliegue en configuración |
| ABB-17 DevSecOps | — | `.github/workflows/ci-cd.yml`: pruebas + JaCoCo + SonarQube Cloud, Trivy (secretos, configuración e imagen), despliegue con Workload Identity | Implementado en el back-end; front-end en construcción |
| ABB-18 Pruebas | — | 195 pruebas: unitarias, de controladores, de integración (Testcontainers) y `ReglasArquitecturaTest`; Playwright en el front-end | Implementado en el back-end; extremo a extremo en construcción |
