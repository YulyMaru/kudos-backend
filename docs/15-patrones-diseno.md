# 15 · Patrones de Diseño — UCO Kudos

Cada patrón se evalúa con los tres criterios del enunciado: **pertinencia** (qué problema real de UCO Kudos resuelve), **aplicación** (dónde y cómo está en el código) y **beneficio** (qué se gana).

## 1. Patrones arquitectónicos

| Patrón | Pertinencia | Aplicación | Beneficio |
|---|---|---|---|
| **Puertos y Adaptadores (Hexagonal)** | El negocio depende de cinco servicios externos (PostgreSQL, Supabase Storage, Brevo, pasarela SMS, WebSocket) que pueden cambiar. | Puertos en `aplicacion/puerto/entrada` y `aplicacion/puerto/salida`; adaptadores en `infraestructura/entrada` e `infraestructura/salida`; `configuracion` los ensambla. ArchUnit (`ReglasArquitecturaTest`) impide que el núcleo dependa de la infraestructura. | Cambiar de proveedor es cambiar un adaptador; los casos de uso se prueban sin base de datos ni red (ADR-001). |
| **Caso de Uso (Interactor)** | Cada funcionalidad del enunciado tiene un flujo propio con reglas y efectos. | Una clase `…CasoUso` por funcionalidad (`PublicarKudoCasoUso`, `DecidirSolicitudCasoUso`, `VetarContenidoCasoUso`…), que implementa su `…Puerto` de entrada. | El flujo de negocio queda en un solo lugar, legible y probable de forma aislada. |
| **CQRS ligero** | El feed se lee mucho más de lo que se publica (DRV-002). | Escritura por `Repositorio…` y el modelo de dominio; lectura por `Consultas…` (`persistencia/consulta`, `ConsultasKudosAdaptador`) que devuelven vistas planas (`KudoResumenVista`) sin cargar agregados. | Consultas rápidas y a la medida de la pantalla, sin complicar el modelo de escritura (ADR-009). |

## 2. Patrones de integración y mensajería

| Patrón | Pertinencia | Aplicación | Beneficio |
|---|---|---|---|
| **Transactional Outbox** | El Kudo y sus tres notificaciones deben guardarse juntos; si el correo o el SMS fallan, no se puede perder el aviso ni deshacer el Kudo. | `PublicarKudoCasoUso` inserta el Kudo, una fila por canal en `outbox_notificacion` y la auditoría en la misma transacción (`GestorTransacciones`). `ProcesadorOutboxProgramado` lee lotes con `FOR UPDATE SKIP LOCKED` y `ProcesarNotificacionesCasoUso` los envía. | Consistencia garantizada sin un *broker* adicional; un proveedor caído solo retrasa su canal (ADR-002). |
| **Consumidor Idempotente** | Un reintento o dos procesadores no deben enviar el mismo aviso dos veces. | Restricción única `(id_evento, canal)` en V1 y reserva de filas con `SKIP LOCKED`. | Sin duplicados aunque haya reintentos o varias instancias. |
| **Reintento con *backoff* exponencial y *jitter*** | Los proveedores externos fallan de forma transitoria. | `PoliticaReintentos` (dominio): máximo 5 intentos, retraso base de 10 s, tope de 10 min y variación aleatoria; luego `FALLIDO` y se audita. | Se recupera de fallos temporales sin saturar al proveedor. |
| **Evento de Dominio** | Publicar un Kudo dispara notificaciones y auditoría, que no son responsabilidad del Kudo. | `KudoPublicado` (`dominio/evento`) se procesa en `PublicarKudoCasoUso` para crear las notificaciones y la entrada de auditoría. | El Kudo no conoce los canales; agregar un canal no toca el modelo. |
| **Publicar-Suscribir (STOMP)** | El Push solo llega a quien está conectado. | `CanalPushWebSocket` publica en `/user/queue/notificaciones`; el front se suscribe con RxStomp. Si el usuario no está en `SimpUserRegistry`, la notificación queda `NO_APLICA`. | Aviso en tiempo real sin consultas periódicas (ADR-004). |

## 3. Patrones de diseño (GoF)

