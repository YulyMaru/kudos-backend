# ADR-006: Enviar notificaciones de WhatsApp mediante WhatsApp Cloud API de Meta

- **Título**: Implementar el canal de WhatsApp con la API oficial WhatsApp Cloud API (Meta) usando el número de prueba y una plantilla de mensaje, detrás del puerto `CanalNotificacion`, con el sandbox de Twilio como alternativa.

- **Estado**: Rechazado (2026-11-13). Propuesto el 2026-11-09; el SPIKE-004 no pudo cumplir sus criterios por las condiciones del proveedor. Reemplazado por [ADR-015](ADR-015-canal-movil-sms-traccar.md).

- **Criterios de evaluación**:

  - **Resumen**: Elegir cómo enviar un mensaje de WhatsApp al destinatario de un Kudo de forma legal, confiable y con costo $0 durante la línea base.

  - **Detalles**:

    - **Cumplimiento**: usar canales oficiales que respeten las políticas de WhatsApp (consentimiento del destinatario y plantillas aprobadas).
    - **Costo**: $0 para la línea base y la demostración.
    - **Facilidad de demostración**: el docente debe poder recibir el mensaje durante la sustentación.
    - **Bajo acoplamiento**: cambiar de proveedor sin modificar los casos de uso.
    - **Resiliencia**: manejo de errores y límites de tasa del proveedor.
    - **Escenarios de calidad relacionados**: ESC-CAL-DIS-034 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-20.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron tres opciones.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **WhatsApp Cloud API (Meta)**, API oficial alojada por Meta.
    2. **Twilio API for WhatsApp** (sandbox).
    3. **Librerías no oficiales** que automatizan WhatsApp Web.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron la documentación de WhatsApp Business Platform (número de prueba, plantillas, ventana de atención de 24 horas) y la del sandbox de Twilio. Las condiciones exactas del entorno de pruebas se confirmarán en el SPIKE-004.

  - **1. WhatsApp Cloud API (Meta)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Es la API oficial; Meta provee un número de prueba gratuito y permite registrar un número limitado de destinatarios de prueba.
        - Los mensajes iniciados por la empresa requieren **plantillas aprobadas**; existe una plantilla de ejemplo lista para usar y se puede solicitar una plantilla propia de tipo *utilidad*.
        - API REST sencilla (HTTP + token), consumible con `RestClient` de Spring.
        - Para producción, la universidad debería verificar su empresa en Meta.

    - **Análisis de costos**:

      - **Resumen**: $0 en el entorno de prueba.

      - **Ejemplos**:

        - **Licencias**: ninguna en pruebas; en producción, cobro por conversación según tarifas de Meta.
        - **Capacitación**: plantillas y webhooks de estado.
        - **Operación**: gestión del token de acceso como secreto; registrar los números de prueba.
        - **Medición**: estados de entrega (enviado, entregado, leído) mediante webhook.

    - **Análisis FODA**:

      - **Fortalezas**: oficial; gratuito en pruebas; sin intermediarios.
      - **Debilidades**: plantillas sujetas a aprobación; destinatarios de prueba limitados; el token temporal expira y debe reemplazarse por uno permanente.
      - **Oportunidades**: producción con el número institucional de la UCO.
      - **Amenazas**: rechazo o demora en la aprobación de la plantilla propia.

    - **Opiniones y comentarios internos**:

      - Es la opción que mejor se puede sustentar: es el canal oficial y el mismo que se usaría en producción.

  - **2. Twilio (sandbox de WhatsApp)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple para la demostración, con limitaciones.

      - **Detalles**:

        - El destinatario debe enviar previamente un código de unión al número del sandbox.
        - Las cuentas de prueba consumen crédito promocional y marcan los mensajes como de prueba.
        - SDK de Java oficial y documentación sencilla.

    - **Análisis de costos**:

      - **Resumen**: $0 mientras dure el crédito de prueba.

      - **Ejemplos**:

        - **Licencias**: crédito de prueba; luego, pago por mensaje.
        - **Capacitación**: baja.
        - **Operación**: los participantes del sandbox deben volver a unirse periódicamente.
        - **Medición**: consola de Twilio.

    - **Análisis FODA**:

      - **Fortalezas**: configuración rápida.
      - **Debilidades**: intermediario con costo; uniones al sandbox que expiran.
      - **Oportunidades**: alternativa de respaldo si la plantilla de Meta no se aprueba a tiempo.
      - **Amenazas**: agotar el crédito; la unión expira antes de la sustentación.

    - **Opiniones y comentarios internos**:

      - Se mantiene como plan B gracias al adaptador intercambiable.

  - **3. Librerías no oficiales (automatización de WhatsApp Web)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Violan los términos de servicio de WhatsApp.
        - Riesgo de bloqueo del número.
        - Requieren mantener una sesión de navegador activa, lo que no encaja en un contenedor sin estado (Twelve-Factor).

    - **Análisis de costos**:

      - **Resumen**: «Gratis», con un costo legal y operativo inaceptable.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: media.
        - **Operación**: frágil.
        - **Medición**: inexistente.

    - **Análisis FODA**:

      - **Fortalezas**: sin aprobaciones.
      - **Debilidades**: ilegítimo; frágil.
      - **Oportunidades**: ninguna.
      - **Amenazas**: bloqueo; incumplimiento de políticas y de la Ley 1581.

    - **Opiniones y comentarios internos**:

      - Descartado: una institución no puede apoyarse en un canal que incumple los términos del proveedor.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-004 — Validará el envío de una plantilla desde Cloud Run al número de prueba registrado, el manejo de un token inválido (reintento y auditoría) y el tiempo de aprobación de una plantilla propia — Ejecutado: rechazado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Twilio (plan B) y librerías no oficiales (descartadas).

  - **¿Qué estás creando?**:

    - El canal de WhatsApp del flujo Publicar Kudo.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios de cumplimiento, costo y facilidad de demostración, y en la validación con el SPIKE-004.

  - **¿Por qué elegiste al ganador?**:

    Es el canal oficial, gratuito en pruebas y el mismo que se usaría en producción; el adaptador permite cambiar a Twilio sin tocar los casos de uso.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de los mensajes de WhatsApp.
    - **¿Qué tipos de integraciones están involucradas?**: Graph API de Meta vía HTTPS, procesador de outbox (ADR-002) y auditoría (ADR-013).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Solicitar la plantilla propia el primer día y registrar desde ya el número del docente como destinatario de prueba.

  - **Anécdotas**:

    - WhatsApp es el canal con más riesgo del proyecto, no por la programación sino por las condiciones del proveedor (aprobaciones, destinatarios de prueba). Por eso se valida primero.

