# SPIKE-004: Envío de WhatsApp con WhatsApp Cloud API (Meta)

- **ADR que valida**: ADR-006 (Proveedor de WhatsApp).
- **Estado**: Rechazado.
- **Timebox**: 2 horas.
- **Funcionalidades relacionadas**: HU-20.

## Pregunta a responder

¿Se puede enviar desde Cloud Run un mensaje de WhatsApp con una plantilla, usando el número de prueba de Meta, al número del docente, y cuánto tarda la aprobación de una plantilla propia?

## Hipótesis

Con el número de prueba, el destinatario registrado y una plantilla aprobada, la API entrega el mensaje en segundos; una plantilla de tipo *utilidad* se aprueba dentro del plazo del proyecto.

## Riesgo que mitiga

Es el canal más riesgoso: depende de aprobaciones, destinatarios de prueba y tokens con vencimiento, todo fuera del control del código.

## Alcance

- **Incluye**: app de Meta para desarrolladores, número de prueba, registro de destinatarios, plantilla, adaptador `WhatsAppCloudAdapter` con `RestClient`, token por variable de entorno.
- **Plan B**: si la plantilla no se aprueba o el registro falla, repetir con el sandbox de Twilio (`WhatsAppTwilioAdapter`).

## Preparación

- Cuenta de Meta for Developers con una app de tipo empresa.
- Número del docente (con su consentimiento) y número propio para pruebas.

## Procedimiento

1. Revisar y anotar las condiciones vigentes del entorno de pruebas (destinatarios permitidos, vigencia del token, plantillas disponibles).
2. Registrar y verificar los números destinatarios de prueba.
3. Enviar la plantilla de ejemplo desde Postman o curl para confirmar la configuración.
4. Solicitar la plantilla propia: «Hola {{1}}, {{2}} te reconoció en UCO Kudos en la categoría {{3}}». Anotar la hora de solicitud y de aprobación.
5. Enviar desde el adaptador desplegado en Cloud Run.
6. Generar un token de sistema de larga duración para no depender del token temporal.
7. Probar con un token inválido y verificar el reintento y la auditoría; probar un número no registrado y verificar que se trata como error definitivo.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Entrega | Mensaje recibido en el celular del destinatario | Sí | No |
| 2 | Latencia | Envío → recepción | < 10 s | No aplica |
| 3 | Plantilla propia | Tiempo de aprobación | Antes del 12-nov-2026 | No (Meta bloqueó la cuenta de desarrollador) |
| 4 | Token | Token de larga duración configurado | Sí | No |
| 5 | Errores | Errores transitorios reintentados y definitivos no reintentados | Sí | Cumplido (pruebas del adaptador) |
| 6 | Costo | Costo del envío | $0 | No aplica |

## Evidencias a recolectar

- Captura del mensaje en el celular.
- Captura de la plantilla aprobada.
- Logs y auditoría del envío.

## Resultados

Meta bloqueó la cuenta de desarrollador; el sandbox de Twilio no se activó; la cuenta de WhatsApp Business creada con WATI fue desactivada por Meta el 13-11-2026. Detalle en el ADR-006.

## Conclusión

Rechazado.

## Impacto en el ADR

ADR-006 pasa a Rechazado y se crea el ADR-015 (SMS con Traccar SMS Gateway), validado con el SPIKE-009.