| Patrón | Pertinencia | Aplicación | Beneficio |
|---|---|---|---|
| **Adaptador** | Cada tecnología externa tiene su propia API. | `CanalCorreoSmtp`, `CanalSmsTraccar`, `CanalPushWebSocket`, `AlmacenamientoArchivosS3`, `EmisorTokensJwt`, `CodificadorContrasenasBCrypt` traducen la API externa al puerto del núcleo. | El núcleo habla un solo idioma, el de sus puertos. |
| **Estrategia** | Los tres canales se envían igual desde el procesador, pero cada uno de forma distinta. | Interfaz `CanalNotificacion`; `ProcesarNotificacionesCasoUso` recibe la lista de canales y los indexa en un `EnumMap<Canal, CanalNotificacion>`. Igual con `AlmacenamientoArchivos` (local o S3, elegido por `kudos.almacenamiento.tipo`). | Un canal nuevo es una clase nueva, sin `if` por canal (abierto/cerrado). |
| **Decorador** | Se necesitan métricas de negocio sin mezclar Micrometer con la auditoría. | `RegistroAuditoriaConMetricas` implementa `RegistroAuditoria`, delega en `RegistroAuditoriaAdaptador` y cuenta el evento solo después del *commit*. | Observabilidad agregada sin tocar el adaptador ni los casos de uso. |
| **Método Fábrica (estático)** | Un objeto de dominio debe nacer siempre válido, y reconstruirse desde la base sin repetir validaciones de creación. | `Kudo.publicar(...)` frente a `Kudo.reconstruir(...)`; `Notificacion.pendiente(...)` / `Notificacion.noAplica(...)`; `Usuario.registrar(...)`. Constructores privados. | Imposible crear un Kudo inválido; la intención queda en el nombre del método. |
| **Máquina de estados (State)** | Una notificación pasa por estados con transiciones permitidas. | `Notificacion` + `EstadoNotificacion`: `PENDIENTE → ENVIADO / REINTENTANDO → FALLIDO` o `NO_APLICA`; cada transición devuelve un objeto nuevo (inmutable). Igual con `EstadoAprobacion` en `SolicitudRegistro`. | No hay transiciones inválidas y el estado es trazable en la auditoría. |
| **Plantilla / Unidad de trabajo** | Varios casos de uso necesitan ejecutar pasos en una sola transacción sin depender de Spring. | Puerto `GestorTransacciones.enTransaccion(Runnable)`, implementado por `GestorTransaccionesAdaptador`. | El núcleo controla la transacción sin anotaciones de framework y se prueba con un doble. |
| **Política (Specification)** | Las reglas de negocio cambian y deben poder probarse solas. | `PoliticaReintentos`, `PoliticaBloqueo`, `PoliticaContrasena`, `ReglaKudo`, `ReglaAdjunto`, `ReglaAprobacion`, `ReglaModeracion`, `ReglaRegistro` (`dominio/regla`). | Cada regla en una clase pequeña con sus propias pruebas. |
| **Objeto de Transferencia (DTO)** | El modelo de dominio no debe salir por la API ni depender del JSON. | `record` inmutables: `…Comando`, `…Vista`, `…Resultado` (aplicación) y `…Solicitud`, `…Respuesta` (REST). | La API puede cambiar sin tocar el dominio, y viceversa. |

## 4. Patrones de persistencia

| Patrón | Pertinencia | Aplicación | Beneficio |
|---|---|---|---|
| **Repositorio** | El núcleo necesita guardar y recuperar agregados sin conocer SQL. | Puertos `RepositorioKudos`, `RepositorioUsuarios`, `RepositorioOutbox`…; implementados con JPA (`persistencia/adaptador`) o con SQL directo (`persistencia/jdbc`). | Persistencia intercambiable y casos de uso probables con dobles. |
| **Mapeador de Datos** | La entidad JPA y el modelo de dominio tienen necesidades distintas. | `KudoMapeador`, `UsuarioMapeador`, `NotificacionMapeador`… convierten entre `…Entidad` y el modelo. | El dominio queda libre de anotaciones JPA. |
| **Paginación por cursor (*keyset*)** | El feed crece sin límite; `OFFSET` se vuelve lento. | `CursorFeed` codifica `(fechaPublicacion, id)` en Base64 URL; la consulta usa el índice `(fecha_publicacion DESC, id DESC)`. | Costo constante por página, sin saltos ni duplicados al llegar Kudos nuevos. |
| **Registro de solo inserción (*append-only*)** | La auditoría no debe poder alterarse. | Tabla `registro_auditoria` protegida con un *trigger* que rechaza `UPDATE`, `DELETE` y `TRUNCATE` (V1). | Evidencia confiable para control institucional (ADR-013). |
| **Caché de datos de referencia (*cache-aside*)** | Las categorías se leen en cada publicación y casi no cambian. | Caché Caffeine `categorias-activas` (5 min) en el adaptador `ConsultasCategoriasAdaptador` (`@Cacheable`), fuera del núcleo. | Menos consultas a la base de datos en la ruta de lectura. |

## 5. Patrones del front-end

| Patrón | Pertinencia | Aplicación | Beneficio |
|---|---|---|---|
| **Interceptor (Cadena de responsabilidad)** | Toda petición a la API necesita el token y el mismo manejo de errores. | `core/http/autenticacion.interceptor.ts` agrega `Authorization: Bearer`, refresca ante un 401 y repite la petición. | Ningún componente maneja tokens por su cuenta. |
| **Guard** | Las rutas privadas y de administración deben protegerse por sesión y rol. | `core/guards/sesion.guards.ts`. | Control de acceso declarativo en las rutas. |
| **Observador** | La interfaz reacciona a la sesión, al feed y a los avisos Push. | *Signals* y `Observable` de RxJS en `SesionService`, `FeedService` y el servicio de notificaciones (RxStomp). | La vista se actualiza sola cuando cambian los datos. |
| **Carga diferida (*Lazy loading*)** | El 90 % de los usuarios entra desde el celular (DRV-001). | `loadComponent` en `app.routes.ts` por funcionalidad. | Primera carga más liviana y mejor puntaje en Lighthouse móvil. |

## 6. Patrones considerados y descartados

| Patrón | Por qué no se usó |
|---|---|
| *Broker* de mensajes (RabbitMQ, Pub/Sub) | El outbox sobre PostgreSQL da la misma garantía sin otro servicio que operar (ADR-002). |
| *Event Sourcing* | La auditoría append-only cubre la trazabilidad exigida; reconstruir estado desde eventos agrega complejidad sin un requisito que la justifique. |
| *Circuit Breaker* (Resilience4j) | Con el envío asíncrono y el *backoff*, un proveedor caído no afecta las peticiones del usuario; queda como evolución si crece el volumen. |
| Microservicios | Inviable para una persona en el plazo; los puertos permiten separar módulos después (ADR-001). |
