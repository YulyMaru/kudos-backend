# ADR-014: Construir el front-end como SPA Angular responsiva y Mobile First, instalable como PWA

- **Título**: Desarrollar el cliente como una aplicación de una sola página (SPA) en Angular, diseñada Mobile First y con capacidades de Progressive Web App (PWA), en lugar de aplicaciones nativas o híbridas.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-002.

- **Criterios de evaluación**:

  - **Resumen**: Elegir cómo atender al 90 % de usuarios que accederán desde dispositivos móviles (DRV-001) sin perder el acceso desde computador y dentro del plazo de una persona.

  - **Detalles**:

    - **Usabilidad móvil**: interfaz pensada primero para pantallas pequeñas y uso táctil.
    - **Alcance**: una sola base de código para móvil y escritorio.
    - **Distribución**: acceso por URL pública (requisito del enunciado), sin tiendas de aplicaciones.
    - **Rendimiento**: carga rápida en redes móviles.
    - **Conocimiento previo**: tecnología dominada para soportar cambios en vivo.
    - **Costo**: $0.
    - **Escenarios de calidad relacionados**: ESC-CAL-USA-001, ESC-CAL-ACC-063 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-17, HU-21 y HU-23.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro enfoques.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **SPA Angular responsiva Mobile First + PWA**.
    2. **Aplicación híbrida** (Ionic / Capacitor sobre Angular).
    3. **Aplicaciones nativas** (Android e iOS).
    4. **Multiplataforma compilada** (Flutter o React Native).

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron el enfoque Mobile First, las capacidades PWA de Angular (`@angular/pwa`), Angular Material/CDK para diseño responsivo, y los costos de publicar en tiendas.

  - **1. SPA Angular Mobile First + PWA**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Una base de código que funciona en cualquier navegador móvil y de escritorio, accesible por URL.
        - Estilos diseñados primero para pantallas pequeñas y ampliados con *media queries* (Mobile First).
        - Instalable en la pantalla de inicio como PWA; *service worker* para recursos estáticos.
        - Carga diferida de rutas (*lazy loading*) y compilación optimizada para redes móviles.
        - Tecnología elegida por la estudiante (restricción técnica propia).

    - **Análisis de costos**:

      - **Resumen**: $0 y una sola base de código.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: baja (Angular conocido).
        - **Operación**: hosting estático (ADR-010).
        - **Medición**: Lighthouse (rendimiento, accesibilidad, PWA).

    - **Análisis FODA**:

      - **Fortalezas**: una base de código; URL pública; sin tiendas.
      - **Debilidades**: acceso limitado a funciones nativas; Web Push en iOS restringido.
      - **Oportunidades**: empaquetar después con Capacitor si se requiere una app en tiendas.
      - **Amenazas**: diferencias entre navegadores móviles.

    - **Opiniones y comentarios internos**:

      - Cumple Mobile First y el requisito de URL pública con el menor esfuerzo.

  - **2. Ionic / Capacitor**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple, con trabajo adicional innecesario para la línea base.

      - **Detalles**:

        - Componentes con apariencia nativa y acceso a funciones del dispositivo.
        - La publicación en tiendas no es un requisito.

    - **Análisis de costos**:

      - **Resumen**: Bajo-medio.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: media.
        - **Operación**: compilaciones adicionales si se publica en tiendas.
        - **Medición**: similar a la opción 1.

    - **Análisis FODA**:

      - **Fortalezas**: apariencia nativa.
      - **Debilidades**: una capa más de dependencias.
      - **Oportunidades**: evolución natural de la opción 1.
      - **Amenazas**: plazo.

    - **Opiniones y comentarios internos**:

      - Queda como evolución, ya que reutiliza el código Angular.

  - **3. Aplicaciones nativas**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: No cumple.

      - **Detalles**:

        - Dos bases de código, cuentas de desarrollador y publicación en tiendas.
        - No ofrece una URL pública utilizable directamente por el docente.

    - **Análisis de costos**:

      - **Resumen**: Alto (incluye el costo de las cuentas de desarrollador).

      - **Ejemplos**:

        - **Licencias**: cuentas de tienda.
        - **Capacitación**: Kotlin y Swift.
        - **Operación**: revisiones de las tiendas.
        - **Medición**: herramientas de cada tienda.

    - **Análisis FODA**:

      - **Fortalezas**: máxima experiencia nativa.
      - **Debilidades**: costo y tiempo.
      - **Oportunidades**: ninguna para la línea base.
      - **Amenazas**: incumplir el plazo.

    - **Opiniones y comentarios internos**:

      - Inviable para el alcance.

  - **4. Flutter / React Native**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Una base de código, pero otra tecnología que aprender y cuyo soporte web es secundario.

    - **Análisis de costos**:

      - **Resumen**: Medio, por la curva de aprendizaje.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: Dart o React.
        - **Operación**: similar.
        - **Medición**: similar.

    - **Análisis FODA**:

      - **Fortalezas**: rendimiento cercano al nativo.
      - **Debilidades**: tecnología no dominada.
      - **Oportunidades**: ninguna diferencial.
      - **Amenazas**: no poder hacer cambios en vivo con soltura.

    - **Opiniones y comentarios internos**:

      - Va contra la restricción técnica de usar Angular.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-002 — Validará en un celular real la publicación de un Kudo, el feed y la recepción de la notificación en tiempo real — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Ionic/Capacitor, nativas, Flutter y React Native.

  - **¿Qué estás creando?**:

    - El cliente web Mobile First de UCO Kudos.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios anteriores y, en la validación, en un dispositivo móvil real y con Lighthouse.

  - **¿Por qué elegiste al ganador?**:

    Cumple Mobile First y el acceso por URL pública con una sola base de código y una tecnología dominada.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de los usuarios.
    - **¿Qué tipos de integraciones están involucradas?**: API REST del back-end, canal WebSocket/STOMP y Firebase Hosting.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Diseñar y probar cada pantalla primero en el ancho de un celular y después ampliarla.

  - **Anécdotas**:

    - Mobile First no es «que se vea bien en el celular» al final, sino empezar el diseño por la pantalla más pequeña y priorizar lo esencial (publicar y ver Kudos).

- **Recomendación**:

  - **Resumen**:

    **SPA Angular Mobile First + PWA**, desplegada en Firebase Hosting.

  - **Detalles**:

    - Componentes *standalone*, rutas con carga diferida y Angular Material/CDK (`BreakpointObserver`).
    - Navegación inferior en móvil y lateral en escritorio.
    - Interceptor HTTP para el JWT y servicio de notificaciones con RxStomp.
    - Configuración por entorno (URL de API y WebSocket).
    - Meta de Lighthouse móvil ≥ 90 en rendimiento y accesibilidad.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-002, aprobado.
  - **Qué se validó**: SPA Angular Mobile First publicada en Firebase Hosting, con rutas cargadas bajo demanda, notificaciones Push por STOMP con reconexión automática y pruebas unitarias y E2E (Playwright) en el pipeline.
  - **Cambios respecto a lo propuesto**: Ninguno.
