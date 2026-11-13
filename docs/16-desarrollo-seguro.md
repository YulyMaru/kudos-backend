# 16 · Desarrollo Seguro — UCO Kudos



## 1. Autenticación

| Control | Implementación |
|---|---|
| Credenciales | Correo institucional y contraseña. Las contraseñas se guardan con **BCrypt de costo 12** (`CodificadorContrasenasBCrypt`); nunca en texto plano ni en logs. |
| Política de contraseñas | `PoliticaContrasena`: mínimo 12 caracteres y máximo 72 bytes (el límite real de BCrypt, para que no se trunque en silencio). |
| Bloqueo por fuerza bruta | `PoliticaBloqueo`: 5 intentos fallidos bloquean la cuenta 15 minutos; cada intento queda en la auditoría. |
| Token de acceso | JWT firmado con **HS256**, emisor `uco-kudos`, vigencia de **15 minutos** (`EmisorTokensJwt`). La API no guarda sesión (`SessionCreationPolicy.STATELESS`). |
| Token de refresco | Opaco, de 7 días, guardado solo como hash. **Se rota en cada uso** y, si alguien reutiliza uno ya rotado, se **revocan todas las sesiones** del usuario y se audita (`RefrescarSesionCasoUso`): detecta el robo de un token. |
| Cerrar sesión | `POST /api/v1/auth/cerrar-sesion` revoca el token de refresco. |
| Activación de la cuenta | Solo se activa con solicitud aprobada, **correo verificado** y **celular verificado** (enunciado, `ActivadorCuentas`). |
| Códigos de verificación | Generados con `SecureRandom`, guardados como SHA-256, vencen a los 15 minutos, intentos limitados y máximo 3 reenvíos por ventana (`VerificarContactoCasoUso`). |
| Restablecer contraseña | Enlace de un solo uso, de 30 minutos. La respuesta es siempre la misma exista o no la cuenta, para no revelar qué correos están registrados (`CuentaControlador`). |
| WebSocket | El `CONNECT` de STOMP exige `Authorization: Bearer`; sin token válido se rechaza (`AutenticacionStompInterceptor`). Cada usuario solo recibe en su propia cola `/user/queue/notificaciones`. |

## 2. Autorización

| Control | Implementación |
|---|---|
| RBAC por ruta | `SeguridadConfiguracion` restringe por rol: categorías (ADMINISTRADOR, RECTOR, COORDINADOR_ACADEMICO), auditoría y métricas (ADMINISTRADOR), reportes y vetos (RECTOR, COORDINADOR_ACADEMICO), facultades y programas (ADMINISTRADOR, COORDINADOR_ACADEMICO). Todo lo demás exige sesión. |
| Reglas de alcance en el dominio | La ruta no basta: `ReglaAprobacion` verifica que cada coordinador apruebe solo su nivel y su facultad o programa; `ReglaModeracion` impide que coordinadores de facultad, de programa, docentes y estudiantes veten; `GestionarVinculosCasoUso` limita a cada coordinador a su programa. Una violación lanza `AccesoDenegadoExcepcion` → 403. |
| Identidad desde el token | El autor de un Kudo, réplica, calificación o reporte se toma del JWT (`@AuthenticationPrincipal`), nunca del cuerpo de la petición. |
| Integridad organizacional | Índices únicos parciales en la base: un rector activo, un coordinador académico activo, un coordinador por facultad y por programa (V1, V5). `CHECK` contra el autorreconocimiento. |
| Adjuntos | El enlace de descarga se genera solo para usuarios autenticados y vence a los 10 minutos; el bucket es privado. |

## 3. Gestión de secretos

| Control | Implementación |
|---|---|
| Fuera del código | `application.properties` solo tiene marcadores `${...}`; ningún valor real está en el repositorio (Twelve-Factor III). |
| Producción | Los secretos (base de datos, JWT, SMTP, token de la pasarela SMS, WhatsApp, S3, administrador) están en **Google Secret Manager** y Cloud Run los inyecta (`--update-secrets`). Solo la cuenta de ejecución tiene el rol *Secret Accessor*. |
| Pipeline | GitHub Actions entra a Google Cloud con **Workload Identity Federation**: no existe ninguna llave de cuenta de servicio guardada. El único *secret* en GitHub es `SONAR_TOKEN`. |
| Detección | Trivy escanea el repositorio en busca de secretos en cada *push* y detiene el pipeline si encuentra uno. |
| Logs | Los DTO sensibles sobrescriben `toString()` (`RegistroSolicitud`, `SesionRespuesta`…) para no imprimir contraseñas ni tokens. |

## 4. Validaciones

| Control | Implementación |
|---|---|
| Entrada | El dominio valida siempre, sin depender del front: campos obligatorios (`CampoObligatorioExcepcion`), longitudes, correo institucional (`ReglaRegistro`, dominios configurables), rango de calificación 1–5, un Kudo no se da a uno mismo (`ReglaKudo`). |
| Adjuntos | `ReglaAdjunto`: máximo 5 archivos de 5 MB. El tipo se detecta por los **primeros bytes del archivo** (`DetectorTipoArchivoPorFirma`), no por la extensión ni el `Content-Type` del cliente; solo JPG, PNG y PDF. Se guarda con un nombre generado (UUID) y su SHA-256. |
| Path traversal | `AlmacenamientoArchivosLocal` normaliza la ruta y rechaza cualquier destino fuera del directorio base. |
| Inyección SQL | JPA y `JdbcClient` con parámetros enlazados; no se concatena SQL con datos del usuario. |
| XSS | El back-end devuelve JSON; Angular escapa todo lo que muestra por defecto y el front no usa `innerHTML` con contenido de usuarios. |

