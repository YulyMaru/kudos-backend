# ADR-015: Entregar la notificación móvil por SMS mediante Traccar SMS Gateway

- **Título**: Implementar el canal móvil del flujo Publicar Kudo (hoy `Canal.SMS`) con mensajes SMS enviados por la pasarela Traccar SMS Gateway, detrás del puerto `CanalNotificacion`, en reemplazo de WhatsApp Cloud API (ADR-006, rechazado).

- **Estado**: Aceptado (2026-11-13). Validado con SPIKE-009.

- **Criterios de evaluación**:

  - **Resumen**: Elegir cómo notificar en el celular al destinatario de un Kudo, a cualquier número, el mismo día de la entrega, después de que Meta desactivara la cuenta de WhatsApp Business.

  - **Detalles**:

    - **Entrega a cualquier número**: el destinatario no debe registrarse ni unirse a un sandbox.
    - **Disponibilidad inmediata**: sin aprobaciones de plantillas, verificación empresarial ni revisiones de terceros.
    - **Costo**: $0 para la demostración.
    - **Cumplimiento y seguridad**: canal legítimo, sin automatizar la cuenta personal de WhatsApp y sin desactivar protecciones del dispositivo.
    - **Bajo acoplamiento**: solo un adaptador nuevo; el dominio, el caso de uso y el outbox no cambian.
    - **Escenarios de calidad relacionados**: ESC-CAL-DIS-034 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-20.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cinco opciones.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Traccar SMS Gateway** (app de Play Store que convierte un Android en pasarela SMS, con API en la nube).
    2. **SMSGate / SMS Gateway for Android** (pasarela de código abierto distribuida como APK).
    3. **Twilio SMS** (proveedor SMS empresarial).
    4. **Brevo SMS** (mismo proveedor del correo, ADR-005).
    5. **Bot de Telegram**.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisó la documentación de cada opción y se probaron en el mismo día las dos pasarelas Android.

  - **1. Traccar SMS Gateway**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Se instala desde Play Store, por lo que Android concede el permiso de SMS sin restricciones.
        - Modo nube: el back-end hace `POST https://www.traccar.org/sms/` con el token del dispositivo en el encabezado `Authorization` y el cuerpo `{"to": "+57…", "message": "…"}`; el servidor de Traccar despierta el celular por notificación push y el celular envía el SMS con su línea.
        - El celular no necesita IP pública ni estar en la misma red que Cloud Run.
        - Código abierto y mantenido por el autor de Traccar.

    - **Análisis de costos**:

      - **Resumen**: $0; el SMS lo cubre el plan del celular.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: muy baja (una petición HTTP).
        - **Operación**: mantener el celular encendido, con internet y sin restricción de batería para la app; el token se guarda en Secret Manager.
        - **Medición**: respuesta `success` del servidor de Traccar y estado del outbox.

    - **Análisis FODA**:

      - **Fortalezas**: inmediato; a cualquier número; sin aprobaciones; gratuito.
      - **Debilidades**: depende de un celular físico; el SMS sale desde un número personal; sin confirmación de entrega al destinatario.
      - **Oportunidades**: reemplazarlo por un proveedor SMS empresarial o por WhatsApp Business verificado cambiando solo el adaptador.
      - **Amenazas**: el celular se apaga o el sistema cierra la app; límites de SMS del operador.

    - **Opiniones y comentarios internos**:

      - Es la única opción que cumple todos los criterios el día de la entrega.

  - **2. SMSGate (APK de código abierto)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - No está en Play Store; Google Play Protect bloqueó la instalación y Android 13+ restringió el permiso de SMS por ser una app de origen externo.
        - Habilitarla exigía desactivar Play Protect y los ajustes restringidos, lo que contradice el criterio de seguridad.

    - **Análisis de costos**:

      - **Resumen**: $0, con un costo de seguridad inaceptable.

      - **Ejemplos**:

        - **Licencias**: Apache 2.0.
        - **Capacitación**: baja.
        - **Operación**: desactivar protecciones del sistema.
        - **Medición**: API propia.

    - **Análisis FODA**:

      - **Fortalezas**: API completa; varios dispositivos.
      - **Debilidades**: instalación fuera de la tienda.
      - **Oportunidades**: servidor privado propio.
      - **Amenazas**: dispositivo sin protecciones activas.

    - **Opiniones y comentarios internos**:

      - Descartada para no debilitar la seguridad del dispositivo.

  - **3. Twilio SMS**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - En la cuenta de prueba solo envía a números verificados uno a uno; enviar a cualquier número requiere pagar la cuenta.
        - SDK oficial y alta confiabilidad.

    - **Análisis de costos**:

      - **Resumen**: Crédito de prueba limitado; luego pago por número y por mensaje.

      - **Ejemplos**:

        - **Licencias**: pago por uso.
        - **Capacitación**: baja.
        - **Operación**: verificar cada destinatario en la cuenta de prueba.
        - **Medición**: consola de Twilio.

    - **Análisis FODA**:

      - **Fortalezas**: proveedor empresarial.
      - **Debilidades**: no llega a cualquier número sin pagar.
      - **Oportunidades**: opción de producción.
      - **Amenazas**: costo recurrente.

    - **Opiniones y comentarios internos**:

      - Se deja como alternativa de producción.

  - **4. Brevo SMS**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple el criterio de costo.

      - **Detalles**:

        - Requiere comprar créditos de SMS; reutilizaría el proveedor ya integrado para correo.

    - **Análisis de costos**:

      - **Resumen**: Pago por créditos.

      - **Ejemplos**:

        - **Licencias**: créditos prepagados.
        - **Capacitación**: baja.
        - **Operación**: recargar créditos.
        - **Medición**: panel de Brevo.

    - **Análisis FODA**:

      - **Fortalezas**: un solo proveedor para correo y SMS.
      - **Debilidades**: costo.
      - **Oportunidades**: producción.
      - **Amenazas**: agotar créditos.

    - **Opiniones y comentarios internos**:

      - Viable con presupuesto; no para la línea base.

  - **5. Bot de Telegram**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Gratuito y oficial, pero el destinatario debe instalar Telegram e iniciar el bot; no llega al número de celular como tal.

    - **Análisis de costos**:

      - **Resumen**: $0.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: baja.
        - **Operación**: cada usuario debe vincular su chat.
        - **Medición**: API de Telegram.

    - **Análisis FODA**:

      - **Fortalezas**: gratuito; API sencilla.
      - **Debilidades**: requiere acción previa del destinatario.
      - **Oportunidades**: canal adicional opcional.
      - **Amenazas**: baja adopción.

    - **Opiniones y comentarios internos**:

      - No cumple el objetivo de notificar al número del destinatario.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con el SPIKE-009.

  - **¿Quién da la opinión?**:

    - SPIKE-009 — Prueba desde Cloud Shell contra la API en la nube de Traccar y prueba de punta a punta desde Cloud Run — Aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - SMSGate (bloqueado por Play Protect), Twilio SMS y Brevo SMS (costo), Telegram (requiere acción del destinatario).

  - **¿Qué estás creando?**:

    - El canal móvil del flujo Publicar Kudo y de los códigos de verificación de celular.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios de entrega a cualquier número, disponibilidad inmediata, costo y seguridad, y con pruebas reales de envío.

  - **¿Por qué elegiste al ganador?**:

    Es el único que entrega a cualquier número el mismo día, gratis, desde una app de la tienda oficial y sin tocar el núcleo de la aplicación.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: la prueba devolvió `success: true` y el SMS llegó a un número externo en segundos.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de las notificaciones móviles.
    - **¿Qué tipos de integraciones están involucradas?**: API HTTPS de Traccar, procesador de outbox (ADR-002), auditoría (ADR-013) y Secret Manager (ADR-010).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Tener desde el inicio un canal de respaldo que no dependa de aprobaciones de terceros.

  - **Anécdotas**:

    - El canal se cambió cuatro veces de proveedor (Meta, Twilio, WATI y SMS) sin modificar el dominio ni el caso de uso: es la mejor evidencia del valor de Puertos y Adaptadores (ADR-001).

