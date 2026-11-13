# ADR-010: Desplegar el back-end en Google Cloud Run y el front-end en Firebase Hosting

- **Título**: Publicar el back-end como contenedor Docker en Google Cloud Run y el front-end Angular como sitio estático en Firebase Hosting, ambos con URL pública HTTPS.

- **Estado**: Aceptado con cambios (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-007.

- **Criterios de evaluación**:

  - **Resumen**: Elegir dónde publicar la solución para que el docente acceda mediante una URL pública, con despliegue automatizado, costo $0 y sin administrar servidores.

  - **Detalles**:

    - **URL pública con HTTPS**: obligatoria según el enunciado.
    - **Capacidad para ser desplegado**: despliegue automatizable desde GitHub Actions (CD).
    - **Compatibilidad**: contenedores Docker y conexiones WebSocket (ADR-004).
    - **Disponibilidad**: el sistema debe responder durante la evaluación y la sustentación.
    - **Elasticidad**: escalado automático (Manifiesto Reactivo).
    - **Costo**: dentro de capas gratuitas.
    - **Conocimiento previo**: reducir el riesgo en el plazo disponible.
    - **Escenarios de calidad relacionados**: ESC-CAL-DIS-032, ESC-CAL-DIS-036 (ver `08-drivers-atributos-calidad.md`).

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro opciones.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Google Cloud Run** (back-end) + **Firebase Hosting** (front-end).
    2. **Render** (back-end y front-end).
    3. **Railway**.
    4. **Máquina virtual** (Compute Engine, EC2 u Oracle Cloud) administrada manualmente.

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron la documentación de cada plataforma, sus capas gratuitas, el soporte de WebSocket y la experiencia previa con Cloud Run en AgroTrack, donde se observó un arranque en frío de unos 27 s.

  - **1. Cloud Run + Firebase Hosting**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Ejecuta cualquier imagen Docker, escala automáticamente (incluso a cero) y entrega HTTPS por defecto.
        - Soporta WebSocket y permite la afinidad de sesión.
        - Despliegue desde GitHub Actions con la acción oficial de Google y autenticación sin llaves (*Workload Identity Federation*).
        - Secret Manager para las credenciales; Cloud Logging, Monitoring y Trace integrados (ADR-012).
        - Firebase Hosting sirve el Angular compilado por CDN con HTTPS.
        - Ya se usó en AgroTrack.

    - **Análisis de costos**:

      - **Resumen**: $0 dentro de la capa gratuita; requiere una cuenta de facturación habilitada.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: baja (experiencia previa).
        - **Operación**: gestionada; configurar instancias mínimas y máximas.
        - **Medición**: métricas nativas de Cloud Run (latencia, instancias, errores).

    - **Análisis FODA**:

      - **Fortalezas**: elástico; HTTPS; WebSocket; ecosistema integrado; conocido.
      - **Debilidades**: arranque en frío de Spring Boot cuando escala a cero; requiere medio de pago registrado.
      - **Oportunidades**: Artifact Registry, Secret Manager y Cloud Trace en el mismo proyecto.
      - **Amenazas**: un arranque en frío durante la demostración; costos si se dejan instancias mínimas encendidas.

    - **Opiniones y comentarios internos**:

      - Es la opción con menos incertidumbre gracias a AgroTrack, y el arranque en frío ya conocido se puede mitigar.

  - **2. Render**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Despliegue sencillo desde el repositorio.
        - El plan gratuito suspende el servicio tras un periodo de inactividad y tarda en reactivarse.

    - **Análisis de costos**:

      - **Resumen**: $0 con suspensión por inactividad.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: mínima.
        - **Operación**: mínima.
        - **Medición**: limitada en el plan gratuito.

    - **Análisis FODA**:

      - **Fortalezas**: simplicidad.
      - **Debilidades**: suspensión; observabilidad limitada.
      - **Oportunidades**: alternativa de respaldo.
      - **Amenazas**: servicio suspendido cuando el docente lo pruebe.

    - **Opiniones y comentarios internos**:

      - Buena alternativa si fallara Cloud Run.

  - **3. Railway**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple técnicamente, con costo incierto.

      - **Detalles**:

        - Muy buena experiencia de desarrollo.
        - El uso gratuito está limitado a créditos de prueba.

    - **Análisis de costos**:

      - **Resumen**: Créditos de prueba; luego, pago.

      - **Ejemplos**:

        - **Licencias**: créditos limitados.
        - **Capacitación**: mínima.
        - **Operación**: mínima.
        - **Medición**: panel propio.

    - **Análisis FODA**:

      - **Fortalezas**: rapidez.
      - **Debilidades**: costo tras los créditos.
      - **Oportunidades**: prototipos.
      - **Amenazas**: agotar los créditos.

    - **Opiniones y comentarios internos**:

      - Se descarta por la restricción de presupuesto.

  - **4. Máquina virtual administrada**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Requiere administrar el sistema operativo, TLS, el proxy inverso y los parches.
        - No escala automáticamente.

    - **Análisis de costos**:

      - **Resumen**: Algunas capas gratuitas, con un costo operativo alto.

      - **Ejemplos**:

        - **Licencias**: según el proveedor.
        - **Capacitación**: administración de Linux.
        - **Operación**: alta.
        - **Medición**: manual.

    - **Análisis FODA**:

      - **Fortalezas**: control total.
      - **Debilidades**: operación manual; sin elasticidad.
      - **Oportunidades**: ninguna relevante.
      - **Amenazas**: vulnerabilidades sin parchear.

    - **Opiniones y comentarios internos**:

      - Contradice el uso de servicios gestionados que impone el plazo.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-007 — Validará el despliegue automatizado desde GitHub Actions, la conexión con Supabase, la carga de secretos, el tiempo de arranque en frío y la conexión WebSocket en el entorno público — Ejecutado: aprobado con cambios.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Render, Railway, máquinas virtuales y Vercel para el front-end (equivalente a Firebase Hosting).

  - **¿Qué estás creando?**:

    - El entorno público de la línea base de UCO Kudos.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores y la experiencia de AgroTrack; en la validación, con el SPIKE-007.

  - **¿Por qué elegiste al ganador?**:

    Cumple todos los criterios, es elástico, soporta WebSocket, se integra con secretos y observabilidad, y ya es conocido, lo que reduce el riesgo del plazo.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 %.
    - **¿Qué tipos de integraciones están involucradas?**: GitHub Actions, Artifact Registry, Secret Manager, Supabase, proveedores de notificación y Firebase Hosting.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Aprovechar lo aprendido en AgroTrack: aumentar el tiempo de espera del chequeo de salud o dejar una instancia mínima durante la evaluación.

  - **Anécdotas**:

    - En AgroTrack, el arranque en frío de Cloud Run (27 s) superó el tiempo de espera del cliente. Aquí se mitiga desde el diseño: instancia mínima en 1 durante la ventana de evaluación, *startup probe* con margen, y un arranque optimizado con CDS (*Class Data Sharing*) y carga diferida.

- **Recomendación**:

  - **Resumen**:

    Desplegar el back-end en **Cloud Run** y el front-end en **Firebase Hosting**.

  - **Detalles**:

    - Imagen Docker multietapa (compilación con Maven, ejecución con JRE 25 mínimo, usuario no root).
    - Imágenes en Artifact Registry; despliegue con `google-github-actions/deploy-cloudrun`.
    - `min-instances=1` durante la evaluación; `max-instances=1` en la línea base por el broker WebSocket en memoria (ADR-004).
    - Secretos en Secret Manager, inyectados como variables de entorno.
    - Front-end Angular en Firebase Hosting, con la URL del back-end configurada por entorno.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado con cambios.
  - **Spikes**: SPIKE-007, aprobado con cambios.
  - **Qué se validó**: Back-end publicado en Cloud Run (`us-west1`) con imágenes en Artifact Registry y secretos en Secret Manager; front-end publicado en Firebase Hosting; despliegue automático desde GitHub Actions con Workload Identity Federation, sin llaves.
  - **Cambios respecto a lo propuesto**: Se fija `min-instances=1` porque con 0 instancias el arranque en frío hacía que el front mostrara "No hay conexión". El despliegue a Firebase también usa Workload Identity Federation, porque la política de la organización de Google Cloud prohíbe crear llaves de cuentas de servicio.
