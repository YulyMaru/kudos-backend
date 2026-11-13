# ADR-002: Procesar las notificaciones de forma asíncrona con eventos de dominio y Transactional Outbox

- **Título**: Desacoplar la publicación del Kudo del envío de notificaciones mediante eventos de dominio persistidos en una tabla *outbox* y procesados de forma asíncrona, con reintentos y backoff exponencial por canal.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-001.

- **Criterios de evaluación**:

  - **Resumen**: Evaluar cómo ejecutar los pasos posteriores a la publicación de un Kudo (correo, WhatsApp, Push y auditoría) de forma que el autor reciba una respuesta rápida, ninguna notificación se pierda y la falla de un canal no afecte a los demás.

  - **Detalles**:

    - **Responsividad**: la respuesta al autor no debe depender de la latencia de los proveedores externos.
    - **Confiabilidad**: si el Kudo se guardó, sus notificaciones deben enviarse; si no se guardó, no se debe enviar ninguna.
    - **Resiliencia**: reintentos ante errores transitorios y aislamiento de fallas por canal.
    - **Prevención de duplicados**: un reintento no debe generar mensajes repetidos al destinatario (idempotencia por evento y canal).
    - **Trazabilidad**: cada envío y su resultado deben quedar auditados (HU-22).
    - **Costo**: sin infraestructura adicional de pago.
    - **Escenarios de calidad relacionados**: ESC-CAL-CON-041, ESC-CAL-CON-042, ESC-CAL-DIS-034 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-17, HU-19, HU-20, HU-21 y HU-22.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron tres enfoques para orquestar las notificaciones.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Envío síncrono** dentro de la misma petición de publicación.
    2. **Eventos de dominio + Transactional Outbox + procesamiento asíncrono** dentro de la misma aplicación.
    3. **Broker de mensajes externo** (RabbitMQ o Kafka) con un consumidor por canal.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron el Manifiesto Reactivo (orientación a mensajes), el patrón *Transactional Outbox*, *Retry with Backoff*, *Circuit Breaker* (Resilience4j), los eventos de Spring (`@TransactionalEventListener`) y la experiencia de AgroTrack (ADR-001 de ese proyecto: idempotencia por identificador de operación y backoff con *jitter*).

  - **1. Envío síncrono**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - El tiempo de respuesta suma las latencias de tres proveedores.
        - Si WhatsApp falla, la publicación falla o queda en un estado inconsistente.
        - No hay reintentos sin bloquear al usuario.

    - **Análisis de costos**:

      - **Resumen**: Costo de implementación bajo y costo de fallas alto.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: mínima.
        - **Operación**: incidentes frecuentes cuando un proveedor degrada.
        - **Medición**: latencia de la publicación.

    - **Análisis FODA**:

      - **Fortalezas**: simple; fácil de depurar.
      - **Debilidades**: acoplamiento temporal; mala experiencia de usuario.
      - **Oportunidades**: ninguna relevante.
      - **Amenazas**: pérdida de notificaciones; caídas en cascada.

    - **Opiniones y comentarios internos**:

      - Contradice directamente los principios de responsividad y resiliencia del Manifiesto Reactivo que se deben sustentar.

  - **2. Eventos de dominio + Transactional Outbox + procesamiento asíncrono**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios con la menor infraestructura posible.

      - **Detalles**:

        - Al publicar, el Kudo y un registro por canal (`EMAIL`, `WHATSAPP`, `PUSH`) se guardan en la tabla `outbox` **en la misma transacción**: o se guarda todo o nada.
        - El autor recibe la respuesta (201) en cuanto termina la transacción.
        - Un procesador asíncrono (`@Scheduled` + hilos virtuales de Java 25) toma los registros pendientes y los entrega al adaptador del canal.
        - Cada canal tiene su propio estado, contador de intentos y próxima fecha de reintento: la falla de WhatsApp no detiene el correo.
        - Reintentos con backoff exponencial y *jitter*; al agotarlos, el registro pasa a `FALLIDO` y queda auditado.
        - La clave idempotente `idEvento + canal` evita reenvíos duplicados.
        - Se puede añadir un Circuit Breaker (Resilience4j) por proveedor si las fallas son prolongadas.

    - **Análisis de costos**:

      - **Resumen**: Costo de implementación medio y costo operativo bajo (usa la misma base de datos).

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: patrón Outbox y procesamiento asíncrono en Spring.
        - **Operación**: monitorear los registros pendientes y fallidos del outbox.
        - **Medición**: notificaciones enviadas y fallidas por canal, intentos promedio y tiempo desde la publicación hasta la entrega.

    - **Análisis FODA**:

      - **Fortalezas**: no pierde notificaciones; aísla fallas; sin infraestructura extra; trazable.
      - **Debilidades**: *polling* sobre la base de datos; requiere bloqueo de filas (`FOR UPDATE SKIP LOCKED`) si hay varias instancias.
      - **Oportunidades**: el outbox puede publicar más adelante hacia un broker externo sin cambiar los casos de uso.
      - **Amenazas**: un backoff mal configurado podría saturar un proveedor; una tabla outbox sin limpieza crece indefinidamente.

    - **Opiniones y comentarios internos**:

      - Es el mismo aprendizaje de AgroTrack: el riesgo principal no es reintentar, sino reintentar sin duplicar. La idempotencia es parte central del diseño.

  - **3. Broker de mensajes externo (RabbitMQ / Kafka)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple técnicamente, pero agrega infraestructura y un problema nuevo: la escritura dual entre la base de datos y el broker.

      - **Detalles**:

        - Guardar el Kudo y publicar en el broker no son atómicos sin un Outbox, así que el Outbox se necesitaría de todos modos.
        - Los servicios gestionados gratuitos tienen límites estrictos y otro proveedor que administrar.
        - Kafka es excesivo para el volumen esperado.

    - **Análisis de costos**:

      - **Resumen**: Costo más alto de implementación y de operación.

      - **Ejemplos**:

        - **Licencias**: código abierto, pero el hosting gestionado tiene límites o costo.
        - **Capacitación**: configuración del broker, colas de mensajes fallidos (DLQ).
        - **Operación**: un componente más que desplegar, asegurar y monitorear.
        - **Medición**: profundidad de las colas y retraso de los consumidores.

    - **Análisis FODA**:

      - **Fortalezas**: escalado independiente de consumidores; DLQ nativas.
      - **Debilidades**: complejidad; escritura dual.
      - **Oportunidades**: base para extraer microservicios.
      - **Amenazas**: más superficie de ataque y de falla; plazos.

    - **Opiniones y comentarios internos**:

      - Queda como evolución natural documentada, no como necesidad de la línea base.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-001 — Validará que al publicar un Kudo se guardan atómicamente el Kudo y sus tres registros de outbox, que una falla simulada de un canal no afecta a los otros y que N reintentos generan exactamente 1 mensaje por canal — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Envío síncrono, RabbitMQ, Kafka y `@Async` sin persistencia (descartado porque pierde los eventos si la aplicación se reinicia).

  - **¿Qué estás creando?**:

    - Un flujo de publicación con notificación multicanal para una plataforma institucional web Mobile First.
    - **Monolito o microservicios**: una sola unidad desplegable organizada con Puertos y Adaptadores (ADR-001).

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores y, en la validación, con el SPIKE-001, que provoca fallas en los adaptadores para medir los reintentos y los duplicados.

  - **¿Por qué elegiste al ganador?**:

    Garantiza que ninguna notificación se pierda y que la publicación sea rápida, sin agregar componentes de infraestructura. Materializa la orientación a mensajes y la resiliencia del Manifiesto Reactivo.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir en la línea base.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de las notificaciones generadas por la publicación de Kudos.
    - **¿Qué tipos de integraciones están involucradas?**: PostgreSQL (outbox), adaptadores de correo (ADR-005), WhatsApp (ADR-006) y WebSocket (ADR-004), y el módulo de auditoría (ADR-013).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Empezar con Outbox y medir; introducir un broker solo cuando el volumen o la necesidad de consumidores independientes lo justifique.

  - **Anécdotas**:

    - El canal Push es distinto a los otros dos: solo tiene sentido si el usuario está conectado. Si no lo está, el registro se marca como `NO_APLICA` en lugar de reintentarse, y el Kudo aparece en el feed en su próxima visita.

