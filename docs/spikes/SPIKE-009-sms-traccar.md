# SPIKE-009: Canal móvil por SMS con Traccar SMS Gateway

- **ADR que valida**: ADR-015 (Canal móvil por SMS con Traccar SMS Gateway).
- **Estado**: Aprobado.
- **Timebox**: 1 hora.
- **Funcionalidades relacionadas**: HU-20.

## Pregunta a responder

¿Se puede entregar la notificación móvil de un Kudo a cualquier número de celular desde Cloud Run, sin aprobaciones de terceros y con costo $0?

## Hipótesis

Si un celular Android con Traccar SMS Gateway (instalado desde Play Store) actúa como pasarela, una petición HTTPS a la API en la nube de Traccar con el token del dispositivo produce un SMS en el número indicado.

## Riesgo que mitiga

Dejar el flujo obligatorio sin notificación móvil tras el rechazo del ADR-006.

## Alcance

- **Incluye**: instalación de la app, token en la nube, envío de prueba, adaptador `CanalSmsTraccar` y despliegue.
- **No incluye**: confirmación de entrega por parte del operador.

## Procedimiento

1. Instalar Traccar SMS Gateway desde Play Store, conceder el permiso de SMS y activar el servicio.
2. Copiar el token de la nube y guardarlo en Secret Manager (`kudos-sms-token`).
3. Enviar desde Cloud Shell `POST https://www.traccar.org/sms/` a un número externo.
4. Implementar el adaptador y sus pruebas, desplegar y publicar un Kudo.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Entrega | SMS recibido en un número externo | Sí | Cumplido |
| 2 | Latencia | Petición → SMS recibido | < 30 s | Cumplido (segundos) |
| 3 | Sin registro previo | Acciones requeridas al destinatario | 0 | Cumplido |
| 4 | Costo | Costo del envío | $0 | Cumplido (plan del celular) |
| 5 | Seguridad | Protecciones del dispositivo desactivadas | 0 | Cumplido (app de Play Store) |
| 6 | Acoplamiento | Cambios en dominio, caso de uso u outbox | 0 | Cumplido |

## Evidencias a recolectar

- Captura de la respuesta en Cloud Shell (`25-traccar-prueba.png`) y del SMS recibido (`03-sms.png`).
- Fila del outbox del canal móvil en `ENVIADO`.

## Resultados

La API respondió `{"successCount":1,"failureCount":0,...,"success":true}` y el SMS llegó al número de prueba. El adaptador se integró sin cambiar el núcleo.

## Conclusión

Aprobado.

## Impacto en el ADR

ADR-015 pasa a Aceptado.