- **Recomendación**:

  - **Resumen**:

    Usar **Traccar SMS Gateway** con el adaptador `CanalSmsTraccar` como único adaptador del canal móvil.

  - **Detalles**:

    - Mensaje corto en español con el autor, la categoría y el enlace al Kudo; los códigos de verificación usan el mismo canal.
    - Número en formato E.164 tomado del celular del destinatario; sin celular, el canal queda `NO_APLICA`.
    - URL por `SMS_TRACCAR_URL` y token por `SMS_TRACCAR_TOKEN` desde Secret Manager (`kudos-sms-token`); el token nunca aparece en logs.
    - Respuestas distintas de 2xx se tratan como error reintentable por el outbox.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-009, aprobado.
  - **Qué se validó**: envío desde Cloud Shell a un número externo con respuesta `success: true` y SMS recibido; adaptador con pruebas automatizadas (éxito, error HTTP, sin celular y código de verificación) y selección por propiedad.
  - **Cambios respecto a lo propuesto**: después de validar, se hizo una limpieza: el canal pasó de `Canal.WHATSAPP` a `Canal.SMS` con la migración Flyway V6 (actualiza el outbox y su restricción `CHECK`), se eliminaron `CanalWhatsappCloudApi`, sus propiedades y la variable `WHATSAPP_PROVEEDOR`. La auditoría histórica **no se modifica** (es de solo inserción, ADR-013): sus registros antiguos conservan `WHATSAPP` y la consulta los interpreta como `SMS`. Una prueba de integración con PostgreSQL real valida la migración.
  - **Limitaciones aceptadas**: el celular pasarela debe estar encendido, con internet y con la app activa durante la demostración; el SMS sale desde un número personal. En producción se reemplazaría por un proveedor SMS empresarial o por WhatsApp Business verificado, cambiando solo el adaptador.
