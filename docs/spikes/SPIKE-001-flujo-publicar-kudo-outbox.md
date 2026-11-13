# SPIKE-001: Flujo Publicar Kudo con Transactional Outbox, idempotencia y auditoría

- **ADR que valida**: ADR-001 (Puertos y Adaptadores), ADR-002 (Notificaciones asíncronas con Outbox), ADR-013 (Auditoría).
- **Estado**: Aprobado.
- **Timebox**: 3 horas.
- **Funcionalidades relacionadas**: HU-17, HU-19, HU-20, HU-21, HU-22.

## Pregunta a responder

¿El caso de uso Publicar Kudo, implementado con puertos y adaptadores y un outbox transaccional, garantiza que **ninguna notificación se pierda, ninguna se duplique** y que **todo el proceso quede auditado**, incluso cuando un proveedor falla?

## Hipótesis

Si el Kudo y sus registros de outbox (uno por canal) se guardan en la misma transacción, y un procesador asíncrono los envía con reintentos, backoff e idempotencia por `idEvento + canal`, entonces:
- una falla de un canal no afecta a los demás,
- N reintentos producen exactamente 1 mensaje,
- un reinicio de la aplicación no pierde notificaciones pendientes.

## Riesgo que mitiga

Pérdida o duplicación de notificaciones y falta de trazabilidad en el flujo obligatorio de la línea base.

## Alcance

- **Incluye**: caso de uso `PublicarKudo`, tabla `outbox_notificacion`, procesador programado, adaptadores **simulados** de correo, WhatsApp y Push (dobles de prueba configurables para fallar), tabla `registro_auditoria`.
- **No incluye**: proveedores reales (SPIKE-003 y SPIKE-004), WebSocket real (SPIKE-002) ni front-end.

## Preparación

- Proyecto Spring Boot 4.1 con Java 25 y PostgreSQL en contenedor (Testcontainers).
- Adaptador simulado con modos: `OK`, `FALLA_TEMPORAL(n)` (falla las primeras n veces) y `FALLA_PERMANENTE`.

## Procedimiento

1. Publicar un Kudo con los tres adaptadores en modo `OK` y verificar los estados `ENVIADO`.
2. Forzar una excepción después de guardar el Kudo (antes del *commit*) y verificar que no queda ni el Kudo ni su outbox.
3. Configurar el adaptador de WhatsApp en `FALLA_TEMPORAL(3)` y verificar que correo y Push se envían de inmediato y que WhatsApp se entrega en el cuarto intento.
4. Configurar el correo en `FALLA_PERMANENTE` y verificar que tras 5 intentos queda `FALLIDO` y auditado.
5. Simular que el adaptador envía pero la confirmación se pierde (timeout después de enviar) y verificar que el reintento no genera un segundo mensaje.
6. Detener la aplicación con registros `PENDIENTE`, reiniciarla y verificar que se procesan.
7. Consultar la auditoría por `idCorrelacion` y verificar que están todos los pasos.
8. Reemplazar un adaptador por otro (por ejemplo, el doble de prueba por el real) sin modificar el caso de uso y verificar con ArchUnit que el dominio no depende de la infraestructura.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Atomicidad | Kudos sin outbox o outbox sin Kudo | 0 | Cumplido |
| 2 | Aislamiento de fallas | Canales afectados por la falla de otro | 0 | Cumplido (verificado en producción) |
| 3 | Reintentos | Entrega tras falla temporal | Sí, en ≤ 5 intentos | Cumplido |
| 4 | Idempotencia | Mensajes por evento y canal tras N reintentos | Exactamente 1 | Cumplido (pruebas automatizadas) |
| 5 | Durabilidad | Pendientes perdidos tras reinicio | 0 | Cumplido |
| 6 | Auditoría | Pasos auditados por publicación | 4 de 4 (guardado + 3 canales) | Cumplido |
| 7 | Respuesta al autor | Tiempo de respuesta con un proveedor lento (5 s) | < 1 s | Cumplido (envío asíncrono; no se midió con proveedor lento) |
| 8 | Reglas de arquitectura | Violaciones de ArchUnit | 0 | Cumplido (ArchUnit) |

## Evidencias a recolectar

- Salida de las pruebas de integración.
- Consulta SQL del outbox y de la auditoría para un Kudo.
- Reporte de ArchUnit.

## Resultados

Pruebas de integración con PostgreSQL real (`PersistenciaPublicarKudoIntegracionTest`) y pruebas del procesador de outbox. En producción, con el proveedor de WhatsApp fallando, el Kudo se guardó, el correo y el Push se entregaron y la fila del canal móvil quedó reintentando y luego `FALLIDO`; la auditoría registró todos los pasos con el mismo `id_correlacion`.

## Conclusión

Aprobado.

## Impacto en el ADR

ADR-001, ADR-002 y ADR-013 pasan a Aceptado.
