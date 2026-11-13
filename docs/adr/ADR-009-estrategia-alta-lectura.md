# ADR-009: Atender la alta demanda de lectura con paginación por cursor, proyecciones de lectura y caché en memoria

- **Título**: Responder al driver DRV-002 (carga predominantemente de lectura) con un modelo de lectura separado (CQRS ligero), paginación por cursor (*keyset*), índices dedicados y caché en memoria (Caffeine) para datos poco cambiantes.

- **Estado**: Aceptado con cambios (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-008.

- **Criterios de evaluación**:

  - **Resumen**: Evaluar cómo mantener tiempos de respuesta bajos en las consultas (feed, detalle, categorías) cuando la gran mayoría de las operaciones son lecturas y el 90 % de los usuarios usa redes móviles.

  - **Detalles**:

    - **Rendimiento**: el feed debe cargar rápido incluso con miles de Kudos.
    - **Eficiencia móvil**: respuestas pequeñas (solo los campos necesarios) y comprimidas.
    - **Consistencia**: un Kudo recién publicado debe aparecer en el feed sin demoras perceptibles.
    - **Escalabilidad**: camino para crecer (réplicas de lectura, caché distribuida).
    - **Costo y complejidad**: $0 y sin componentes adicionales en la línea base.
    - **Escenarios de calidad relacionados**: ESC-CAL-REN-011, ESC-CAL-REN-016 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-16, HU-23 y HU-24.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro estrategias.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Consultas directas con paginación por *offset*** sobre las entidades JPA.
    2. **CQRS ligero**: proyecciones de lectura (DTO), paginación por cursor, índices y caché en memoria (Caffeine).
    3. **Caché distribuida** (Redis) delante de la base de datos.
    4. **CQRS completo** con base de datos de lectura separada o réplica de lectura.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron el patrón CQRS, la paginación por cursor frente a *offset*, el patrón *Cache-Aside*, las proyecciones de Spring Data y la compresión HTTP.

  - **1. Consultas directas con *offset***:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple solo para volúmenes pequeños.

      - **Detalles**:

        - `OFFSET` obliga a la base de datos a recorrer y descartar filas: las páginas profundas son cada vez más lentas.
        - Cargar entidades completas con relaciones provoca consultas N+1 y respuestas pesadas.
        - Si se publican Kudos mientras el usuario navega, se repiten elementos entre páginas.

    - **Análisis de costos**:

      - **Resumen**: Bajo inicialmente; se degrada con el volumen.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: mínima.
        - **Operación**: consultas lentas al crecer.
        - **Medición**: latencia del feed.

    - **Análisis FODA**:

      - **Fortalezas**: simple.
      - **Debilidades**: no escala; N+1; duplicados al paginar.
      - **Oportunidades**: ninguna.
      - **Amenazas**: degradación de la experiencia móvil.

    - **Opiniones y comentarios internos**:

      - No justifica el driver de alta lectura.

  - **2. CQRS ligero**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios con la mínima complejidad.

      - **Detalles**:

        - Los casos de uso de **escritura** usan el modelo de dominio; los de **lectura** usan consultas específicas que devuelven DTO planos (`KudoResumenDto`) con solo los campos del feed.
        - Paginación por cursor (`WHERE (fecha, id) < (:fecha, :id) ORDER BY fecha DESC, id DESC LIMIT :n`): tiempo constante y sin duplicados.
        - Índice compuesto `(fecha_publicacion DESC, id DESC)` filtrado por Kudos no vetados.
        - Caché Caffeine para categorías y datos de referencia (invalidada al modificarlos).
        - Compresión gzip/brotli en las respuestas y transacciones de solo lectura.
        - El feed no se guarda en caché para evitar inconsistencias; su velocidad viene del índice y del cursor.

    - **Análisis de costos**:

      - **Resumen**: Costo de implementación bajo-medio y sin infraestructura.

      - **Ejemplos**:

        - **Licencias**: ninguna (Caffeine es código abierto).
        - **Capacitación**: paginación por cursor y proyecciones.
        - **Operación**: ninguna adicional.
        - **Medición**: latencia p95 del feed y tasa de aciertos de la caché.

    - **Análisis FODA**:

      - **Fortalezas**: rápido; liviano para el móvil; sin componentes extra.
      - **Debilidades**: la caché en memoria no se comparte entre instancias.
      - **Oportunidades**: cambiar a Redis sin modificar los casos de uso (abstracción de caché de Spring).
      - **Amenazas**: datos de referencia desactualizados si no se invalida la caché.

    - **Opiniones y comentarios internos**:

      - Responde al driver con decisiones concretas que se pueden medir y explicar.

  - **3. Caché distribuida (Redis)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple, pero es prematura.

      - **Detalles**:

        - Útil con varias instancias o con un volumen muy alto.
        - Agrega un componente, latencia de red y problemas de invalidación.

    - **Análisis de costos**:

      - **Resumen**: Planes gratuitos limitados.

      - **Ejemplos**:

        - **Licencias**: código abierto; servicios gestionados con límites.
        - **Capacitación**: invalidación y expiración.
        - **Operación**: otro servicio.
        - **Medición**: aciertos y memoria.

    - **Análisis FODA**:

      - **Fortalezas**: compartida entre instancias.
      - **Debilidades**: complejidad.
      - **Oportunidades**: también serviría como broker para WebSocket (ADR-004).
      - **Amenazas**: inconsistencias por invalidación.

    - **Opiniones y comentarios internos**:

      - Evolución documentada para cuando haya varias instancias.

  - **4. CQRS completo / réplica de lectura**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Excede el alcance.

      - **Detalles**:

        - Requiere sincronizar modelos (consistencia eventual) o pagar una réplica.

    - **Análisis de costos**:

      - **Resumen**: Alto.

      - **Ejemplos**:

        - **Licencias**: costo de la réplica.
        - **Capacitación**: alta.
        - **Operación**: dos almacenes.
        - **Medición**: retraso de replicación.

    - **Análisis FODA**:

      - **Fortalezas**: escala de lectura casi ilimitada.
      - **Debilidades**: complejidad; consistencia eventual.
      - **Oportunidades**: futuro a gran escala.
      - **Amenazas**: costos; plazo.

    - **Opiniones y comentarios internos**:

      - El CQRS ligero deja preparado el camino hacia esta opción.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-008 — Validará con 10 000 Kudos de prueba que la paginación por cursor mantiene un tiempo de respuesta estable en páginas profundas frente a *offset*, y que no se repiten elementos al publicar durante la navegación — Ejecutado: aprobado con cambios.

  - **¿Cuáles son otros candidatos que consideró?**:

    - *Offset*, Redis y CQRS completo.

  - **¿Qué estás creando?**:

    - La estrategia de consulta de una plataforma web Mobile First con alta demanda de lectura.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores y, en la validación, midiendo latencias con datos de prueba (SPIKE-008).

  - **¿Por qué elegiste al ganador?**:

    Atiende el driver de alta lectura con decisiones de bajo costo (índices, cursor, DTO, caché local) y deja abiertas las evoluciones de escala.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de las consultas del feed y de las categorías.
    - **¿Qué tipos de integraciones están involucradas?**: PostgreSQL, Spring Data (proyecciones), Spring Cache con Caffeine y front-end Angular (desplazamiento infinito).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Diseñar el feed con cursor desde el inicio; migrar de *offset* a cursor más tarde rompe el contrato de la API.

  - **Anécdotas**:

    - En una carga de lectura, la decisión más rentable no suele ser agregar una caché, sino consultar menos datos y con el índice correcto.

- **Recomendación**:

  - **Resumen**:

    Aplicar **CQRS ligero + paginación por cursor + índices + caché Caffeine para datos de referencia**.

  - **Detalles**:

    - Endpoint `GET /api/v1/kudos?cursor=…&tamano=20`.
    - DTO de lectura con solo los campos del feed; el detalle se consulta aparte.
    - Caché de categorías con invalidación al crear, editar o desactivar.
    - Compresión HTTP habilitada.
    - Métrica de latencia p95 del feed en Micrometer.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado con cambios.
  - **Spikes**: SPIKE-008, aprobado con cambios.
  - **Qué se validó**: El feed usa CQRS ligero (consultas de lectura separadas en `persistencia/consulta`) y paginación por cursor, validados con pruebas automatizadas sin duplicados entre páginas.
  - **Cambios respecto a lo propuesto**: La prueba de carga con 10 000 Kudos y 200 usuarios concurrentes no se ejecutó dentro del plazo; queda como trabajo futuro. El volumen de la demostración no la requiere.
