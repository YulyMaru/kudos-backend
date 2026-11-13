# SPIKE-007: Despliegue en Cloud Run con base de datos, secretos y observabilidad

- **ADR que valida**: ADR-001 (unidad desplegable), ADR-003 (PostgreSQL en Supabase), ADR-010 (Cloud Run + Firebase Hosting), ADR-012 (Observabilidad).
- **Estado**: Aprobado con cambios.
- **Timebox**: 3 horas.
- **Drivers relacionados**: DRV-005, DRV-006, DRV-007.

## Pregunta a responder

¿La aplicación se puede desplegar en Cloud Run conectada a Supabase, con los secretos fuera del código, un arranque en frío aceptable y logs, métricas y trazas visibles en Google Cloud Observability?

## Hipótesis

Con una imagen Docker multietapa, Secret Manager, Flyway y la instrumentación de Actuator, Micrometer y OpenTelemetry, el servicio queda accesible por URL pública HTTPS y observable de punta a punta. El arranque en frío observado en AgroTrack (27 s) se mitiga con una instancia mínima y una *startup probe* con margen.

## Riesgo que mitiga

No contar con una URL pública funcional el día de la entrega; credenciales expuestas; no poder demostrar la observabilidad.

## Alcance

- **Incluye**: Dockerfile, Artifact Registry, Cloud Run, Secret Manager, conexión a Supabase, migraciones Flyway, endpoints de salud, logs JSON, métricas personalizadas, trazas y front-end en Firebase Hosting.
- **No incluye**: varias instancias (máximo 1 en la línea base).

## Preparación

- Proyecto de GCP con facturación habilitada (capa gratuita) y proyecto de Supabase.
- Aplicación mínima con un endpoint de prueba y la migración inicial.

## Procedimiento

1. Construir la imagen multietapa (JRE 25, usuario no root) y medir su tamaño.
2. Desplegar en Cloud Run con los secretos de Secret Manager como variables de entorno.
3. Verificar que Flyway aplica las migraciones en Supabase.
4. Medir el arranque en frío con `min-instances=0` y luego con `min-instances=1`.
5. Verificar los endpoints de *liveness* y *readiness*.
6. Hacer una petición y seguirla en Cloud Logging (logs JSON con `traceId`) y en Cloud Trace.
7. Publicar la métrica personalizada `kudos.publicados` y verla en Cloud Monitoring.
8. Desplegar el front-end en Firebase Hosting y verificar CORS hacia el back-end.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | URL pública | Back-end y front-end accesibles por HTTPS | Sí | Cumplido |
| 2 | Secretos | Credenciales en código o imagen | 0 | Cumplido |
| 3 | Migraciones | Flyway aplicado en Supabase | Sí | Cumplido (V1 a V5) |
| 4 | Arranque en frío | Primera respuesta con `min-instances=1` | < 2 s | Cumplido con 1 instancia mínima |
| 5 | Arranque en frío | Primera respuesta con `min-instances=0` | Medir y documentar | Arranque lento: el front mostraba "No hay conexión" |
| 6 | Logs | Logs JSON con `traceId` en Cloud Logging | Sí | Cumplido (Cloud Logging) |
| 7 | Trazas | Traza visible en Cloud Trace | Sí | Evidencia complementaria |
| 8 | Métricas | Métrica personalizada en Cloud Monitoring | Sí | Cumplido (métricas del outbox con Micrometer) |
| 9 | Costo | Consumo fuera de la capa gratuita | $0 | Cumplido (capa gratuita y créditos) |

## Evidencias a recolectar

- URL pública y captura del servicio en Cloud Run.
- Capturas de Cloud Logging, Cloud Trace y Cloud Monitoring.
- Tiempos de arranque medidos.

## Resultados

Back-end en Cloud Run conectado a Supabase con secretos de Secret Manager; Flyway aplicó las migraciones; `/actuator/health` responde `UP`. Con `min-instances=0` el arranque en frío afectaba la primera petición.

## Conclusión

Aprobado con cambios.

## Impacto en el ADR

ADR-001, ADR-003 y ADR-012 pasan a Aceptado; ADR-010 a Aceptado con cambios (`min-instances=1` y Workload Identity Federation también para Firebase).
