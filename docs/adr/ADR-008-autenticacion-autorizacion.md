# ADR-008: Autenticar con JWT emitidos por Spring Security y autorizar con RBAC

- **Título**: Implementar la autenticación con tokens JWT firmados emitidos por el propio back-end (Spring Security como *OAuth2 Resource Server*) y la autorización con control de acceso basado en roles (RBAC) y reglas de alcance organizacional.

- **Estado**: Aceptado (2026-11-13). Propuesto el 2026-11-09; validado con SPIKE-002.

- **Criterios de evaluación**:

  - **Resumen**: Elegir el mecanismo de identidad que proteja la API REST y el canal WebSocket, que soporte los siete roles institucionales con sus reglas y que pueda implementarse y sustentarse en el plazo.

  - **Detalles**:

    - **Seguridad**: contraseñas con hash robusto, tokens firmados y de vida corta, protección contra fuerza bruta.
    - **Autorización**: roles exclusivos y reglas de alcance (un coordinador solo gestiona su programa o facultad; solo el Rector y el Coordinador Académico vetan).
    - **Sin estado**: compatible con escalado horizontal y Twelve-Factor (factor VI).
    - **Integración**: el mismo mecanismo para REST y WebSocket.
    - **Costo y complejidad**: $0, sin servidores adicionales.
    - **Escenarios de calidad relacionados**: ESC-CAL-SEG-020, ESC-CAL-SEG-021, ESC-CAL-SEG-022 (ver `08-drivers-atributos-calidad.md`).
    - **Funcionalidades significativas relacionadas**: HU-06, HU-14, HU-15 y HU-29.

- **Candidatos a considerar**:

  - **Resumen**: Se evaluaron cuatro opciones.

  - **Enumerar todos los candidatos y opciones relacionadas**:

    1. **Sesiones de servidor con cookie** (Spring Session).
    2. **JWT emitidos por el back-end** con Spring Security.
    3. **Servidor de identidad propio** (Keycloak).
    4. **Identidad como servicio** (Firebase Authentication, Auth0).

- **Investigación y análisis de cada candidato**:

  - **Resumen**: Se revisaron OWASP ASVS (autenticación y sesiones), OWASP API Security Top 10 (especialmente la autorización a nivel de objeto, BOLA), la documentación de Spring Security 7 y la experiencia de AgroTrack.

  - **1. Sesiones de servidor con cookie**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Requiere almacenamiento compartido de sesiones (Redis) para escalar.
        - Front-end y back-end en dominios distintos complican las cookies (SameSite, CORS) y exigen protección CSRF.

    - **Análisis de costos**:

      - **Resumen**: Bajo en una sola instancia; requiere Redis para escalar.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: baja.
        - **Operación**: almacén de sesiones.
        - **Medición**: sesiones activas.

    - **Análisis FODA**:

      - **Fortalezas**: revocación inmediata.
      - **Debilidades**: con estado; problemas entre dominios.
      - **Oportunidades**: ninguna diferencial.
      - **Amenazas**: CSRF si se configura mal.

    - **Opiniones y comentarios internos**:

      - Va en contra de un back-end sin estado.

  - **2. JWT emitidos por el back-end (Spring Security)**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple los criterios.

      - **Detalles**:

        - Token de acceso firmado (HS256 con secreto en variable de entorno, o RS256), de vida corta, con `sub`, `rol` y alcance (`programaId`/`facultadId`).
        - Spring Security valida el token en cada petición sin consultar sesiones.
        - El mismo token autentica el `CONNECT` de WebSocket (ADR-004).
        - Autorización en dos niveles: por rol (`@PreAuthorize`) y por alcance de recurso en los casos de uso (evita BOLA).
        - Contraseñas con BCrypt o Argon2; bloqueo temporal tras 5 intentos fallidos.

    - **Análisis de costos**:

      - **Resumen**: Costo de implementación medio-bajo y sin infraestructura.

      - **Ejemplos**:

        - **Licencias**: ninguna.
        - **Capacitación**: Spring Security y buenas prácticas de JWT.
        - **Operación**: rotación del secreto de firma.
        - **Medición**: inicios de sesión exitosos y fallidos, respuestas 401 y 403.

    - **Análisis FODA**:

      - **Fortalezas**: sin estado; integrado con Spring; un mecanismo para REST y WebSocket.
      - **Debilidades**: la revocación no es inmediata (se mitiga con vida corta y *refresh token*).
      - **Oportunidades**: migrar a un proveedor de identidad (Keycloak o el directorio institucional) cambiando solo quién emite el token.
      - **Amenazas**: un secreto de firma filtrado; tokens guardados de forma insegura en el navegador.

    - **Opiniones y comentarios internos**:

      - Es la opción conocida, suficiente para la línea base y con una ruta de evolución clara.

  - **3. Keycloak**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple funcionalmente, pero agrega un servidor que desplegar y mantener.

      - **Detalles**:

        - Ofrece OAuth2/OIDC, MFA y federación con directorios.
        - Consume memoria y requiere su propia base de datos.

    - **Análisis de costos**:

      - **Resumen**: Código abierto, pero con un costo operativo alto.

      - **Ejemplos**:

        - **Licencias**: código abierto.
        - **Capacitación**: alta.
        - **Operación**: otro servicio desplegado.
        - **Medición**: propia de Keycloak.

    - **Análisis FODA**:

      - **Fortalezas**: estándar; completo.
      - **Debilidades**: pesado para el alcance.
      - **Oportunidades**: integración con el directorio institucional de la UCO.
      - **Amenazas**: plazo.

    - **Opiniones y comentarios internos**:

      - Evolución recomendada para producción, no para la línea base.

  - **4. Firebase Authentication / Auth0**:

    - **Cumple/no cumple con los criterios y por qué**:

      - **Resumen**: Cumple parcialmente.

      - **Detalles**:

        - Resuelve el inicio de sesión y la verificación de correo, pero los roles y las reglas institucionales siguen en el back-end.
        - Dependencia de un tercero para un dato crítico (la identidad).

    - **Análisis de costos**:

      - **Resumen**: Planes gratuitos con límites.

      - **Ejemplos**:

        - **Licencias**: plan gratuito.
        - **Capacitación**: SDK del proveedor.
        - **Operación**: sincronizar usuarios con la base de datos propia.
        - **Medición**: panel del proveedor.

    - **Análisis FODA**:

      - **Fortalezas**: verificación de correo y MFA incluidas.
      - **Debilidades**: dependencia; datos personales en un tercero.
      - **Oportunidades**: inicio de sesión social.
      - **Amenazas**: implicaciones de Habeas Data por la transferencia de datos.

    - **Opiniones y comentarios internos**:

      - No elimina el trabajo de autorización, que es lo complejo de este dominio.

