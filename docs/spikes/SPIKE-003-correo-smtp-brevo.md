# SPIKE-003: Envío de correo mediante SMTP de Brevo

- **ADR que valida**: ADR-005 (Proveedor de correo).
- **Estado**: Aprobado con cambios.
- **Timebox**: 1 hora.
- **Funcionalidades relacionadas**: HU-19.

## Pregunta a responder

¿Un correo enviado desde la aplicación desplegada en Cloud Run mediante el SMTP de Brevo llega a la **bandeja de entrada** de un correo institucional y de uno personal, sin dominio propio y con costo $0?

## Hipótesis

Con un remitente verificado en Brevo y una plantilla HTML sencilla, el correo se entrega en menos de un minuto y no se clasifica como spam.

## Riesgo que mitiga

Que el correo institucional bloquee o clasifique como spam los mensajes, o que el plan gratuito tenga restricciones que impidan la demostración.

## Alcance

- **Incluye**: cuenta de Brevo, remitente verificado, adaptador `CorreoSmtpAdapter` con Spring Mail, plantilla HTML, credenciales por variables de entorno.
- **No incluye**: verificación de un dominio propio (evolución para producción).

## Preparación

- Crear la cuenta de Brevo, verificar el remitente y generar la clave SMTP.
- Guardar la clave en Secret Manager (o GitHub Secrets).

## Procedimiento

1. Revisar y anotar las condiciones vigentes del plan gratuito (cupo diario, restricciones).
2. Enviar un correo de prueba a un correo **institucional** y a uno **personal**.
3. Verificar en qué carpeta llega (entrada o spam) y medir el tiempo de entrega.
4. Revisar en el panel de Brevo el estado del envío.
5. Configurar una clave inválida y verificar que el adaptador lanza una excepción que el outbox trata como falla (reintento y auditoría).
6. Revisar que el correo no contenga datos sensibles y que el enlace al Kudo funcione.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Entrega institucional | Correo en bandeja de entrada | Sí | No probado (se usó correo personal) |
| 2 | Entrega personal | Correo en bandeja de entrada | Sí | Cumplido |
| 3 | Latencia | Envío → recepción | < 1 min | Cumplido |
| 4 | Costo | Costo del envío | $0 | Cumplido (plan gratuito) |
| 5 | Manejo de errores | Clave inválida produce falla controlada y auditada | Sí | Cumplido (pruebas automatizadas) |

## Evidencias a recolectar

- Captura del correo recibido en ambas cuentas.
- Captura del panel de Brevo.
- Registro de auditoría del envío.

## Resultados

El correo del Kudo llegó a la bandeja de entrada del destinatario (Gmail) enviado desde Cloud Run por `smtp-relay.brevo.com:587`. Brevo exigió verificar el teléfono de la cuenta y el remitente antes de habilitar el SMTP.

## Conclusión

Aprobado con cambios.

## Impacto en el ADR

ADR-005 pasa a Aceptado.
