# 08 · Drivers Arquitectónicos y Atributos de Calidad — UCO Kudos
## 1. Drivers arquitectónicos

### 1.1 Resumen

| ID | Driver | Tipo | Origen | Atributos de calidad asociados | ADRs |
|----|--------|------|--------|--------------------------------|------|
| DRV-001 | Movilidad (Mobile First) | Atributo de calidad | Enunciado | Usabilidad, Accesibilidad, Rendimiento, Eficiencia | ADR-009, ADR-014 |
| DRV-002 | Alta demanda de lectura | Atributo de calidad | Enunciado | Rendimiento, Escalabilidad, Eficiencia | ADR-003, ADR-009 |
| DRV-003 | Notificaciones multicanal | Funcional + calidad | Enunciado | Confiabilidad, Disponibilidad, Rendimiento | ADR-002, ADR-004, ADR-005, ADR-006 |
| DRV-004 | Adjuntos | Funcional + calidad | Enunciado | Seguridad, Confiabilidad, Disponibilidad | ADR-007 |
| DRV-005 | Seguridad | Atributo de calidad | Enunciado | Seguridad | ADR-007, ADR-008, ADR-011 |
| DRV-006 | Observabilidad | Atributo de calidad | Enunciado | Capacidad para ser soportado, Capacidad para ser administrado | ADR-012, ADR-013 |
| DRV-007 | Automatización (DevSecOps) | Atributo de calidad | Enunciado | Capacidad para ser desplegado, probado y mantenido | ADR-010, ADR-011 |
| DRV-008 | Integridad de las reglas organizacionales | Funcional + calidad | HU-04, HU-05, HU-14 y Mapa de Empatía (ESC-CAL-CON-049) | Confiabilidad, Seguridad | ADR-003, ADR-008 |
| DRV-009 | Plazo, equipo de una persona y presupuesto $0 | Restricción | Restricciones de negocio | Costo, Capacidad para ser desplegado, Mantenido | ADR-001, ADR-010, ADR-011 |

### 1.2 Detalle de cada driver

#### DRV-001 · Movilidad (Mobile First)
- **Descripción:** se estima que el 90 % de los usuarios usará la plataforma desde el celular, con redes móviles de calidad variable.
- **Implicación para la arquitectura:** la interfaz se diseña primero para pantallas pequeñas; las respuestas de la API deben ser livianas; la publicación no debe bloquear al usuario mientras se notifica.
- **Funcionalidades relacionadas:** HU-17, HU-21, HU-23.
- **Escenarios relacionados:** ESC-CAL-USA-001, ESC-CAL-ACC-063, ESC-CAL-REN-015, ESC-CAL-REN-017.
- **Decisiones:** SPA Angular Mobile First + PWA (ADR-014); DTO de lectura y compresión (ADR-009).

#### DRV-002 · Alta demanda de lectura
- **Descripción:** la carga es predominantemente de consulta (feed, detalle, categorías).
- **Implicación para la arquitectura:** separar el modelo de lectura del de escritura, paginar por cursor, indexar y usar caché para los datos de referencia.
- **Funcionalidades relacionadas:** HU-16, HU-23, HU-24.
- **Escenarios relacionados:** ESC-CAL-REN-011, ESC-CAL-REN-016, ESC-CAL-ESC-050.
- **Decisiones:** CQRS ligero, paginación por cursor y Caffeine (ADR-009); índices en PostgreSQL (ADR-003).

#### DRV-003 · Notificaciones multicanal
- **Descripción:** al publicar un Kudo, el destinatario debe recibir correo, WhatsApp y, si está conectado, una notificación Push en tiempo real; todo auditado.
- **Implicación para la arquitectura:** procesamiento asíncrono orientado a mensajes, aislamiento de fallas por canal, reintentos idempotentes y adaptadores intercambiables por proveedor.
- **Funcionalidades relacionadas:** HU-17, HU-19, HU-20, HU-21, HU-22.
- **Escenarios relacionados:** ESC-CAL-CON-041, ESC-CAL-CON-042, ESC-CAL-DIS-034, ESC-CAL-REN-013.
- **Decisiones:** Puertos y Adaptadores (ADR-001); Outbox con reintentos (ADR-002); WebSocket/STOMP (ADR-004); Brevo (ADR-005); canal móvil por SMS con Traccar (ADR-015; WhatsApp Cloud API rechazado en ADR-006).

