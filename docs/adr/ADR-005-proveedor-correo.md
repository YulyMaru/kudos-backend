# ADR-005: Enviar correos mediante SMTP con Spring Mail usando Brevo como relay

- **Título**: Implementar el canal de correo electrónico con Spring Mail (`JavaMailSender`) sobre SMTP, usando Brevo como servicio de relay transaccional, detrás de un puerto `CanalNotificacion`.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-003.

- **Criterios de evaluación**:

  - **Resumen**: Elegir el proveedor y el mecanismo con los que el destinatario de un Kudo recibe un correo de forma confiable, con costo $0 y sin quedar atado a un proveedor.

  - **Detalles**:

    - **Entregabilidad**: los correos deben llegar a la bandeja de entrada, incluido el correo institucional.
    - **Costo**: plan gratuito suficiente para la línea base y la sustentación.
    - **Requisitos de configuración**: viable sin un dominio propio (se verifica un remitente individual).
    - **Bajo acoplamiento**: cambiar de proveedor sin modificar los casos de uso.
    - **Seguridad**: credenciales fuera del código.
    - **Escenarios de calidad relacionados**: ESC-CAL-CON-041, ESC-CAL-REN-014 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-19.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro opciones para el envío de correo transaccional.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **SMTP de Gmail** con contraseña de aplicación.
    2. **Brevo** (SMTP relay o API transaccional).
    3. **Resend** (API HTTP).
    4. **Amazon SES**.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron los requisitos de verificación de remitente o dominio, los límites de los planes gratuitos y el soporte de cada opción para Spring Boot. Los límites exactos se confirmarán en el SPIKE-003 porque cambian con frecuencia.

  - **1. SMTP de Gmail**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Fácil de configurar con Spring Mail.
        - Usa una cuenta personal como remitente; no está pensado para correo transaccional.
        - Riesgo de bloqueos o de clasificación como spam.

    - **Análisis de costos**:

      - **Resumen**: $0, con riesgo operativo.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: mínima.
        - **Operación**: depende de una cuenta personal con doble factor y contraseña de aplicación.
        - **Medición**: sin panel de entregas.

    - **Análisis FODA**:

      - **Fortalezas**: inmediato; gratuito.
      - **Debilidades**: no transaccional; sin métricas.
      - **Oportunidades**: respaldo de emergencia.
      - **Amenazas**: bloqueo de la cuenta.

    - **Opiniones y comentarios internos**:

      - Sirve como respaldo, no como opción principal.

  - **2. Brevo**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Ofrece SMTP relay y API transaccional, con un plan gratuito con cupo diario.
        - Permite enviar verificando un remitente individual, sin dominio propio.
        - Al usar SMTP estándar, el código (Spring Mail) es independiente del proveedor.
        - Ofrece un panel de entregas, rebotes y aperturas (evidencia para la entrega).

    - **Análisis de costos**:

      - **Resumen**: $0 en el plan gratuito.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: baja.
        - **Operación**: gestionar la clave SMTP como secreto.
        - **Medición**: panel del proveedor y métricas propias por canal.

    - **Análisis FODA**:

      - **Fortalezas**: transaccional; sin dominio; panel de entregas; SMTP estándar.
      - **Debilidades**: cupo diario limitado; marca del proveedor en los correos del plan gratuito.
      - **Oportunidades**: verificar el dominio institucional en producción para mejorar la entregabilidad.
      - **Amenazas**: los filtros del correo institucional podrían marcar como spam los correos de un remitente no verificado por dominio.

    - **Opiniones y comentarios internos**:

      - Es la opción que menos fricción agrega para cumplir el requisito en el plazo.

  - **3. Resend**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - API moderna y sencilla.
        - Sin un dominio verificado, solo permite enviar al propio correo de la cuenta, lo que impide notificar a destinatarios reales en la demostración.

    - **Análisis de costos**:

      - **Resumen**: Plan gratuito, pero requiere un dominio propio para el caso de uso real.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: baja.
        - **Operación**: gestión de DNS del dominio.
        - **Medición**: panel del proveedor.

    - **Análisis FODA**:

      - **Fortalezas**: excelente experiencia de desarrollo.
      - **Debilidades**: requiere dominio.
      - **Oportunidades**: opción de producción si se cuenta con dominio.
      - **Amenazas**: no poder demostrar el envío a terceros.

    - **Opiniones y comentarios internos**:

      - Se descarta para la línea base por el requisito de dominio.

  - **4. Amazon SES**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple técnicamente, pero no en costo ni en tiempo.

      - **Detalles**:

        - Inicia en modo *sandbox*: solo envía a direcciones verificadas hasta que AWS apruebe la salida.
        - Requiere una cuenta de AWS con medio de pago.

    - **Análisis de costos**:

      - **Resumen**: Muy económico a escala, pero con fricción inicial alta.

      - **Ejemplos**:

        - **Licencias**: pago por uso.
        - **Capacitación**: IAM y configuración de SES.
        - **Operación**: solicitud de salida del sandbox.
        - **Medición**: CloudWatch.

    - **Análisis FODA**:

      - **Fortalezas**: escalable; económico.
      - **Debilidades**: sandbox; complejidad.
      - **Oportunidades**: producción a gran escala.
      - **Amenazas**: aprobación fuera de plazo.

    - **Opiniones y comentarios internos**:

      - Excesivo para la línea base.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-003 — Validará el envío desde Cloud Run vía SMTP de Brevo a un correo institucional y a uno personal, la llegada a la bandeja de entrada y el comportamiento ante credenciales inválidas (reintento y auditoría) — Ejecutado: aprobado con cambios.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Gmail SMTP, Resend y Amazon SES.

  - **¿Qué estás creando?**:

    - El canal de correo del flujo Publicar Kudo.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios de costo, requisitos de configuración y acoplamiento, y en la validación con el SPIKE-003.

  - **¿Por qué elegiste al ganador?**:

    Brevo permite enviar a destinatarios reales sin dominio propio y con costo $0, y al usarlo vía SMTP el código no depende del proveedor.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de los correos.
    - **¿Qué tipos de integraciones están involucradas?**: Procesador de outbox (ADR-002), plantillas HTML (Thymeleaf) y auditoría (ADR-013).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Probar desde el primer día que el correo institucional no clasifica los mensajes como spam.

  - **Anécdotas**:

    - El requisito de dominio propio de algunos proveedores es fácil de pasar por alto y bloquea la demostración con el docente.

