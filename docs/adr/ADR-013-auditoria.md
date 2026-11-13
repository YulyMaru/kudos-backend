# ADR-013: Registrar la auditoría en una tabla de solo inserción alimentada por eventos de dominio

- **Título**: Implementar la auditoría como un módulo que se suscribe a los eventos de dominio y registra cada acción en una tabla PostgreSQL de solo inserción (*append-only*), con identificador de correlación.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-001.

- **Criterios de evaluación**:

  - **Resumen**: Definir cómo se garantiza que todo el proceso de publicación de un Kudo (y en general las acciones críticas) quede auditado, de forma confiable, consultable e inalterable.

  - **Detalles**:

    - **Completitud**: registrar cada paso del flujo Publicar Kudo (almacenamiento, correo, WhatsApp, Push) con su resultado.
    - **Integridad**: los registros no pueden modificarse ni eliminarse desde la aplicación.
    - **Correlación**: reconstruir el proceso completo de un Kudo a partir de un identificador.
    - **Bajo acoplamiento**: la lógica de negocio no debe depender de la auditoría.
    - **Consultabilidad**: filtrar por fecha, actor, acción y recurso (HU-30).
    - **Privacidad**: sin secretos ni datos personales innecesarios.
    - **Escenarios de calidad relacionados**: ESC-CAL-ADM-069 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-22 y HU-30.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro enfoques.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Solo logs de aplicación** como auditoría.
    2. **Auditoría de entidades** con Hibernate Envers / Spring Data Auditing.
    3. **Tabla de auditoría de solo inserción alimentada por eventos de dominio**.
    4. **Servicio externo de auditoría** o almacenamiento inmutable dedicado.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron las recomendaciones de OWASP sobre registro de eventos de seguridad, los patrones *Audit Log* y *Domain Events*, y las capacidades de Envers.

  - **1. Solo logs de aplicación**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Los logs tienen retención limitada, no son consultables desde la aplicación y se mezclan con información técnica.

    - **Análisis de costos**:

      - **Resumen**: Bajo.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: ninguna.
        - **Operación**: retención limitada.
        - **Medición**: búsqueda manual.

    - **Análisis FODA**:

      - **Fortalezas**: ya existen.
      - **Debilidades**: no son una fuente de auditoría formal.
      - **Oportunidades**: complemento para el diagnóstico.
      - **Amenazas**: pérdida de evidencia al expirar.

    - **Opiniones y comentarios internos**:

      - Los logs sirven a la observabilidad (ADR-012), no a la auditoría.

  - **2. Hibernate Envers / Spring Data Auditing**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Registra cambios de entidades (qué columnas cambiaron), pero no acciones que no modifican entidades, como «correo enviado» o «push no aplicable».
        - Acopla la auditoría al modelo JPA.

    - **Análisis de costos**:

      - **Resumen**: Bajo para cambios de datos; insuficiente para el proceso.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: baja.
        - **Operación**: tablas `_AUD` por entidad.
        - **Medición**: consultas por revisión.

    - **Análisis FODA**:

      - **Fortalezas**: historial automático de cambios.
      - **Debilidades**: no cubre acciones del proceso; acoplado a JPA.
      - **Oportunidades**: complemento para el historial de entidades.
      - **Amenazas**: falsa sensación de auditoría completa.

    - **Opiniones y comentarios internos**:

      - El requisito es auditar un proceso, no solo cambios de filas.

  - **3. Tabla de solo inserción alimentada por eventos**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Cada caso de uso y el procesador de outbox emiten eventos (`KudoPublicado`, `NotificacionEnviada`, `NotificacionFallida`…); el módulo de auditoría los escucha y los registra.
        - El evento `KudoPublicado` se audita en la misma transacción del Kudo; los resultados de cada canal se auditan al terminar cada intento.
        - El usuario de base de datos de la aplicación solo tiene permiso `INSERT` y `SELECT` sobre la tabla (sin `UPDATE` ni `DELETE`).
        - Todos los registros de un Kudo comparten un `idCorrelacion`.

    - **Análisis de costos**:

      - **Resumen**: Bajo; usa la base de datos existente.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: eventos de dominio.
        - **Operación**: política de retención y archivado a futuro.
        - **Medición**: eventos auditados por tipo.

    - **Análisis FODA**:

      - **Fortalezas**: desacoplada; completa; consultable; inalterable desde la aplicación.
      - **Debilidades**: la tabla crece de forma continua.
      - **Oportunidades**: particionar por fecha; exportar a almacenamiento inmutable.
      - **Amenazas**: un administrador de la base de datos con privilegios podría alterarla (se mitiga con *hash* encadenado como evolución).

    - **Opiniones y comentarios internos**:

      - Reutiliza los eventos que ya existen para las notificaciones (ADR-002): la auditoría es otro suscriptor más.

  - **4. Servicio externo / almacenamiento inmutable**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple, pero excede el alcance y el presupuesto.

      - **Detalles**:

        - Ofrece inmutabilidad fuerte (WORM), pero agrega un proveedor y costos.

    - **Análisis de costos**:

      - **Resumen**: Alto.

      - **Ejemplos**:

        - **Licencias**: pago.
        - **Capacitación**: media.
        - **Operación**: integración adicional.
        - **Medición**: propia del proveedor.

    - **Análisis FODA**:

      - **Fortalezas**: inmutabilidad fuerte.
      - **Debilidades**: costo; dependencia.
      - **Oportunidades**: cumplimiento normativo estricto.
      - **Amenazas**: presupuesto.

    - **Opiniones y comentarios internos**:

      - Evolución, no necesidad de la línea base.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-001 — Validará que una publicación de Kudo genera los registros de auditoría de todos sus pasos con el mismo `idCorrelacion`, incluidos los fallos simulados de un canal, y que la aplicación no puede modificarlos — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Logs, Envers y servicios externos.

  - **¿Qué estás creando?**:

    - El módulo de auditoría de la plataforma.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores y, en la validación, revisando la auditoría generada por una publicación con fallas simuladas.

  - **¿Por qué elegiste al ganador?**:

    Audita el proceso completo, no solo cambios de datos; está desacoplado del negocio y no requiere infraestructura adicional.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de las acciones auditables.
    - **¿Qué tipos de integraciones están involucradas?**: Eventos de dominio, procesador de outbox (ADR-002), PostgreSQL y consulta para administradores (HU-30).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Definir el catálogo de acciones auditables desde el inicio.

  - **Anécdotas**:

    - La auditoría no debe contener información que no se querría ver filtrada: se guarda *quién reconoció a quién y cuándo*, no el contenido completo del Kudo ni los adjuntos.

- **Recomendación**:

  - **Resumen**:

    Tabla **`registro_auditoria` de solo inserción**, alimentada por **eventos de dominio**.

  - **Detalles**:

    - Campos: id, fecha_hora (UTC), id_correlacion, actor_id, rol_actor, accion, recurso_tipo, recurso_id, canal, resultado, detalle (JSON sin datos sensibles), ip_origen, trace_id.
    - Acciones mínimas: `KUDO_PUBLICADO`, `ADJUNTO_SUBIDO`, `NOTIFICACION_ENVIADA`, `NOTIFICACION_FALLIDA`, `NOTIFICACION_NO_APLICA`, `INICIO_SESION`, `INICIO_SESION_FALLIDO`, `CONTENIDO_VETADO`.
    - Permisos de base de datos restringidos a `INSERT` y `SELECT`.
    - Consulta paginada y con filtros, solo para administradores.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-001, aprobado.
  - **Qué se validó**: Cada publicación registra en `registro_auditoria` el guardado del Kudo y el resultado de cada canal con el mismo `id_correlacion`; la tabla es de solo inserción (un *trigger* impide actualizar o borrar). Verificado en producción desde la consulta de auditoría del administrador.
  - **Cambios respecto a lo propuesto**: Ninguno.
