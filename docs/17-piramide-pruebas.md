# 17 · Pirámide de Pruebas — UCO Kudos

## 1. La pirámide

```text
                 ▲  End-to-End
                ╱ ╲   Playwright (front-end) + prueba del flujo en la URL pública
               ╱───╲
              ╱     ╲   Integración
             ╱       ╲  Controladores (MockMvc) · PostgreSQL real (Testcontainers) · contexto Spring
            ╱─────────╲
           ╱           ╲  Unitarias
          ╱             ╲ Dominio · casos de uso · adaptadores · reglas de arquitectura (ArchUnit)
         ╱───────────────╲
```

La mayoría de las pruebas está en la base porque Puertos y Adaptadores permite probar el dominio y los casos de uso sin base de datos, red ni Spring: son rápidas y señalan el fallo con precisión. Hacia arriba hay menos pruebas, más lentas y más costosas, que confirman que las piezas funcionan juntas.

## 2. Back-end (`kudos-backend`)

| Nivel | Qué verifica | Herramientas | Clases de prueba | Pruebas |
|---|---|---|---|:-:|
| **Unitarias — dominio** | Reglas de negocio sin dependencias: un Kudo no se da a uno mismo, transiciones de la notificación, política de reintentos, contraseñas, registro y aprobaciones. | JUnit 5, AssertJ | `KudoTest`, `NotificacionTest`, `UsuarioTest`, `PoliticaContrasenaTest`, `PoliticaReintentosTest`, `ReglasRegistroYAprobacionTest` | 30 |
| **Unitarias — casos de uso** | El flujo completo de cada funcionalidad con dobles de los puertos de salida: Publicar Kudo (Kudo + 3 notificaciones + auditoría en una transacción, borrado de adjuntos si falla), procesamiento del outbox (envío, reintento, `FALLIDO`, `NO_APLICA`), autenticación, registro, conversación, moderación y administración. | JUnit 5, Mockito, dobles propios (`DoblesAutenticacion`) | `PublicarKudoCasoUsoTest`, `ProcesarNotificacionesCasoUsoTest`, `IniciarSesionCasoUsoTest`, `RefrescarSesionCasoUsoTest`, `RegistroYActivacionCasoUsoTest`, `ConversacionYModeracionCasoUsoTest`, `AdministracionCasoUsoTest` y 4 más | 76 |
| **Unitarias — adaptadores** | Cada adaptador aislado de su proveedor: correo (`JavaMailSender` simulado con Mockito), SMS (API de Traccar simulada con `MockRestServiceServer`: éxito, error HTTP, sin celular y código de verificación), Push (usuario conectado o no), S3, detección de tipo por firma, *path traversal*, JWT y BCrypt, mapeadores, interceptor STOMP, decorador de métricas. | JUnit 5, Mockito, `MockRestServiceServer` | `CanalCorreoSmtpTest`, `CanalWhatsappCloudApiTest`, `CanalPushWebSocketTest`, `AlmacenamientoS3Test`, `DetectorTipoArchivoPorFirmaTest`, `AlmacenamientoArchivosLocalTest`, `SeguridadAdaptadoresTest`, `AutenticacionStompInterceptorTest` y 2 más | 39 |
| **Arquitectura** | El dominio no depende de nadie; la aplicación solo del dominio; los controladores solo usan puertos de entrada. Si alguien rompe una regla, el *build* falla. | ArchUnit | `ReglasArquitecturaTest` | 3 |
| **Integración — API** | Cada endpoint con la seguridad real: códigos HTTP, JSON de entrada y salida, 401 sin token, 403 por rol, errores como ProblemDetail, *multipart* con adjuntos. | Spring `@WebMvcTest`, MockMvc, Spring Security Test | `KudoControladorTest`, `AutenticacionControladorTest`, `ConsultasControladoresTest`, `FuncionalidadesControladoresTest` | 32 |
| **Integración — persistencia** | Contra **PostgreSQL real**: migraciones Flyway V1–V6 (incluida la conversión del canal a `SMS` sin modificar la auditoría inmutable), Publicar Kudo completo en la base (Kudo, adjunto, outbox y auditoría), reserva de notificaciones una sola vez (`FOR UPDATE SKIP LOCKED`), *trigger* que impide modificar o borrar la auditoría, `CHECK` contra el autorreconocimiento aunque se salte el dominio, las 15 categorías del enunciado, feed por cursor, bloqueo de cuenta, revocación de tokens, registro, estructura, réplicas, calificaciones, reportes, vetos y limpieza. | Testcontainers (PostgreSQL), `@SpringBootTest` | `PersistenciaPublicarKudoIntegracionTest`, `PersistenciaFuncionalidadesIntegracionTest`, `KudosApplicationTests` | 18 |