- **Recomendación**:

  - **Resumen**:

    Usar **Spring Mail sobre SMTP de Brevo**, detrás del puerto `CanalNotificacion`.

  - **Detalles**:

    - Adaptador `CorreoSmtpAdapter` que implementa `CanalNotificacion`.
    - Plantilla HTML con autor, categoría y enlace al Kudo (sin datos sensibles).
    - Host, puerto, usuario y clave por variables de entorno.
    - Reintentos y auditoría gestionados por el outbox (ADR-002).
    - Gmail SMTP como alternativa de respaldo configurable.

- **Resultado de la validación**:

  - **Fecha**: 12 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-003, ejecutado.
  - **Qué se validó**: Envío real del correo del Kudo desde la URL pública (Cloud Run) por SMTP de Brevo (`smtp-relay.brevo.com:587`, STARTTLS) con remitente verificado. El correo llega al destinatario y la notificación queda `ENVIADO` en el outbox y auditada.
  - **Cambios respecto a lo propuesto**: Ninguno en el código. Durante la activación de Brevo se comprobó que el proveedor se puede cambiar solo por configuración (`SMTP_HOST`, `SMTP_PUERTO` y secretos), por ejemplo a Gmail SMTP con contraseña de aplicación, sin recompilar.
  - **Pendiente**: Ninguno.
