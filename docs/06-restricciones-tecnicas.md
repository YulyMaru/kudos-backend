# 06 · Restricciones Técnicas — UCO Kudos

**Tipo**:
- *Impuesta por el cliente*: exigida en el enunciado del proyecto.
- *Propia del proyecto*: decisión tomada por el equipo (en este caso, la estudiante) y justificada.

| Tipo | Categoría | Restricción técnica | Justificación |
|------|-----------|---------------------|---------------|
| Impuesta por el cliente | Arquitectura | La solución debe aplicar **Clean Architecture** (capas, dependencias hacia el dominio, responsabilidades y reglas arquitectónicas explícitas). | Aísla las reglas de negocio de frameworks, base de datos y proveedores externos, lo que facilita las pruebas, el mantenimiento y la evolución. |
| Impuesta por el cliente | Prácticas de diseño | La solución debe analizar y aplicar el **Manifiesto Reactivo** (responsividad, resiliencia, elasticidad, orientación a mensajes). | Las notificaciones multicanal dependen de terceros con fallas y latencias propias; un diseño orientado a mensajes mantiene al sistema responsivo y resiliente. |
| Impuesta por el cliente | Patrones de diseño | La solución debe evaluarse frente a la metodología **Twelve-Factor App**. | Garantiza portabilidad, configuración por entorno, despliegue continuo y escalabilidad en la nube. |
| Impuesta por el cliente | Prácticas de código limpio | Aplicar **Clean Code**: nombres expresivos, métodos cohesivos, bajo acoplamiento y alta cohesión. | Facilita la comprensión, la sustentación y los cambios en vivo. |
| Impuesta por el cliente | Prácticas DevSecOps | Pipelines de **CI** y **CD** funcionales, análisis estático con **Sonar** y escaneo de vulnerabilidades con **Trivy**. | Automatiza la calidad y la seguridad desde la línea base (DRV-007). |
| Impuesta por el cliente | Calidad | Pruebas automatizadas siguiendo la **pirámide de pruebas**: unitarias, integración y End-to-End. | Asegura la calidad de forma repetible y rápida. |
| Impuesta por el cliente | Despliegue | La solución debe estar **publicada en una URL pública** utilizable por el docente. | Permite validar el flujo de publicación de Kudos en un entorno real. |
| Impuesta por el cliente | Experiencia de usuario | Diseño **Mobile First** (≈90 % de usuarios desde móvil). | Respuesta directa al driver DRV-001. |
| Impuesta por el cliente | Integraciones | Notificaciones por **correo electrónico**, **WhatsApp** y **Push** en tiempo real. | Driver DRV-003 y flujo obligatorio de la línea base. |
| Impuesta por el cliente | Organización del código | Repositorios separados de **Front-End** y **Back-End**. | Entregables obligatorios; permite ciclos de vida y pipelines independientes. |
| Propia del proyecto | Tecnología back-end | **Java 25 (LTS) + Spring Boot 4.1**. | Stack ya utilizado por la estudiante en el proyecto AgroTrack. Java 25 es la versión LTS vigente y Spring Boot 4.1, la línea estable recomendada para proyectos nuevos, la soporta oficialmente. Ofrece un ecosistema maduro (Spring Security, Spring Data JPA, Actuator, WebSocket, Micrometer/OpenTelemetry) y soporte de largo plazo. |
| Propia del proyecto | Tecnología front-end | **Angular** con diseño responsivo Mobile First. | Framework dominado por la estudiante, con tipado fuerte (TypeScript), arquitectura por módulos/componentes y buen soporte de PWA. |
| Propia del proyecto | Persistencia | **PostgreSQL** como base de datos relacional. | Integridad referencial, índices únicos parciales (unicidad de roles), consultas recursivas (réplicas) y planes gestionados gratuitos. |
| Propia del proyecto | Contenedores | Empaquetar el back-end como **imagen Docker**. | Paridad entre entornos (factor X de Twelve-Factor) y requisito para el escaneo con Trivy. |
| Propia del proyecto | Prácticas de diseño | Seguir los principios **SOLID**. | Software mantenible, extensible (por ejemplo, agregar un nuevo canal de notificación sin modificar el caso de uso) y fácil de probar. |
| Propia del proyecto | Patrones de diseño | Usar patrones **GoF** y **GRASP** pertinentes, y principios **DRY** y **KISS**. | Se evalúa la pertinencia, no la cantidad: cada patrón se justifica por el beneficio obtenido. |
| Propia del proyecto | Herramientas DevOps | **GitHub** + **GitHub Actions** para el control de versiones y los pipelines. | Gratuito, integrado con SonarCloud y Trivy, y con evidencias visibles para la entrega. |
| Propia del proyecto | Marco metodológico | Gestión ágil con un tablero **Kanban** y entregas incrementales. | Con una sola persona y cuatro días de plazo, Kanban da visibilidad sin la ceremonia de Scrum. |