#### DRV-004 · Adjuntos
- **Descripción:** los Kudos admiten archivos; se exigen estrategias de almacenamiento, seguridad, recuperación y trazabilidad.
- **Implicación para la arquitectura:** almacenamiento de objetos externo a la base de datos y al contenedor; validación por contenido; URL firmadas; auditoría de subidas y descargas.
- **Funcionalidades relacionadas:** HU-18, HU-24.
- **Escenarios relacionados:** ESC-CAL-SEG-027, ESC-CAL-SEG-028, ESC-CAL-REN-018.
- **Decisiones:** Supabase Storage privado con API S3 (ADR-007).

#### DRV-005 · Seguridad
- **Descripción:** la arquitectura debe incorporar mecanismos de seguridad apropiados.
- **Implicación para la arquitectura:** autenticación sin estado, autorización por rol y por alcance, secretos fuera del código, HTTPS, protección de datos personales (Ley 1581) y escaneo de vulnerabilidades en el pipeline.
- **Funcionalidades relacionadas:** HU-06, HU-14, HU-29.
- **Escenarios relacionados:** ESC-CAL-SEG-020 (n.º 1 del Top 10), ESC-CAL-SEG-021, ESC-CAL-SEG-022, ESC-CAL-SEG-029, ESC-CAL-DES-102.
- **Decisiones:** JWT + RBAC (ADR-008); adjuntos privados (ADR-007); Trivy y Sonar (ADR-011).

#### DRV-006 · Observabilidad
- **Descripción:** la arquitectura debe incorporar monitoreo, métricas, trazas y auditoría.
- **Implicación para la arquitectura:** instrumentación neutral (Micrometer y OpenTelemetry), logs estructurados con `traceId`, métricas de negocio por canal y auditoría inalterable separada de los logs.
- **Funcionalidades relacionadas:** HU-22, HU-30, HU-31.
- **Escenarios relacionados:** ESC-CAL-SOP-080, ESC-CAL-SOP-077, ESC-CAL-ADM-069.
- **Decisiones:** Google Cloud Observability (ADR-012); auditoría de solo inserción (ADR-013).

#### DRV-007 · Automatización (DevSecOps)
- **Descripción:** capacidades DevSecOps desde la línea base: CI, CD, Sonar y Trivy obligatorios.
- **Implicación para la arquitectura:** artefacto inmutable en contenedor, configuración por entorno, pruebas automatizadas en los tres niveles y compuertas que bloquean el despliegue.
- **Escenarios relacionados:** ESC-CAL-DES-097, ESC-CAL-DES-098, ESC-CAL-DES-102.
- **Decisiones:** Cloud Run + Firebase Hosting (ADR-010); GitHub Actions + SonarQube Cloud + Trivy (ADR-011).

#### DRV-008 · Integridad de las reglas organizacionales *(derivado)*
- **Descripción:** cada persona tiene un único rol activo; existe un único rector, coordinador académico, coordinador por facultad y coordinador por programa activos; la cuenta solo se activa con aprobación más doble verificación.
- **Por qué es un driver:** los cuatro directivos lo votaron en el Mapa de Empatía (ESC-CAL-CON-049, puesto 4 del Top 10). Además, solo se puede garantizar bajo concurrencia con restricciones en la base de datos, lo que condiciona la elección del motor.
- **Funcionalidades relacionadas:** HU-04, HU-05, HU-14.
- **Escenarios relacionados:** ESC-CAL-CON-049, ESC-CAL-CON-045, ESC-CAL-SEG-031.
- **Decisiones:** índices únicos parciales en PostgreSQL (ADR-003); autorización por alcance (ADR-008).

#### DRV-009 · Plazo, equipo de una persona y presupuesto $0 *(derivado de las restricciones de negocio)*
- **Descripción:** entrega el 13 de noviembre de 2026, desarrollo individual y sin presupuesto.
- **Por qué es un driver:** descarta las arquitecturas distribuidas, los brokers externos y la infraestructura autoadministrada, y favorece los servicios gestionados con capa gratuita.
- **Escenarios relacionados:** ESC-CAL-COS-084, ESC-CAL-COS-086.
- **Decisiones:** una sola unidad desplegable con Puertos y Adaptadores (ADR-001); servicios gestionados (ADR-003, ADR-007, ADR-010); pipeline sin infraestructura propia (ADR-011).

---

## 2. Tácticas arquitectónicas por atributo crítico e importante