**Último resultado (`./mvnw verify`):** 195 pruebas ejecutadas, **0 fallidas**. En una máquina sin Docker, las de Testcontainers se omiten (`disabledWithoutDocker`); en GitHub Actions se ejecutan todas.

**Cobertura (JaCoCo):** 70 % de instrucciones. Se excluyen de la medición `KudosApplication` y las clases `Propiedades*`, que solo leen configuración. El reporte `jacoco.xml` alimenta a SonarQube Cloud.

## 3. Front-end (`kudos-frontend`)

| Nivel | Qué verifica | Herramientas | Archivos de prueba | Pruebas |
|---|---|---|---|:-:|
| **Unitarias** | Servicio de sesión (guardar, refrescar, cerrar), interceptor (agrega el token, refresca ante 401, no toca rutas públicas), *guards* por sesión y rol, mensajes de error, *pipes*. | Vitest, `TestBed`, `HttpTestingController` | `sesion.service.spec.ts`, `autenticacion.interceptor.spec.ts`, `sesion.guards.spec.ts`, `mensaje-error.spec.ts`, `formato.pipes.spec.ts`, `rol-legible.pipe.spec.ts` | 21 |
| **Componentes** | Pantallas con la API simulada: iniciar sesión, feed (carga, vacío, error, scroll), detalle del Kudo, página no encontrada. | Vitest, `TestBed` | `iniciar-sesion.spec.ts`, `feed.spec.ts`, `detalle-kudo.spec.ts`, `pagina-no-encontrada.spec.ts`, `app.spec.ts` | 17 |
| **End-to-End** | El flujo obligatorio en un navegador real: iniciar sesión → publicar un Kudo → verlo en el feed → el destinatario conectado recibe el aviso Push. | Playwright | En construcción | — |

## 4. End-to-End del flujo obligatorio

Además de Playwright, el flujo completo se verifica sobre la **URL pública** con dos usuarios de demostración, y se guarda como evidencia:

| Paso | Qué se comprueba | Evidencia |
|---|---|---|
| 1. Almacenar | El Kudo aparece en el feed y en la tabla `kudo`. | Captura del feed y de la consulta en Supabase |
| 2. Correo | Llega el correo al destinatario. | Captura del correo |
| 3. Canal móvil (SMS) | Llega el SMS con el autor, la categoría y el enlace al Kudo. | Captura del celular |
| 4. Push | El destinatario conectado ve el aviso sin recargar. | Captura o video de las dos sesiones |
| 5. Auditoría | Las entradas de la publicación y de los tres canales con el mismo `id_correlacion`. | `GET /api/v1/auditoria?idCorrelacion=...` |

## 5. Cómo se ejecutan

| Qué | Comando | Cuándo |
|---|---|---|
| Todas las pruebas del back-end + cobertura | `./mvnw verify` | Local y en cada *push* (pipeline) |
| Pruebas del front-end | `npm test` | Local y en cada *push* (pipeline) |
| End-to-End | `npx playwright test` | Local contra el back-end y en el pipeline del front-end |

Las pruebas forman parte de la compuerta del pipeline: si una falla, no se construye la imagen ni se despliega (ver documento 18).
