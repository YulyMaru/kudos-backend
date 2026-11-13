# ADR-007: Almacenar los adjuntos en almacenamiento de objetos privado (Supabase Storage)

- **Título**: Guardar los archivos adjuntos de los Kudos en un *bucket* privado de almacenamiento de objetos compatible con S3 (Supabase Storage), con metadatos en PostgreSQL y descarga mediante URL firmadas de corta duración.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-005.

- **Criterios de evaluación**:

  - **Resumen**: Definir dónde y cómo se guardan los adjuntos, cubriendo las cuatro estrategias que exige el enunciado: almacenamiento, seguridad, recuperación y trazabilidad.

  - **Detalles**:

    - **Almacenamiento**: durable, sin depender del sistema de archivos del contenedor (los contenedores de Cloud Run son efímeros).
    - **Seguridad**: archivos privados, validación de tipo y tamaño, acceso solo para usuarios autenticados.
    - **Recuperación**: posibilidad de restaurar archivos borrados o corruptos.
    - **Trazabilidad**: saber quién subió y quién descargó cada archivo y cuándo.
    - **Rendimiento**: no saturar la base de datos ni la API con binarios.
    - **Costo**: $0 en la línea base.
    - **Escenarios de calidad relacionados**: ESC-CAL-SEG-027, ESC-CAL-SEG-028 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-18 y HU-24.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro opciones.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **BLOB en PostgreSQL** (`bytea`).
    2. **Sistema de archivos del servidor**.
    3. **Almacenamiento de objetos compatible con S3** (Supabase Storage, Cloudflare R2, Google Cloud Storage).
    4. **Servicio de medios gestionado** (Cloudinary).

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron el factor VI de Twelve-Factor (procesos sin estado), el patrón *Valet Key* (URL firmadas), las recomendaciones de OWASP para la carga de archivos y los planes gratuitos de cada proveedor.

  - **1. BLOB en PostgreSQL**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Infla la base de datos y sus copias de seguridad, y consume rápidamente el plan gratuito.
        - Cada descarga pasa por la API y por el pool de conexiones.

    - **Análisis de costos**:

      - **Resumen**: Simple al inicio y costoso al crecer.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: mínima.
        - **Operación**: copias de seguridad pesadas.
        - **Medición**: tamaño de la base de datos.

    - **Análisis FODA**:

      - **Fortalezas**: transaccional junto al Kudo.
      - **Debilidades**: rendimiento; tamaño.
      - **Oportunidades**: ninguna relevante.
      - **Amenazas**: alcanzar el límite del plan gratuito.

    - **Opiniones y comentarios internos**:

      - Mezcla datos transaccionales con binarios; se descarta.

  - **2. Sistema de archivos del servidor**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Cloud Run tiene un sistema de archivos efímero: los archivos se pierden al reiniciar la instancia.
        - Impide escalar horizontalmente (cada instancia tendría archivos distintos).

    - **Análisis de costos**:

      - **Resumen**: Aparentemente gratuito, pero implica pérdida de datos.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: mínima.
        - **Operación**: inviable en contenedores.
        - **Medición**: no aplica.

    - **Análisis FODA**:

      - **Fortalezas**: simple en local.
      - **Debilidades**: no persiste; incumple Twelve-Factor.
      - **Oportunidades**: ninguna.
      - **Amenazas**: pérdida de evidencias.

    - **Opiniones y comentarios internos**:

      - Incompatible con el despliegue elegido.

  - **3. Almacenamiento de objetos compatible con S3 (Supabase Storage)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Bucket **privado**; los archivos se descargan solo mediante **URL firmadas** de pocos minutos, generadas tras verificar la autorización (patrón Valet Key).
        - Mismo proyecto de Supabase que la base de datos (ADR-003): un proveedor menos que gestionar.
        - API compatible con S3: se puede usar el SDK de AWS para Java y migrar a R2, GCS o S3 sin cambiar el puerto `AlmacenamientoArchivos`.
        - Los metadatos (nombre original, tipo MIME, tamaño, hash SHA-256, autor, fecha, clave del objeto) se guardan en PostgreSQL.

    - **Análisis de costos**:

      - **Resumen**: $0 en el plan gratuito.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: SDK S3 y URL firmadas.
        - **Operación**: gestión de claves como secretos.
        - **Medición**: espacio usado, número de subidas y descargas.

    - **Análisis FODA**:

      - **Fortalezas**: durable; privado; desacoplado; portable.
      - **Debilidades**: límites de espacio del plan gratuito.
      - **Oportunidades**: CDN y análisis antivirus en la evolución.
      - **Amenazas**: que se filtre una URL firmada (mitigado con vigencia corta).

    - **Opiniones y comentarios internos**:

      - Resuelve las cuatro estrategias exigidas con un solo componente.

  - **4. Cloudinary**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Excelente para imágenes y videos (transformaciones), menos adecuado para documentos.
        - Otro proveedor más y una API propietaria.

    - **Análisis de costos**:

      - **Resumen**: Plan gratuito con créditos.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: API propietaria.
        - **Operación**: otro proveedor.
        - **Medición**: panel propio.

    - **Análisis FODA**:

      - **Fortalezas**: optimización de imágenes.
      - **Debilidades**: dependencia de una API propietaria.
      - **Oportunidades**: miniaturas en el feed.
      - **Amenazas**: dependencia del proveedor.

    - **Opiniones y comentarios internos**:

      - No aporta lo suficiente frente a la opción S3.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-005 — Validará la subida al bucket privado, el rechazo de un archivo con extensión falsa (validación por contenido), la descarga mediante URL firmada y su expiración — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - BLOB en base de datos, sistema de archivos y Cloudinary. Cloudflare R2 y GCS como alternativas S3 equivalentes.

  - **¿Qué estás creando?**:

    - El soporte de evidencias adjuntas de los Kudos.

  - **¿Cómo evaluó a los candidatos?**:

    Frente a las cuatro estrategias exigidas y a las restricciones de despliegue en contenedores, y en la validación con el SPIKE-005.

  - **¿Por qué elegiste al ganador?**:

    Es durable, privado por defecto, portable (API S3) y comparte proveedor con la base de datos, sin costo.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de los adjuntos.
    - **¿Qué tipos de integraciones están involucradas?**: Supabase Storage (API S3), PostgreSQL (metadatos) y auditoría (ADR-013).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Validar el tipo de archivo por su contenido (*magic bytes*), no por la extensión que envía el cliente.

  - **Anécdotas**:

    - El sistema de archivos local «funciona» en desarrollo y falla en Cloud Run en cuanto la instancia se recicla: es un ejemplo típico de por qué existe el factor VI de Twelve-Factor.