- **Recomendación**:

  - **Resumen**:

    Implementar **eventos de dominio + Transactional Outbox + procesamiento asíncrono con reintentos y backoff** para las notificaciones.

  - **Detalles**:

    - Evento de dominio `KudoPublicado` emitido por el agregado `Kudo`.
    - Tabla `outbox_notificacion` (id, id_evento, canal, destinatario, payload, estado, intentos, proximo_intento, ultimo_error, creado_en).
    - Estados: `PENDIENTE`, `ENVIADO`, `REINTENTANDO`, `FALLIDO`, `NO_APLICA`.
    - Máximo 5 intentos por canal, con backoff exponencial y jitter.
    - Puerto `CanalNotificacion` con un adaptador por proveedor (patrón Strategy).
    - Toma de lotes con `SELECT … FOR UPDATE SKIP LOCKED` para soportar varias instancias.
    - Auditoría de cada intento y de su resultado.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-001, aprobado.
  - **Qué se validó**: El Kudo y sus filas de outbox se guardan en la misma transacción; el procesador programado entrega cada canal con reintentos y *backoff* (`FOR UPDATE SKIP LOCKED`). En producción, mientras el proveedor de WhatsApp fallaba, el Kudo se publicó y el correo y el Push se entregaron; la fila del canal móvil quedó reintentando y luego `FALLIDO`, sin afectar a los demás canales.
  - **Cambios respecto a lo propuesto**: Ninguno.
