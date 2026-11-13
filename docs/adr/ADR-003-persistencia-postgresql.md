# ADR-003: Usar PostgreSQL gestionado como base de datos principal

- **Título**: Usar PostgreSQL gestionado (Supabase) como base de datos relacional principal, con migraciones versionadas mediante Flyway.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-007 y SPIKE-008.

- **Criterios de evaluación**:

  - **Resumen**: Elegir el motor de persistencia que garantice las reglas de integridad de UCO Kudos (unicidad de roles, calificación única, réplicas anidadas, auditoría inmutable) y que soporte una carga predominantemente de lectura con costo $0.

  - **Detalles**:

    - **Integridad**: restricciones únicas, incluidas restricciones condicionales (un solo rector *activo*).
    - **Modelo de datos**: relaciones muchos a muchos (estudiante–programa, docente–programa) y árboles de profundidad arbitraria (réplicas).
    - **Rendimiento de lectura**: índices y paginación eficiente (DRV-002).
    - **Transacciones**: guardar el Kudo y su outbox de forma atómica (ADR-002).
    - **Costo**: plan gratuito gestionado, sin administrar servidores.
    - **Evolución futura**: réplicas de lectura y escalado vertical sin cambiar de motor.
    - **Escenarios de calidad relacionados**: ESC-CAL-CON-049, ESC-CAL-CON-045 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-14, HU-17, HU-23, HU-25 y HU-27.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron tres opciones de persistencia.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **PostgreSQL** gestionado (Supabase, Neon o Cloud SQL).
    2. **MySQL** gestionado.
    3. **MongoDB** (Atlas, documental).

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se compararon las capacidades de integridad, las consultas recursivas, los índices parciales, el soporte en Spring Data JPA y los planes gratuitos disponibles.

  - **1. PostgreSQL**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple todos los criterios.

      - **Detalles**:

        - Los **índices únicos parciales** (`UNIQUE … WHERE activo = true`) garantizan un único rector, coordinador académico, coordinador por facultad y coordinador por programa activos, incluso con peticiones concurrentes.
        - Las **CTE recursivas** (`WITH RECURSIVE`) resuelven las réplicas sin límite de profundidad.
        - `INSERT … ON CONFLICT` implementa la calificación única (*upsert*).
        - `FOR UPDATE SKIP LOCKED` permite procesar el outbox con varias instancias.
        - Las transacciones ACID cubren el Kudo y su outbox.
        - Supabase ofrece además almacenamiento de objetos compatible con S3 en el mismo proyecto (ADR-007).

    - **Análisis de costos**:

      - **Resumen**: Costo $0 en el plan gratuito y costo de aprendizaje bajo.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: SQL estándar; ya conocido por la estudiante.
        - **Operación**: gestionado (copias de seguridad incluidas en el plan).
        - **Medición**: consultas lentas, conexiones y tamaño de la base de datos.

    - **Análisis FODA**:

      - **Fortalezas**: integridad fuerte; características avanzadas; amplio soporte en Spring.
      - **Debilidades**: el plan gratuito tiene límites de almacenamiento y puede pausarse por inactividad.
      - **Oportunidades**: réplicas de lectura y búsqueda de texto completo para el feed.
      - **Amenazas**: alcanzar los límites del plan gratuito; pausa del proyecto si no se usa.

    - **Opiniones y comentarios internos**:

      - Ya se usó en AgroTrack, lo que reduce el riesgo en el plazo disponible.

  - **2. MySQL**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - No tiene índices únicos parciales; la unicidad de un cargo «activo» requiere columnas generadas u otros mecanismos.
        - Soporta CTE recursivas desde la versión 8.
        - Menos opciones gestionadas gratuitas.

    - **Análisis de costos**:

      - **Resumen**: Similar a PostgreSQL, con más esfuerzo de modelado para las reglas de unicidad.

      - **Ejemplos**:

        - **Licencias**: código abierto (GPL).
        - **Capacitación**: baja.
        - **Operación**: depende del proveedor.
        - **Medición**: similar.

    - **Análisis FODA**:

      - **Fortalezas**: popular; rendimiento de lectura sólido.
      - **Debilidades**: reglas de unicidad condicional más complejas.
      - **Oportunidades**: ninguna diferencial.
      - **Amenazas**: soluciones alternativas frágiles para las reglas de negocio.

    - **Opiniones y comentarios internos**:

      - Sin ventajas frente a PostgreSQL para este dominio.

  - **3. MongoDB**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple los criterios de integridad.

      - **Detalles**:

        - El dominio es altamente relacional: jerarquía organizacional, vínculos con programas, autor y destinatario.
        - Las reglas de unicidad entre documentos y las transacciones multidocumento son más complejas.
        - Ventaja en contenido de longitud libre, que PostgreSQL resuelve con `TEXT`.

    - **Análisis de costos**:

      - **Resumen**: Plan gratuito disponible, pero con un alto costo de modelado.

      - **Ejemplos**:

        - **Licencias**: SSPL.
        - **Capacitación**: modelado documental.
        - **Operación**: gestionado en Atlas.
        - **Medición**: propia del proveedor.

    - **Análisis FODA**:

      - **Fortalezas**: esquema flexible.
      - **Debilidades**: integridad referencial débil.
      - **Oportunidades**: almacenar la auditoría en el futuro.
      - **Amenazas**: inconsistencias en las reglas organizacionales.

    - **Opiniones y comentarios internos**:

      - Se descarta por no ajustarse a un dominio relacional con reglas de unicidad.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-007 — Validará la conexión desde Cloud Run, las migraciones Flyway y la gestión de credenciales mediante variables de entorno — Ejecutado: aprobado con cambios.
    - SPIKE-008 — Validará el rendimiento del feed paginado con datos de prueba — Ejecutado: aprobado con cambios.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Neon y Cloud SQL como proveedores alternativos de PostgreSQL. Cloud SQL se descarta por no tener plan gratuito permanente; Neon queda como alternativa equivalente.

  - **¿Qué estás creando?**:

    - Persistencia para una plataforma web institucional organizada con Puertos y Adaptadores (ADR-001).

  - **¿Cómo evaluó a los candidatos?**:

    Contrastando cada regla de negocio crítica con la capacidad nativa del motor para garantizarla.

  - **¿Por qué elegiste al ganador?**:

    PostgreSQL garantiza en la propia base de datos las reglas que más riesgo tienen bajo concurrencia, y Supabase ofrece en el mismo plan gratuito la base de datos y el almacenamiento de adjuntos.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de la persistencia transaccional.
    - **¿Qué tipos de integraciones están involucradas?**: Spring Data JPA/Hibernate, Flyway, HikariCP y Cloud Run.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Definir los índices del feed y las restricciones únicas desde la primera migración.

  - **Anécdotas**:

    - Las reglas de unicidad del enunciado parecen reglas de aplicación, pero solo una restricción en la base de datos las garantiza ante dos peticiones simultáneas. Por eso se validan en dos niveles: en el dominio (mensaje claro al usuario) y en la base de datos (garantía final).

- **Recomendación**:

  - **Resumen**:

    Usar **PostgreSQL gestionado en Supabase** con **Flyway** para las migraciones.

  - **Detalles**:

    - Contenido de Kudos y réplicas en columnas `TEXT` (sin límite de longitud).
    - Índices únicos parciales para la unicidad de roles activos.
    - Índice `(fecha_publicacion DESC, id)` para el feed paginado.
    - Restricción `UNIQUE (usuario_id, contenido_id, tipo_contenido)` para las calificaciones.
    - Credenciales solo mediante variables de entorno o un gestor de secretos (Twelve-Factor, factor III).
    - Las entidades JPA viven en la capa de infraestructura y se mapean a entidades de dominio.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-007 y SPIKE-008, aprobados (SPIKE-008 con cambios).
  - **Qué se validó**: Supabase PostgreSQL en producción con conexión por el *pooler* en modo sesión; Flyway aplicó las migraciones V1 a V5 en el primer arranque de Cloud Run; pruebas de integración con Testcontainers sobre PostgreSQL real.
  - **Cambios respecto a lo propuesto**: La conexión usa el *pooler* de sesión de Supabase (compatible con IPv4 desde Cloud Run).
