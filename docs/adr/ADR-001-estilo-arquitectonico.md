# ADR-001: Adoptar la arquitectura de Puertos y Adaptadores (Hexagonal) guiada por los principios SOLID

- **Título**: Organizar el back-end de UCO Kudos con la arquitectura de Puertos y Adaptadores (Arquitectura Hexagonal), apoyada en los principios SOLID, en especial la Inversión de Dependencias, como forma concreta de cumplir Clean Architecture.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-001 y SPIKE-007.

- **Criterios de evaluación**:

  - **Resumen**: Elegir el estilo arquitectónico interno del back-end que aísle las reglas de negocio de los frameworks, la base de datos y los proveedores externos (correo, WhatsApp, almacenamiento), que permita cambiar cualquiera de ellos sin tocar el negocio y que pueda construir y sustentar una sola persona en el plazo disponible.

  - **Detalles**:

    - **Mantenibilidad**: reglas de negocio aisladas de Spring, JPA y los SDK de los proveedores.
    - **Capacidad para ser probado**: el dominio y los casos de uso deben probarse sin levantar infraestructura, reemplazando las dependencias externas por dobles de prueba.
    - **Intercambiabilidad de integraciones**: el proyecto tiene muchas integraciones con alternativas reales (Brevo o Gmail, Meta o Twilio, Supabase u otro S3), así que cambiar de proveedor no debe afectar los casos de uso.
    - **Cumplimiento de Clean Architecture y SOLID**: exigidos por el enunciado y por las restricciones técnicas.
    - **Costo de implementación y operación**: viable para una sola persona, con presupuesto $0 y entrega el 13 de noviembre de 2026.
    - **Capacidad para ser desplegado**: un artefacto simple de construir, escanear (Trivy) y desplegar.
    - **Evolución futura**: facilidad para agregar canales, proveedores o nuevas entradas (por ejemplo, una app móvil o mensajería) sin reescribir el núcleo.
    - **Escenarios de calidad relacionados**: ESC-CAL-ESC-050, ESC-CAL-MAN-103 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-17 (publicar Kudo), HU-18 (adjuntos), HU-19 a HU-22 (notificaciones y auditoría).

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro estilos arquitectónicos habituales para aplicaciones empresariales.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Arquitectura en capas tradicional** (controlador → servicio → repositorio).
    2. **Arquitectura de Puertos y Adaptadores (Hexagonal)**: el núcleo (dominio y casos de uso) define **puertos** (interfaces) y el mundo exterior se conecta mediante **adaptadores** de entrada (REST, WebSocket, tareas programadas) y de salida (persistencia, correo, WhatsApp, almacenamiento, auditoría).
    3. **Arquitectura de Cebolla (Onion)**: anillos concéntricos (dominio, servicios de dominio, servicios de aplicación, infraestructura) con dependencias hacia el centro.
    4. **Microservicios**: un servicio desplegable por contexto (identidad, kudos, notificaciones, auditoría).

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron la Arquitectura Hexagonal (Alistair Cockburn), Clean Architecture (Robert C. Martin), Onion Architecture (Jeffrey Palermo), los principios SOLID y los criterios de Twelve-Factor para despliegue en la nube. Clean Architecture se presenta como una síntesis de la hexagonal y la de cebolla: las tres comparten la **regla de dependencia** (las dependencias apuntan hacia el negocio). La diferencia está en cómo se organiza y se explica el código.

  - **1. Arquitectura en capas tradicional**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple. Es rápida de construir, pero no demuestra Clean Architecture ni la inversión de dependencias.

      - **Detalles**:

        - La capa de negocio depende directamente de la de datos (JPA) y de los SDK de los proveedores.
        - Cambiar de proveedor de WhatsApp o de correo obliga a modificar los servicios de negocio, lo que viola el principio abierto/cerrado.
        - Las pruebas unitarias requieren simular la infraestructura en todas partes.

    - **Análisis de costos**:

      - **Resumen**: El costo inicial es el más bajo, pero el de mantenimiento crece rápido.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: mínima.
        - **Operación**: un solo desplegable.
        - **Medición**: sencilla, sin límites claros entre negocio e infraestructura.

    - **Análisis FODA**:

      - **Fortalezas**: rapidez; familiaridad.
      - **Debilidades**: alto acoplamiento con frameworks; reglas de negocio dispersas.
      - **Oportunidades**: refactorizar más adelante.
      - **Amenazas**: incumplir un requisito evaluado; deuda técnica temprana.

    - **Opiniones y comentarios internos**:

      - Se descarta porque el proyecto se evalúa precisamente por aplicar Clean Architecture y SOLID.

  - **2. Arquitectura de Puertos y Adaptadores (Hexagonal)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios principales y es la que mejor se ajusta a un sistema cuyo reto central son las integraciones externas.

      - **Detalles**:

        - **Inversión de Dependencias (D de SOLID)**: el núcleo define los puertos (`RepositorioKudos`, `CanalNotificacion`, `AlmacenamientoArchivos`, `RegistroAuditoria`) y la infraestructura los implementa. El negocio no conoce Spring, JPA, Brevo, Meta ni Supabase.
        - **Abierto/Cerrado (O)**: agregar un canal de notificación o cambiar de proveedor es crear un nuevo adaptador; el caso de uso `PublicarKudo` no se modifica.
        - **Sustitución de Liskov (L)**: cualquier implementación de un puerto (Meta o Twilio, Brevo o Gmail, el adaptador real o un doble de prueba) puede reemplazar a otra sin romper el comportamiento esperado.
        - **Segregación de Interfaces (I)**: puertos pequeños y específicos por necesidad, en lugar de una interfaz «servicio externo» genérica.
        - **Responsabilidad Única (S)**: cada adaptador encapsula una sola tecnología; cada caso de uso, una sola intención del usuario.
        - **Cumple Clean Architecture**: el dominio y los casos de uso equivalen a las capas de entidades y casos de uso; los adaptadores, a la capa de interfaces e infraestructura. La regla de dependencia se respeta y se verifica con pruebas de arquitectura (ArchUnit).
        - Se despliega como **una sola unidad** (imagen Docker), lo que mantiene simples CI/CD, Trivy y el despliegue. La hexagonal organiza el interior del sistema; no obliga a distribuirlo.

    - **Análisis de costos**:

      - **Resumen**: Costo de implementación medio (más interfaces y mapeos) y costo operativo bajo.

      - **Ejemplos**:

        - **Licencias**: ninguna (Spring Boot y ArchUnit son de código abierto).
        - **Capacitación**: puertos, adaptadores y SOLID, ya trabajados en el curso.
        - **Operación**: un solo servicio que monitorear.
        - **Medición**: métricas y logs por adaptador (por ejemplo, fallas por proveedor) mediante etiquetas en Micrometer.

    - **Análisis FODA**:

      - **Fortalezas**: bajo acoplamiento; alta capacidad de prueba; proveedores intercambiables; aplicación directa de SOLID; despliegue simple.
      - **Debilidades**: más clases e interfaces; código de mapeo entre el modelo de dominio y los modelos de persistencia o transporte.
      - **Oportunidades**: agregar adaptadores de entrada (app móvil, mensajería) o de salida (Web Push, SMS) sin tocar el núcleo; extraer el módulo de notificaciones como servicio independiente en el futuro.
      - **Amenazas**: si no se verifican las reglas de dependencia, el núcleo puede terminar importando clases de infraestructura.

    - **Opiniones y comentarios internos**:

      - El corazón de UCO Kudos es «publicar y avisar por tres canales externos». La hexagonal hace explícito ese límite: el negocio decide *qué* notificar y los adaptadores resuelven *cómo*.
      - Facilita los cambios en vivo durante la sustentación: cambiar de proveedor o agregar un canal queda localizado en un adaptador.

  - **3. Arquitectura de Cebolla (Onion)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple, pero es menos expresiva para el problema de este proyecto.

      - **Detalles**:

        - Respeta la regla de dependencia con anillos concéntricos.
        - Pone el énfasis en las capas internas del dominio (servicios de dominio y de aplicación), no en la frontera con el exterior.
        - En un sistema dominado por integraciones, el vocabulario de puertos de entrada y salida comunica mejor las decisiones.

    - **Análisis de costos**:

      - **Resumen**: Similar a la hexagonal.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: media.
        - **Operación**: un solo desplegable.
        - **Medición**: similar.

    - **Análisis FODA**:

      - **Fortalezas**: dominio protegido; capas bien definidas.
      - **Debilidades**: más anillos de los necesarios para el tamaño del dominio.
      - **Oportunidades**: dominios con reglas de negocio más complejas.
      - **Amenazas**: sobreingeniería en las capas internas.

    - **Opiniones y comentarios internos**:

      - Es válida, pero la hexagonal explica mejor por qué los proveedores son intercambiables.

  - **4. Microservicios**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente. Ofrece escalabilidad independiente, pero su complejidad operativa es inviable para una persona en cuatro días. Además, es una decisión de despliegue, no de organización interna: cada microservicio necesitaría igualmente su propia arquitectura interna.

      - **Detalles**:

        - Requiere varios pipelines, un *API Gateway*, comunicación por red y un broker de mensajes.
        - Multiplica los costos de hosting y los puntos de falla.
        - La consistencia entre servicios exige patrones adicionales (Saga, Outbox distribuido).

    - **Análisis de costos**:

      - **Resumen**: Es el costo más alto en implementación y operación.

      - **Ejemplos**:

        - **Licencias**: ninguna, pero mayor consumo de capa gratuita y probablemente un costo de hosting.
        - **Capacitación**: alta (orquestación, trazas distribuidas).
        - **Operación**: varios servicios que desplegar, escanear y monitorear.
        - **Medición**: trazas distribuidas obligatorias.

    - **Análisis FODA**:

      - **Fortalezas**: escalado y despliegue independientes.
      - **Debilidades**: complejidad; latencia de red entre servicios.
      - **Oportunidades**: equipos independientes en el futuro.
      - **Amenazas**: no alcanzar a desplegar a tiempo; costos.

    - **Opiniones y comentarios internos**:

      - Es prematuro para el alcance y el tamaño del equipo. Si se necesita en el futuro, la hexagonal facilita la extracción porque los límites ya están definidos por puertos.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-001 — Validará que el caso de uso Publicar Kudo funciona de punta a punta a través de sus puertos, y que un adaptador puede reemplazarse por un doble de prueba sin modificar el caso de uso — Ejecutado: aprobado.
    - SPIKE-007 — Validará que la unidad desplegable se construye, se escanea con Trivy y se despliega en Cloud Run — Ejecutado: aprobado con cambios.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Capas tradicional, Onion y microservicios. Descartados por incumplir Clean Architecture y SOLID, por ser menos expresiva para un sistema de integraciones y por complejidad, respectivamente.

  - **¿Qué estás creando?**:

    - Una plataforma web institucional de reconocimientos para la comunidad UCO.
    - **B2B o B2C**: uso interno institucional (B2E).
    - **Orientado al exterior o solo para empleados**: comunidad universitaria (estudiantes, docentes y directivos).
    - **Computadora de escritorio o móvil**: web Mobile First (≈90 % móvil).
    - **Piloto o producción**: línea base funcional, con criterios de calidad para evolucionar a producción.
    - **Monolito o microservicios**: una sola unidad desplegable organizada internamente con Puertos y Adaptadores.

  - **¿Cómo evaluó a los candidatos?**:

    Contrastando cada estilo con los criterios de mantenibilidad, prueba, intercambiabilidad de proveedores, cumplimiento de SOLID y Clean Architecture, costo y despliegue, y con las restricciones de negocio (plazo, una persona, $0).

  - **¿Por qué elegiste al ganador?**:

    La arquitectura de Puertos y Adaptadores aplica directamente los principios SOLID (sobre todo la Inversión de Dependencias), cumple la regla de dependencia de Clean Architecture y resuelve el reto principal del proyecto: integrarse con varios proveedores externos intercambiables sin contaminar el negocio.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir tras la implementación de la línea base.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % del tráfico del back-end.
    - **¿Qué tipos de integraciones están involucradas?**: Adaptadores de entrada (REST, WebSocket, procesador programado del outbox) y de salida (PostgreSQL, Supabase Storage, Brevo, WhatsApp Cloud API, canal WebSocket y auditoría).
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Escribir desde el inicio las pruebas de arquitectura (ArchUnit) que impiden que el núcleo dependa de los adaptadores, y definir los puertos a partir de lo que necesita el caso de uso, no de lo que ofrece el proveedor.

  - **Anécdotas**:

    - Un error común es diseñar el puerto a imagen del SDK del proveedor (por ejemplo, un `enviarPlantillaMeta(...)`). Eso filtra la tecnología al núcleo. El puerto correcto expresa la intención del negocio (`notificar(destinatario, mensaje)`) y el adaptador traduce a cada proveedor.