## 5. APIs

| Control | Implementación |
|---|---|
| Versionado | Todo bajo `/api/v1`. |
| Errores | `ManejadorErrores` responde con **ProblemDetail (RFC 9457)**; un error inesperado devuelve un mensaje genérico 500 y el detalle queda solo en el log, sin trazas de pila hacia el cliente. |
| CORS | Solo los orígenes de `CORS_ORIGENES` (el dominio de Firebase Hosting); lo mismo para el WebSocket. |
| CSRF | Deshabilitado de forma deliberada: la API no usa cookies, el token viaja en la cabecera `Authorization`, así que un sitio ajeno no puede enviarlo. |
| Exposición mínima | Actuator expone solo `health` (público) y `info` y `metrics` (solo ADMINISTRADOR). |
| Paginación | El feed y las réplicas usan cursor con `limite` acotado, para evitar respuestas enormes. |
| Tamaño de petición | `max-file-size=5MB`, `max-request-size=30MB`. |

## 6. Integraciones con terceros

| Tercero | Riesgo | Control |
|---|---|---|
| WhatsApp Cloud API (Meta) | Token filtrado; envío a números no verificados | Token permanente de usuario del sistema en Secret Manager; solo se envía a celulares **verificados** (si no, `NO_APLICA`); plantillas aprobadas por Meta; tiempos de conexión y lectura acotados. |
| Traccar SMS Gateway | Token filtrado; celular pasarela apagado | Token en Secret Manager (`kudos-sms-token`), nunca en logs ni en `toString`; si no hay celular válido, `NO_APLICA`; si la pasarela falla, el outbox reintenta y audita (ADR-015). |
| Brevo (SMTP) | Suplantación del remitente; spam | SMTP con TLS; remitente verificado; credenciales en Secret Manager. |
| Supabase (PostgreSQL y Storage) | Acceso a datos o archivos | Conexión con `sslmode=require`; bucket privado; enlaces firmados de 10 minutos; llaves S3 en Secret Manager. |
| Cualquier proveedor caído | Bloquear la publicación | Envío asíncrono por outbox: el usuario nunca espera a un tercero, y el fallo se reintenta y se audita. |
| Dependencias (Maven y npm) | Librerías vulnerables | Trivy escanea la imagen Docker y `package-lock.json`; Sonar revisa el código. Versiones administradas por Spring Boot. |

## 7. Riesgos de seguridad (OWASP Top 10)

| Riesgo | Mitigación en UCO Kudos | Riesgo residual |
|---|---|---|
| A01 Control de acceso roto | RBAC por ruta + reglas de alcance en el dominio + identidad tomada del JWT. | Bajo. |
| A02 Fallas criptográficas | BCrypt 12, SHA-256 para tokens y códigos, HTTPS obligatorio en Cloud Run y Firebase, TLS hacia la base de datos. | Bajo. La firma HS256 depende de un secreto fuerte (mínimo 256 bits). |
| A03 Inyección | Consultas parametrizadas; validación en el dominio; Angular escapa la salida. | Bajo. |
| A04 Diseño inseguro | Activación con doble verificación, aprobación jerárquica y bloqueo de cuenta. | Bajo. |
| A05 Configuración insegura | Actuator mínimo, CORS cerrado, contenedor sin privilegios (`USER kudos`), Trivy `misconfig`. | Bajo. |
| A06 Componentes vulnerables | Trivy bloquea vulnerabilidades CRITICAL con corrección disponible. | Medio: las HIGH solo se reportan. |
| A07 Fallas de autenticación | Bloqueo tras 5 intentos, rotación y detección de reutilización de tokens, códigos con vencimiento. | Bajo. |
| A08 Integridad de software y datos | Imagen construida y escaneada en el pipeline; auditoría append-only con *trigger*. | Bajo. |
| A09 Fallas de registro y monitoreo | Auditoría de cada acción, logs JSON con `traceId`, métricas de negocio. | Bajo. |
| A10 SSRF | El back-end solo llama a URL fijas de configuración (Meta, Brevo, Supabase); nunca a una URL enviada por el usuario. | Bajo. |

**Riesgos aceptados para la línea base:**
- **Sin límite de peticiones por IP.** Mitigado parcialmente por el bloqueo de cuenta y los límites de reenvío de códigos. Evolución: Cloud Armor o un filtro con Bucket4j.
- **El token de refresco se guarda en `localStorage` del navegador**, expuesto si hubiera un XSS. Mitigado por el escape de Angular, la cabecera `Content-Security-Policy` de Firebase Hosting y la rotación con detección de reutilización. Evolución: cookie `HttpOnly` `SameSite=Strict`.

## Controles de sesión en el front-end

| Control | Implementación |
|---|---|
| Cierre por inactividad | Tras un periodo sin actividad (clic, tecla, toque o desplazamiento) se muestra el aviso "Tu sesión está por expirar" con cuenta regresiva; si no hay respuesta, se cierra la sesión, se revoca el *refresh token* y se desconecta STOMP. Los tiempos se configuran por entorno. |
| Confirmación de cierre de sesión | El cierre manual pide confirmación para evitar cierres accidentales. |
| Celular con indicativo | El registro usa un selector de indicativo de país (+57 por defecto) y solo acepta dígitos; el número se envía en formato internacional, que es el que usa la pasarela SMS. |
