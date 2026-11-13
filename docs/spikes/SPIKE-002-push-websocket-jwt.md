# SPIKE-002: Notificación Push en tiempo real con WebSocket/STOMP autenticado con JWT

- **ADR que valida**: ADR-004 (WebSocket + STOMP), ADR-008 (JWT + RBAC), ADR-014 (Angular Mobile First).
- **Estado**: Aprobado.
- **Timebox**: 2 horas.
- **Funcionalidades relacionadas**: HU-06, HU-21.

## Pregunta a responder

¿Un usuario con sesión abierta en un celular real recibe en pocos segundos la notificación de un Kudo, **solo él**, a través de una conexión WebSocket autenticada con el mismo JWT de la API, y la conexión se recupera tras un corte?

## Hipótesis

Validando el JWT en el `CONNECT` de STOMP y enviando a `/user/queue/notificaciones`, Spring entrega el mensaje solo al usuario dueño de la sesión, y RxStomp reconecta automáticamente tras una pérdida de red.

## Riesgo que mitiga

Notificaciones que no llegan, que llegan a otro usuario o canales sin autenticación; incompatibilidades del WebSocket con Cloud Run o con navegadores móviles.

## Alcance

- **Incluye**: endpoint `/ws`, interceptor de autenticación, envío a destino de usuario, cliente Angular con RxStomp, prueba en un celular real contra el entorno desplegado.
- **No incluye**: varias instancias con broker externo (documentado como evolución en ADR-004).

## Preparación

- Back-end desplegado en Cloud Run (SPIKE-007) y front-end en Firebase Hosting.
- Dos usuarios de prueba (A y B) y un tercer usuario C conectado.

## Procedimiento

1. Conectar al usuario B desde un celular y al usuario C desde un computador.
2. El usuario A publica un Kudo para B; medir el tiempo hasta que aparece la notificación en el celular de B.
3. Verificar que C no recibe nada.
4. Intentar conectar sin token y con un token vencido; verificar el rechazo.
5. Con un token válido de C, intentar suscribirse a `/user/B/queue/notificaciones`; verificar que no recibe mensajes de B.
6. Activar el modo avión en el celular de B por 30 s, desactivarlo y verificar la reconexión automática.
7. Publicar un Kudo para un usuario desconectado; verificar que no hay error y que el outbox marca `NO_APLICA`.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Latencia | Publicación → notificación visible | < 3 s | Cumplido (inmediato) |
| 2 | Privacidad | Notificaciones recibidas por otros usuarios | 0 | Cumplido |
| 3 | Autenticación | Conexiones aceptadas sin token o con token vencido | 0 | Cumplido (pruebas automatizadas) |
| 4 | Reconexión | Reconexión automática tras corte de 30 s | Sí, < 10 s | Cumplido (reconexión automática del cliente STOMP) |
| 5 | Usuario desconectado | Errores al notificar a un usuario desconectado | 0 | Cumplido |
| 6 | Mobile First | Notificación legible en pantalla de 360 px | Sí | Cumplido |

## Evidencias a recolectar

- Video o capturas del celular recibiendo la notificación.
- Logs del back-end con el `traceId` del envío.
- Captura del rechazo de la conexión sin token.

## Resultados

En la aplicación publicada, la ventana del destinatario mostró el aviso Push sin recargar en cuanto el docente publicó el Kudo; otros usuarios conectados no lo recibieron. El interceptor STOMP rechaza conexiones sin JWT válido.

## Conclusión

Aprobado.

## Impacto en el ADR

ADR-004, ADR-008 y ADR-014 pasan a Aceptado.