- **Opiniones y comentarios externos**:

  - **Resumen**: Validado con los spikes ejecutados.

  - **¿Quién da la opinión?**:

    - SPIKE-002 — Validará que el mismo JWT autentica la API REST y el canal WebSocket, y que un usuario no puede suscribirse al canal de otro — Ejecutado: aprobado.

  - **¿Cuáles son otros candidatos que consideró?**:

    - Sesiones con cookie, Keycloak, Firebase Authentication y Auth0.

  - **¿Qué estás creando?**:

    - La capa de identidad y acceso de una plataforma institucional con siete roles.

  - **¿Cómo evaluó a los candidatos?**:

    Con los criterios de seguridad, sin estado, integración y costo; en la validación, con el SPIKE-002 y con pruebas de integración de autorización por rol.

  - **¿Por qué elegiste al ganador?**:

    JWT con Spring Security es seguro con una configuración adecuada, sin estado, integra REST y WebSocket y no requiere infraestructura adicional.

  - **¿Qué está pasando desde entonces?**:

    - **¿Cómo se desempeña el ganador?**: Por medir.
    - **¿Qué porcentaje del tráfico de usuarios de producción fluye a través del ganador?**: El 100 % de las peticiones autenticadas.
    - **¿Qué tipos de integraciones están involucradas?**: Spring Security, front-end Angular (interceptor HTTP), canal WebSocket y auditoría.
    - **Sabiendo lo que sabes ahora, ¿qué aconsejarías a las personas que hicieran de manera diferente?**: Probar la autorización por alcance con pruebas de integración desde el inicio; los errores de autorización a nivel de objeto son los más comunes en las API.

  - **Anécdotas**:

    - En este dominio, autenticar es la parte sencilla; lo difícil es autorizar por alcance («este coordinador solo gestiona su programa»). Esa regla vive en la capa de aplicación, no solo en una anotación del controlador.

- **Recomendación**:

  - **Resumen**:

    Usar **JWT firmados + Spring Security (Resource Server) + RBAC con reglas de alcance**.

  - **Detalles**:

    - Token de acceso de 15 minutos y *refresh token* rotativo.
    - Contraseñas con BCrypt (o Argon2).
    - Bloqueo temporal tras 5 intentos fallidos; intentos auditados.
    - CORS restringido al dominio del front-end; HTTPS obligatorio.
    - Matriz de permisos documentada (rol × acción) y probada con pruebas de integración.

- **Resultado de la validación**:

  - **Fecha**: 13 de noviembre de 2026.
  - **Estado final**: Aceptado.
  - **Spikes**: SPIKE-002, aprobado.
  - **Qué se validó**: Inicio de sesión con JWT HS256 y rotación de *refresh tokens*, contraseñas con BCrypt, roles con alcance aplicados en Spring Security y en el canal STOMP; validado en producción con los usuarios docente, estudiante y administrador.
  - **Cambios respecto a lo propuesto**: Ninguno.
