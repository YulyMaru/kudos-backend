# ADR-012: Instrumentar con Spring Boot Actuator, Micrometer y OpenTelemetry, y operar con Google Cloud Observability

- **Título**: Instrumentar el back-end de forma neutral al proveedor (Actuator, Micrometer, Micrometer Tracing con OpenTelemetry y logs estructurados en JSON) y visualizar logs, métricas y trazas en Google Cloud Observability (Cloud Logging, Cloud Monitoring y Cloud Trace).

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-007.

- **Criterios de evaluación**:

  - **Resumen**: Elegir cómo se obtienen y consultan los logs, las métricas y las trazas (DRV-006), de modo que se pueda seguir el flujo Publicar Kudo de punta a punta y generar evidencias de observabilidad.

  - **Detalles**:

    - **Tres pilares**: logs, métricas y trazas correlacionados por `traceId`.
    - **Métricas de negocio**: Kudos publicados y notificaciones enviadas y fallidas por canal.
    - **Salud**: endpoints de *liveness* y *readiness* para Cloud Run.
    - **Neutralidad**: poder cambiar de plataforma de visualización sin reescribir la instrumentación.
    - **Seguridad**: sin datos personales ni secretos en los logs (Ley 1581).
    - **Costo**: $0 dentro de las capas gratuitas.
    - **Escenarios de calidad relacionados**: ESC-CAL-SOP-080, ESC-CAL-SOP-077 (ver `08-drivers-atributos-calidad.md`).

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron tres plataformas para el mismo esquema de instrumentación.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Google Cloud Observability** (nativo de Cloud Run).
    2. **Grafana Cloud** (Prometheus, Loki y Tempo gestionados).
    3. **Stack autoalojado** (ELK o Prometheus + Grafana + Jaeger).

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron el soporte de logs estructurados de Spring Boot, Micrometer y Micrometer Tracing, el protocolo OTLP de OpenTelemetry y las capacidades gratuitas de cada plataforma.

  - **1. Google Cloud Observability**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Cloud Run envía automáticamente la salida estándar a Cloud Logging; si es JSON, los campos se pueden consultar y filtrar.
        - Cloud Monitoring ofrece métricas de la plataforma (latencia, instancias, errores) sin configuración y acepta métricas personalizadas.
        - Cloud Trace recibe trazas OTLP.
        - No requiere cuentas ni servicios adicionales.

    - **Análisis de costos**:

      - **Resumen**: $0 dentro de la capa gratuita.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: consola de GCP y lenguaje de consulta de logs.
        - **Operación**: ninguna adicional.
        - **Medición**: tableros y alertas en Cloud Monitoring.

    - **Análisis FODA**:

      - **Fortalezas**: integrado; sin configuración de infraestructura; logs automáticos.
      - **Debilidades**: acoplado a GCP en la visualización (no en la instrumentación).
      - **Oportunidades**: alertas por correo ante tasas de error.
      - **Amenazas**: superar los volúmenes gratuitos si los logs son muy verbosos.

    - **Opiniones y comentarios internos**:

      - Aprovecha que el despliegue ya está en GCP (ADR-010).

  - **2. Grafana Cloud**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple, con un proveedor adicional.

      - **Detalles**:

        - Excelente visualización y estándares abiertos.
        - Requiere configurar el envío de los tres pilares hacia otro proveedor.

    - **Análisis de costos**:

      - **Resumen**: Plan gratuito con límites.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: PromQL y LogQL.
        - **Operación**: configurar exportadores y credenciales.
        - **Medición**: tableros de Grafana.

    - **Análisis FODA**:

      - **Fortalezas**: tableros potentes; estándares abiertos.
      - **Debilidades**: más configuración.
      - **Oportunidades**: alternativa si se cambia de nube.
      - **Amenazas**: límites del plan gratuito.

    - **Opiniones y comentarios internos**:

      - Alternativa válida; la instrumentación neutral permite cambiar después.

  - **3. Stack autoalojado**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple en costo ni en tiempo.

      - **Detalles**:

        - Exige desplegar y mantener varios servicios con almacenamiento persistente.

    - **Análisis de costos**:

      - **Resumen**: Alto.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: alta.
        - **Operación**: alta.
        - **Medición**: completa, pero costosa.

    - **Análisis FODA**:

      - **Fortalezas**: control total.
      - **Debilidades**: operación pesada.
      - **Oportunidades**: entornos *on-premise*.
      - **Amenazas**: plazo y presupuesto.

    - **Opiniones y comentarios internos**:

      - Desproporcionado para la línea base.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-007 — Validará en el entorno desplegado que una publicación de Kudo genera logs JSON con el mismo `traceId` en todos sus pasos, que las métricas personalizadas aparecen en Cloud Monitoring y que la traza se ve en Cloud Trace — Ejecutado: aprobado con cambios.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Grafana Cloud, ELK, Prometheus + Grafana + Jaeger y Datadog (descartado por costo).

  - **¿Qué estás creando?**:

    - La capacidad de monitoreo, métricas, trazas y diagnóstico de la línea base.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores; en la validación, siguiendo una publicación de Kudo real de punta a punta.

  - **¿Por qué elegiste al ganador?**:

    Ofrece los tres pilares sin infraestructura ni cuentas adicionales, y la instrumentación neutral evita quedar atado a la plataforma.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de la telemetría del back-end.
    - **¿Qué tipos de integraciones están involucradas?**: Spring Boot Actuator, Micrometer, OpenTelemetry (OTLP), Cloud Logging, Cloud Monitoring y Cloud Trace.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Definir desde el inicio qué campos no deben aparecer nunca en los logs (correo, celular, tokens) y enmascararlos.

  - **Anécdotas**:

    - La observabilidad y la auditoría se parecen pero responden preguntas distintas: la observabilidad dice *cómo se está comportando el sistema*; la auditoría dice *quién hizo qué y cuándo* (ADR-013). Por eso tienen almacenamientos y reglas de retención diferentes.

- **Recomendación**:

  - **Resumen**:

    **Actuator + Micrometer + Micrometer Tracing (OpenTelemetry) + logs JSON**, visualizados en **Google Cloud Observability**.

  - **Detalles**:

    - Logs estructurados JSON con `traceId`, `spanId`, módulo y evento; datos personales enmascarados.
    - Endpoints `/actuator/health/liveness` y `/actuator/health/readiness` para las sondas de Cloud Run.
    - Métricas personalizadas: `kudos.publicados`, `notificaciones.enviadas{canal}`, `notificaciones.fallidas{canal}`, `outbox.pendientes`.
    - Muestreo de trazas configurable por variable de entorno.
    - Tablero de Cloud Monitoring con las métricas del flujo Publicar Kudo (evidencia).

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-007, aprobado con cambios.
  - **Qué se validó**: Spring Boot Actuator expone `/actuator/health` (usado para verificar el despliegue), los logs del contenedor se consultan en Cloud Logging y el outbox publica métricas con Micrometer (`observabilidad`, `RegistroAuditoriaConMetricas`).
  - **Cambios respecto a lo propuesto**: Las trazas distribuidas en Cloud Trace quedan como evidencia complementaria.