- **Recomendación**:

  - **Resumen**:

    Usar **Supabase Storage (bucket privado, API S3)** detrás del puerto `AlmacenamientoArchivos`.

  - **Detalles**:

    - **Estrategia de almacenamiento**: binarios en el bucket con clave `kudos/{idKudo}/{uuid}`; metadatos en la tabla `adjunto`.
    - **Estrategia de seguridad**: lista blanca de tipos (PDF, JPG, PNG), tamaño máximo por archivo y por Kudo, validación por contenido (Apache Tika), nombre aleatorio, bucket privado, URL firmadas de vigencia corta y autorización previa.
    - **Estrategia de recuperación**: hash SHA-256 para verificar integridad; borrado lógico (el objeto se conserva un periodo antes de eliminarse); copia periódica del bucket como evolución.
    - **Estrategia de trazabilidad**: eventos de auditoría `ADJUNTO_SUBIDO` y `ADJUNTO_DESCARGADO` con usuario, fecha, Kudo y hash.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-005, aprobado.
  - **Qué se validó**: Los adjuntos se guardan en el bucket privado `kudos-adjuntos` de Supabase Storage mediante su API compatible con S3; el tipo se valida por la firma del archivo (`DetectorTipoArchivoPorFirma`) y la descarga usa URL firmadas con expiración.
  - **Cambios respecto a lo propuesto**: Se usa la API S3 de Supabase Storage con el SDK de AWS; existe un adaptador local para desarrollo.