- **Recomendación**:

  - **Resumen**:

    Implementar el back-end con **Puertos y Adaptadores (Arquitectura Hexagonal)**, aplicando **SOLID** y verificando la regla de dependencia de **Clean Architecture**.

  - **Detalles**:

    - **Dominio** (`dominio`): entidades (`Kudo`, `Usuario`, `Categoria`, `Replica`), objetos de valor, eventos de dominio (`KudoPublicado`) y reglas de negocio. No depende de ningún framework.
    - **Aplicación** (`aplicacion`): casos de uso (`PublicarKudoCasoUso`, `ConsultarFeedCasoUso`), **puertos de entrada** (interfaces de los casos de uso) y **puertos de salida** (`RepositorioKudos`, `CanalNotificacion`, `AlmacenamientoArchivos`, `RegistroAuditoria`, `PublicadorEventos`).
    - **Adaptadores de entrada** (`infraestructura.entrada`): controladores REST, endpoint WebSocket y procesador programado del outbox.
    - **Adaptadores de salida** (`infraestructura.salida`): persistencia JPA, correo SMTP, WhatsApp (Meta y Twilio), Push por WebSocket, almacenamiento S3 y auditoría.
    - **Configuración** (`configuracion`): ensamblaje de puertos con adaptadores mediante la inyección de dependencias de Spring.
    - **Reglas verificadas con ArchUnit en CI**: el dominio no importa `org.springframework` ni `jakarta.persistence`; la aplicación solo depende del dominio; los adaptadores dependen de los puertos y nunca al revés.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-001 y SPIKE-007, aprobados.
  - **Qué se validó**: La línea base completa se construyó con Puertos y Adaptadores: el dominio no depende de frameworks y la regla se verifica con ArchUnit (`ReglasArquitecturaTest`) en cada ejecución del pipeline. El canal móvil cambió de proveedor cuatro veces (Meta, Twilio, WATI y SMS) agregando solo adaptadores del puerto `CanalNotificacion`, sin tocar el dominio ni los casos de uso.
  - **Cambios respecto a lo propuesto**: Se agregaron los paquetes `consulta` y `jdbc` en persistencia (CQRS ligero) y `observabilidad`; no cambia la regla de dependencias.
