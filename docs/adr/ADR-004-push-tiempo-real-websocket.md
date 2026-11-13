# ADR-004: Implementar notificaciones Push en tiempo real con WebSocket y STOMP

- **Título**: Entregar las notificaciones Push a los usuarios conectados mediante WebSocket con el protocolo STOMP (Spring WebSocket), autenticadas con el mismo JWT de la API.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-002.

- **Criterios de evaluación**:

  - **Resumen**: Elegir el mecanismo con el que un destinatario conectado a la aplicación recibe en pocos segundos la notificación de un nuevo Kudo, sin recargar la página, solo él y de forma segura.

  - **Detalles**:

    - **Latencia**: la notificación debe llegar en pocos segundos tras la publicación.
    - **Direccionamiento**: el mensaje debe llegar solo al destinatario (canal privado por usuario).
    - **Seguridad**: la conexión debe autenticarse con el JWT; nadie puede suscribirse al canal de otra persona.
    - **Compatibilidad móvil**: debe funcionar en navegadores móviles (DRV-001).
    - **Costo y complejidad**: $0 y sin servicios externos obligatorios.
    - **Escalabilidad**: camino claro para operar con varias instancias.
    - **Escenarios de calidad relacionados**: ESC-CAL-REN-013 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-21.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro mecanismos de entrega en tiempo real.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Polling periódico** desde el front-end.
    2. **Server-Sent Events (SSE)**.
    3. **WebSocket con STOMP** (Spring WebSocket + cliente STOMP en Angular).
    4. **Web Push con Firebase Cloud Messaging (FCM)** y *service worker*.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron la documentación de Spring WebSocket (destinos de usuario `/user/queue/...`), SSE con `SseEmitter`, el soporte de WebSocket en Cloud Run y la experiencia de AgroTrack con FCM (ADR-028 de ese proyecto).

  - **1. Polling periódico**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple. No es tiempo real y genera tráfico innecesario.

      - **Detalles**:

        - La latencia depende del intervalo de consulta.
        - Multiplica las peticiones de lectura y consume datos y batería en el móvil.

    - **Análisis de costos**:

      - **Resumen**: Implementación trivial; costo de infraestructura proporcional al número de usuarios conectados.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: ninguna.
        - **Operación**: carga constante sobre la API.
        - **Medición**: peticiones por minuto.

    - **Análisis FODA**:

      - **Fortalezas**: simple; sin conexiones persistentes.
      - **Debilidades**: no es tiempo real; desperdicio de recursos.
      - **Oportunidades**: mecanismo de respaldo.
      - **Amenazas**: sobrecarga de la API con muchos usuarios.

    - **Opiniones y comentarios internos**:

      - No satisface el requisito de notificación «en tiempo real».

  - **2. Server-Sent Events (SSE)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Es unidireccional (servidor → cliente), suficiente para notificaciones.
        - `EventSource` nativo no permite enviar la cabecera `Authorization`; requiere pasar el token en la URL o usar una librería.
        - El registro de qué emisor pertenece a qué usuario debe programarse a mano.

    - **Análisis de costos**:

      - **Resumen**: Bajo.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: baja.
        - **Operación**: conexiones HTTP de larga duración.
        - **Medición**: emisores activos.

    - **Análisis FODA**:

      - **Fortalezas**: simple; sobre HTTP estándar; reconexión automática.
      - **Debilidades**: autenticación incómoda; enrutamiento por usuario manual.
      - **Oportunidades**: alternativa liviana si WebSocket presentara problemas en el hosting.
      - **Amenazas**: token expuesto en la URL si se pasa como parámetro.

    - **Opiniones y comentarios internos**:

      - Es una buena alternativa de respaldo.

  - **3. WebSocket con STOMP**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios principales.

      - **Detalles**:

        - Spring ofrece destinos privados por usuario (`convertAndSendToUser(usuario, "/queue/notificaciones", …)`).
        - El JWT se valida en el mensaje `CONNECT` mediante un interceptor de canal, y el principal queda asociado a la sesión.
        - Spring sabe qué usuarios están conectados (`SimpUserRegistry`): si el destinatario no lo está, el registro de outbox se marca `NO_APLICA` (ADR-002).
        - Cloud Run soporta conexiones WebSocket.
        - En Angular se integra con un cliente STOMP (`@stomp/rx-stomp`) y servicios reactivos (RxJS).

    - **Análisis de costos**:

      - **Resumen**: Costo de implementación medio-bajo y sin servicios externos.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: STOMP y destinos de usuario en Spring.
        - **Operación**: conexiones persistentes; con varias instancias se necesita un *broker relay* compartido.
        - **Medición**: sesiones activas y mensajes enviados.

    - **Análisis FODA**:

      - **Fortalezas**: tiempo real; canal privado por usuario; integrado con Spring Security.
      - **Debilidades**: conexiones persistentes; con varias instancias, el broker en memoria no comparte sesiones.
      - **Oportunidades**: reutilizar el canal para contadores en vivo, réplicas y calificaciones.
      - **Amenazas**: los cortes de conexión en redes móviles exigen reconexión automática; mayor superficie de ataque.

    - **Opiniones y comentarios internos**:

      - Es la opción mejor integrada con el stack (Spring + Angular) y con la seguridad basada en JWT.

  - **4. Web Push con FCM**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple, pero su objetivo principal son los usuarios **no conectados**, y el requisito es para usuarios conectados.

      - **Detalles**:

        - Requiere *service worker*, permisos del navegador y un proyecto de Firebase.
        - En iOS, Web Push solo funciona si la PWA está instalada en la pantalla de inicio.
        - Ya se cubre la notificación a usuarios no conectados por correo y WhatsApp.

    - **Análisis de costos**:

      - **Resumen**: Gratuito, pero con más configuración y dependencia de un tercero.

      - **Ejemplos**:

        - **Licencias**: gratuito.
        - **Capacitación**: service workers y FCM.
        - **Operación**: gestión de tokens de dispositivo.
        - **Medición**: entregas de FCM.

    - **Análisis FODA**:

      - **Fortalezas**: llega aunque la app esté cerrada.
      - **Debilidades**: permisos; limitaciones en iOS; dependencia externa.
      - **Oportunidades**: evolución futura para usuarios desconectados.
      - **Amenazas**: el usuario puede rechazar los permisos.

    - **Opiniones y comentarios internos**:

      - Queda como evolución si la universidad quiere avisos con la aplicación cerrada.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-002 — Validará la conexión WebSocket/STOMP desde Angular hasta Cloud Run autenticada con JWT, la entrega solo al destinatario y la reconexión automática tras un corte — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Polling, SSE y FCM.

  - **¿Qué estás creando?**:

    - Notificaciones en tiempo real dentro de una aplicación web Mobile First, para usuarios con sesión abierta.

  - **¿Cómo evaluó a los candidatos?**:

    Frente a los criterios de latencia, direccionamiento, seguridad y costo, y en la validación con el SPIKE-002 en el entorno desplegado.

  - **¿Por qué elegiste al ganador?**:

    WebSocket/STOMP ofrece tiempo real, canales privados por usuario y autenticación con el mismo JWT, sin servicios externos y con soporte nativo en Spring y en Cloud Run.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de las notificaciones Push.
    - **¿Qué tipos de integraciones están involucradas?**: Procesador de outbox (ADR-002), Spring Security (ADR-008) y cliente STOMP en Angular.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Limitar la línea base a una instancia (o usar afinidad de sesión en Cloud Run) y documentar el *broker relay* para escalar.

  - **Anécdotas**:

    - La palabra «Push» suele asociarse a FCM, pero el enunciado pide notificar «si el destinatario se encuentra conectado a la aplicación». Para ese caso, un canal WebSocket es más directo y no depende de permisos del navegador.

- **Recomendación**:

  - **Resumen**:

    Implementar Push en tiempo real con **WebSocket + STOMP** en Spring y **RxStomp** en Angular.

  - **Detalles**:

    - Endpoint `/ws`; destino privado `/user/queue/notificaciones`.
    - Validación del JWT en el `CONNECT` mediante `ChannelInterceptor`.
    - Consulta de `SimpUserRegistry` para saber si el destinatario está conectado.
    - Reconexión automática con backoff en el cliente.
    - Línea base con una instancia; evolución con un broker relay (por ejemplo, RabbitMQ con plugin STOMP) o Redis Pub/Sub.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-002, aprobado.
  - **Qué se validó**: La notificación Push llega en tiempo real solo al destinatario conectado, autenticado con JWT en el `CONNECT` de STOMP (`AutenticacionStompInterceptor`), y se muestra sin recargar la página en la aplicación publicada.
  - **Cambios respecto a lo propuesto**: Ninguno.