| Atributo | Tácticas aplicadas | Dónde se materializan |
|---|---|---|
| Seguridad | Autenticar actores (JWT), autorizar por rol y alcance, limitar intentos, cifrar en tránsito (HTTPS), proteger secretos, validar entradas y archivos, auditar | ADR-007, ADR-008, ADR-011, ADR-013 |
| Confiabilidad | Transacciones atómicas, Outbox, reintento con backoff, idempotencia, restricciones de integridad | ADR-002, ADR-003 |
| Disponibilidad | Health checks, reemplazo automático de instancias, aislamiento de fallas por canal, copias de seguridad gestionadas | ADR-002, ADR-003, ADR-010 |
| Usabilidad | Mobile First, flujos de máximo 3 pasos, validación en línea, retroalimentación inmediata | ADR-014 |
| Rendimiento | Procesamiento asíncrono, paginación por cursor, índices, DTO de lectura, caché, compresión | ADR-002, ADR-009 |
| Capacidad para ser administrado | Gestión jerárquica, consulta de auditoría, configuración externa | ADR-008, ADR-013 |
| Capacidad para ser soportado | Logs estructurados con `traceId`, métricas por canal, trazas, endpoints de salud | ADR-012 |
| Escalabilidad | Procesos sin estado, escalado horizontal, puertos que permiten extraer componentes | ADR-001, ADR-010 |

---

## 3. Trade-offs identificados

| Tensión | Descripción | Decisión |
|---|---|---|
| **Seguridad vs. Usabilidad** | Tokens de vida corta (15 min) obligarían a iniciar sesión con frecuencia. | *Refresh token* rotativo: la sesión se renueva sin intervención del usuario (ADR-008). |
| **Rendimiento vs. Confiabilidad** | Responder al autor sin esperar a los proveedores implica que las notificaciones se completan unos segundos después (consistencia eventual). | Se acepta: el Outbox garantiza la entrega y la auditoría registra el estado final de cada canal (ADR-002). |
| **Escalabilidad vs. Simplicidad** | El broker WebSocket en memoria impide ejecutar varias instancias, lo que limita el escalado horizontal que pide ESC-CAL-ESC-050. | En la línea base, `max-instances=1`; la evolución documentada es un *broker relay* (RabbitMQ con STOMP o Redis Pub/Sub) sin cambiar los casos de uso (ADR-004, ADR-010). |
| **Disponibilidad vs. Costo** | Evitar el arranque en frío exige mantener una instancia encendida, lo que consume capa gratuita. | `min-instances=1` (adoptado en producción) solo durante la ventana de evaluación (ADR-010). |
| **Seguridad vs. Rendimiento** | Validar cada adjunto por contenido y generar URL firmadas agrega latencia. | Se acepta: la seguridad es el atributo n.º 1 y el costo es de milisegundos (ADR-007). |

---

## 4. Matriz de trazabilidad

| Driver | Atributo(s) | Escenarios (Top 10 en negrita) | ADRs | Spikes |
|---|---|---|---|---|
| DRV-001 | Usabilidad, Accesibilidad, Rendimiento | **USA-001**, **ACC-063**, REN-015, REN-017 | ADR-009, ADR-014 | SPIKE-002 |
| DRV-002 | Rendimiento, Escalabilidad | **REN-011**, **ESC-050**, REN-016 | ADR-003, ADR-009 | SPIKE-008 |
| DRV-003 | Confiabilidad, Disponibilidad | **CON-041**, CON-042, DIS-034, REN-013 | ADR-001, ADR-002, ADR-004, ADR-005, ADR-006 | SPIKE-001, 002, 003, 004 |
| DRV-004 | Seguridad, Confiabilidad | SEG-027, SEG-028, REN-018 | ADR-007 | SPIKE-005 |
| DRV-005 | Seguridad | **SEG-020**, SEG-021, SEG-022, SEG-029, DES-102 | ADR-007, ADR-008, ADR-011 | SPIKE-002, SPIKE-006 |
| DRV-006 | Soportado, Administrado | **SOP-080**, SOP-077, ADM-069 | ADR-012, ADR-013 | SPIKE-001, SPIKE-007 |
| DRV-007 | Desplegado, Probado, Mantenido | DES-097, DES-098, DES-102 | ADR-010, ADR-011 | SPIKE-006, SPIKE-007 |
| DRV-008 | Confiabilidad, Seguridad | **CON-049**, **ADM-073**, CON-045, SEG-031 | ADR-003, ADR-008 | SPIKE-001 |
| DRV-009 | Costo | COS-084, COS-086 | ADR-001, ADR-010, ADR-011 | SPIKE-007 |


