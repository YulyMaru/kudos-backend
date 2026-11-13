# 12 · Manifiesto Reactivo — UCO Kudos

| Propiedad | Cómo la aborda UCO Kudos | Mecanismos | Escenario / ADR |
|---|---|---|---|
| **Responsividad** | El sistema responde en un tiempo corto y consistente, aun cuando los proveedores externos son lentos. | Publicación desacoplada de las notificaciones; feed con paginación por cursor, índices y caché; respuestas comprimidas. | ESC-CAL-REN-011 · ADR-002, ADR-009 |
| **Resiliencia** | El sistema sigue respondiendo ante fallas: la falla de un componente queda aislada. | Un registro de outbox por canal; reintentos con backoff exponencial e idempotencia; reemplazo automático de instancias; verificaciones de salud. | ESC-CAL-CON-041, ESC-CAL-DIS-032 · ADR-002, ADR-010 |
| **Elasticidad** | El sistema ajusta su capacidad a la carga. | Back-end sin estado (JWT) en Cloud Run, que agrega y quita instancias; outbox con `FOR UPDATE SKIP LOCKED` para procesar con varias instancias. | ESC-CAL-ESC-050 · ADR-001, ADR-010 |
| **Orientación a mensajes** | Los componentes se comunican mediante mensajes asíncronos, sin esperar la respuesta del otro. | Evento de dominio `KudoPublicado`; tabla outbox como cola persistente; notificaciones Push por WebSocket/STOMP. | ADR-002, ADR-004 |

---

## 1. Responsividad

- Al publicar un Kudo, el sistema guarda el Kudo y sus notificaciones pendientes en una transacción y **responde de inmediato** (201). El correo, el SMS y el Push se envían después.
- Así, si el proveedor del canal móvil tarda 5 segundos, el autor no espera esos 5 segundos.
- El feed, la operación más frecuente (DRV-002), mantiene un tiempo de respuesta estable gracias a la paginación por cursor: la página 400 tarda lo mismo que la primera.
- **Meta medible:** publicación < 1 s y feed < 500 ms (p95) con 200 usuarios concurrentes.

## 2. Resiliencia

- **Aislamiento:** cada canal tiene su propio registro en el outbox. Si falla el correo, el SMS y el Push no se enteran. Esto se comprobó en producción: mientras WhatsApp (Meta) fallaba, el correo y el Push se entregaron.
- **Recuperación:** las fallas transitorias se reintentan hasta 5 veces con backoff exponencial y *jitter*. Si se agotan, la notificación queda `FALLIDA` y auditada; nunca se pierde en silencio.
- **Durabilidad:** como el outbox está en la base de datos, un reinicio de la aplicación no pierde notificaciones pendientes.
- **Idempotencia:** la clave `idEvento + canal` evita que un reintento envíe dos veces el mismo mensaje.
- **Supervisión:** Cloud Run reemplaza una instancia que falla; las sondas de salud evitan enviarle tráfico mientras no está lista.

## 3. Elasticidad

- El back-end no guarda sesiones (usa JWT) y los archivos están en el almacenamiento de objetos, así que cualquier instancia puede atender cualquier petición.
- Cloud Run aumenta o reduce el número de instancias según la carga.
- El procesador del outbox toma lotes con `FOR UPDATE SKIP LOCKED`, de modo que varias instancias pueden procesar notificaciones sin duplicarlas.
- **Limitación consciente de la línea base:** el broker WebSocket en memoria obliga a usar una sola instancia (`max-instances=1`). La evolución documentada es un *broker relay* (RabbitMQ con STOMP o Redis Pub/Sub), sin cambiar los casos de uso (ADR-004).

## 4. Orientación a mensajes

- `PublicarKudo` no llama directamente a los canales: emite el evento `KudoPublicado` y deja un mensaje por canal en el outbox.
- El procesador consume esos mensajes de forma asíncrona y los entrega al adaptador correspondiente.
- La notificación Push llega al navegador como mensaje STOMP por WebSocket, sin que el cliente tenga que consultar.
- El outbox puede publicar más adelante en un broker externo (RabbitMQ o Kafka) sin cambiar los casos de uso.

---

## 5. Decisión: arquitectura reactiva sin programación reactiva

UCO Kudos aplica el Manifiesto Reactivo **a nivel de arquitectura** (mensajes, aislamiento, elasticidad), pero usa **Spring MVC** y no Spring WebFlux.

| Opción | Ventaja | Desventaja |
|---|---|---|
| Spring MVC + hilos virtuales de Java 25 | Código imperativo, sencillo de leer, probar y cambiar en la sustentación; los hilos virtuales manejan muchas peticiones concurrentes con bajo costo. | Las llamadas a la base de datos son bloqueantes (mitigado con hilos virtuales). |
| Spring WebFlux (programación reactiva) | No bloqueante de punta a punta. | Mayor curva de aprendizaje, depuración más difícil y drivers reactivos adicionales; no aporta un beneficio proporcional para la carga esperada. |