- **Recomendación**:

  - **Resumen**:

    Usar **WhatsApp Cloud API** con un adaptador `WhatsAppCloudAdapter`, y **Twilio** como adaptador alternativo configurable.

  - **Detalles**:

    - Plantilla de utilidad con variables: nombre del autor y categoría.
    - Número de teléfono en formato E.164, tomado del celular verificado del destinatario.
    - Token, ID del número y versión de la API por variables de entorno.
    - Selección del adaptador por configuración (`notificaciones.whatsapp.proveedor=meta|twilio`).
    - Errores 429 y 5xx tratados como transitorios (reintento); errores 4xx de validación como definitivos.

- **Resultado de la validación**:

  - **Fecha**: 12 y 13 de noviembre de 2026.
  - **Estado final**: **Rechazado**. Reemplazado por el ADR-015 (canal móvil por SMS con Traccar SMS Gateway).
  - **Spikes**: SPIKE-004, ejecutado con resultado Rechazado.
  - **Qué se validó**:
    1. **WhatsApp Cloud API directa (Meta for Developers)**: Meta bloqueó la creación de la cuenta de desarrollador por seguridad ("dispositivo que no usas habitualmente"), desde computador y celular. No fue posible obtener token ni número de prueba.
    2. **Twilio (sandbox, plan B)**: cuenta y número verificados, pero el mensaje de activación `join` nunca llegó al sandbox; no se pudo activar.
    3. **API no oficial por QR (GREEN-API)**: descartada por la misma razón que el candidato 3: acceso de un tercero a la cuenta personal y riesgo de suspensión del número.
    4. **WATI (proveedor oficial de Meta)**: se creó la cuenta oficial de WhatsApp Business con el número +57 304 4326336 mediante el registro integrado de Meta. El número quedó conectado, pero el envío quedó retenido por la revisión de la cuenta y el 13-11-2026 Meta **desactivó de forma permanente** la cuenta de WhatsApp Business.
  - **Argumentos del rechazo**:
    - **Cumplimiento del criterio de demostración**: ninguna ruta oficial permitió entregar un mensaje al destinatario antes de la fecha de entrega.
    - **Dependencia de aprobaciones externas**: el canal depende de la verificación del negocio y de revisiones de Meta que un proyecto individual sin empresa registrada no controla; la cuenta puede suspenderse sin aviso, como ocurrió.
    - **Costo y tiempo**: las alternativas oficiales restantes (proveedores BSP de pago) exigen verificación empresarial y tiempos de aprobación incompatibles con la línea base.
    - **Riesgo de las vías no oficiales**: incumplen los términos de WhatsApp y exponen la cuenta personal; se mantienen descartadas.
  - **Lo que sí se validó de la decisión**: el adaptador `CanalWhatsappCloudApi` y sus pruebas automáticas funcionan contra la API simulada, y el diseño con el puerto `CanalNotificacion` permitió cambiar de proveedor sin tocar el caso de uso, el outbox ni el dominio. Mientras el proveedor falló, el Kudo se publicó, el correo y el Push se entregaron, y la notificación de WhatsApp quedó en el outbox reintentándose con *backoff* hasta `FALLIDO`, todo auditado (valida ADR-002 y ADR-013).
  - **Decisión que lo reemplaza**: [ADR-015]. Tras la validación, el adaptador de Meta se retiró del código para no mantener código sin uso; queda en el historial de Git y podría restaurarse como un adaptador más de `CanalNotificacion` si la universidad obtiene una cuenta de WhatsApp Business verificada.
