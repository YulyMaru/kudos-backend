# ADR-011: Automatizar CI/CD con GitHub Actions, SonarQube Cloud y Trivy

- **Título**: Construir los pipelines de integración y despliegue continuos con GitHub Actions, con análisis estático en SonarQube Cloud (antes SonarCloud) y escaneo de vulnerabilidades con Trivy como compuertas de calidad obligatorias.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-006.

- **Criterios de evaluación**:

  - **Resumen**: Elegir la plataforma y la configuración del pipeline que automaticen calidad, seguridad y despliegue desde la línea base (DRV-007), cumpliendo el requisito obligatorio de Sonar y Trivy.

  - **Detalles**:

    - **Cumplimiento**: CI, CD, Sonar y Trivy son obligatorios y deben dejar evidencias.
    - **Integración**: con los repositorios de GitHub, Cloud Run y Firebase Hosting.
    - **Seguridad**: secretos fuera del código; autenticación hacia GCP sin llaves de larga duración.
    - **Costo**: $0 para repositorios públicos o dentro de los minutos gratuitos.
    - **Mantenibilidad**: pipelines como código, versionados junto a la aplicación.
    - **Compuertas**: el despliegue se bloquea si fallan las pruebas, la compuerta de calidad de Sonar o el umbral de Trivy.
    - **Escenarios de calidad relacionados**: ESC-CAL-DES-097, ESC-CAL-DES-098, ESC-CAL-DES-102 (ver `08-drivers-atributos-calidad.md`).

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron tres plataformas de CI/CD y dos formas de usar Sonar.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **GitHub Actions** + **SonarQube Cloud** + **Trivy**.
    2. **GitLab CI/CD** + SonarQube + Trivy.
    3. **Jenkins** autoalojado + **SonarQube Server** autoalojado + Trivy.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron la integración de cada plataforma con GitHub, las acciones oficiales de Sonar y Trivy (`aquasecurity/trivy-action`), las opciones gratuitas y las prácticas de AgroTrack.

  - **1. GitHub Actions + SonarQube Cloud + Trivy**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Nativo en GitHub, donde viven ambos repositorios; flujos YAML versionados en `.github/workflows`.
        - SonarQube Cloud es gratuito para proyectos públicos y publica la compuerta de calidad en cada *pull request*.
        - Trivy escanea dependencias (`fs`), la imagen Docker (`image`) y la configuración (`config`, Dockerfile), y genera reportes SARIF visibles en la pestaña de seguridad de GitHub.
        - Workload Identity Federation para desplegar en GCP sin llaves JSON.
        - El historial de ejecuciones sirve directamente como evidencia.

    - **Análisis de costos**:

      - **Resumen**: $0 con repositorios públicos.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: YAML de Actions; ya conocido.
        - **Operación**: ninguna infraestructura propia.
        - **Medición**: duración del pipeline, tasa de éxito, cobertura, deuda técnica y vulnerabilidades.

    - **Análisis FODA**:

      - **Fortalezas**: integrado; gratuito; evidencias automáticas.
      - **Debilidades**: dependencia de GitHub; minutos limitados en repositorios privados.
      - **Oportunidades**: Dependabot y escaneo de secretos de GitHub.
      - **Amenazas**: acciones de terceros comprometidas (se mitiga fijando versiones).

    - **Opiniones y comentarios internos**:

      - Es el camino más corto hacia evidencias verificables.

  - **2. GitLab CI/CD**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple técnicamente.

      - **Detalles**:

        - Requeriría mover o replicar los repositorios en GitLab.
        - Muy completo en funciones DevSecOps.

    - **Análisis de costos**:

      - **Resumen**: Plan gratuito con minutos limitados.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: media.
        - **Operación**: migración de repositorios.
        - **Medición**: propia de GitLab.

    - **Análisis FODA**:

      - **Fortalezas**: plataforma DevSecOps integrada.
      - **Debilidades**: cambio de plataforma.
      - **Oportunidades**: ninguna diferencial aquí.
      - **Amenazas**: tiempo de migración.

    - **Opiniones y comentarios internos**:

      - No compensa el cambio de plataforma.

  - **3. Jenkins + SonarQube Server autoalojados**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple en costo ni en tiempo.

      - **Detalles**:

        - Requiere servidores propios, plugins y mantenimiento.
        - El servidor debería estar público para recibir *webhooks*.

    - **Análisis de costos**:

      - **Resumen**: Software libre, con infraestructura y operación costosas.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: alta.
        - **Operación**: alta.
        - **Medición**: manual.

    - **Análisis FODA**:

      - **Fortalezas**: control total.
      - **Debilidades**: mantenimiento; seguridad del propio servidor.
      - **Oportunidades**: entornos *on-premise* de la universidad.
      - **Amenazas**: plazo y presupuesto.

    - **Opiniones y comentarios internos**:

      - Desproporcionado para una línea base individual.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-006 — Validará que el pipeline falla si una prueba falla, si la compuerta de Sonar no se cumple o si Trivy detecta una vulnerabilidad CRÍTICA o ALTA con corrección disponible, y que despliega automáticamente cuando todo pasa — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - GitLab CI, Jenkins, SonarQube Server, y OWASP Dependency-Check como complemento a Trivy.

  - **¿Qué estás creando?**:

    - Pipelines DevSecOps para dos repositorios (back-end Spring Boot y front-end Angular).

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores y, en la validación, provocando fallas a propósito en cada compuerta (SPIKE-006).

  - **¿Por qué elegiste al ganador?**:

    Cumple los requisitos obligatorios sin infraestructura propia, con costo $0 y evidencias automáticas.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de los cambios que llegan a producción.
    - **¿Qué tipos de integraciones están involucradas?**: GitHub, SonarQube Cloud, Trivy, Artifact Registry, Cloud Run y Firebase Hosting.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Montar el pipeline completo con un «hola mundo» el primer día, antes de escribir la lógica de negocio.

  - **Anécdotas**:

    - Una compuerta de seguridad que nunca falla no demuestra nada. Para las evidencias conviene mostrar también una ejecución bloqueada a propósito (por ejemplo, una dependencia vulnerable) y su corrección.

- **Recomendación**:

  - **Resumen**:

    Usar **GitHub Actions + SonarQube Cloud + Trivy** en ambos repositorios.

  - **Detalles**:

    - **CI (en cada push y pull request)**: compilación → pruebas unitarias y de integración (Testcontainers) → cobertura (JaCoCo / Karma-Jest) → SonarQube Cloud con compuerta de calidad → Trivy `fs` sobre dependencias.
    - **CD (al fusionar en `main`)**: construcción de la imagen → Trivy `image` (falla con CRITICAL/HIGH corregibles) → push a Artifact Registry → despliegue en Cloud Run → prueba de humo y E2E (Playwright) contra la URL pública.
    - **Front-end**: compilación → pruebas → SonarQube Cloud → Trivy `fs` → despliegue en Firebase Hosting.
    - Versiones de las acciones fijadas; secretos en GitHub Secrets; Workload Identity Federation hacia GCP.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-006, aprobado.
  - **Qué se validó**: Los dos repositorios ejecutan en cada *push* a `main`: pruebas y cobertura, SonarQube Cloud con compuerta de calidad, Trivy y despliegue. La compuerta bloqueó un despliegue real cuando la cobertura del código nuevo fue 63,6 %; tras agregar pruebas subió a 98 % y el pipeline desplegó. Calificación A en seguridad, fiabilidad y mantenibilidad en ambos proyectos.
  - **Cambios respecto a lo propuesto**: Ninguno.
