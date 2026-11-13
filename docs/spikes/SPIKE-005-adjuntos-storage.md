# SPIKE-005: Adjuntos en bucket privado con validación por contenido y URL firmadas

- **ADR que valida**: ADR-007 (Almacenamiento de adjuntos).
- **Estado**: Aprobado.
- **Timebox**: 2 horas.
- **Funcionalidades relacionadas**: HU-18, HU-24.

## Pregunta a responder

¿Se pueden subir adjuntos a un bucket privado de Supabase Storage mediante la API S3, rechazar archivos peligrosos validando su contenido y permitir la descarga solo con URL firmadas que expiran?

## Hipótesis

Con Apache Tika para detectar el tipo real, el SDK de AWS para S3 apuntando a Supabase y URL prefirmadas de 5 minutos, se cumplen las estrategias de almacenamiento, seguridad, recuperación y trazabilidad.

## Riesgo que mitiga

Subida de archivos maliciosos, exposición pública de adjuntos y pérdida de archivos por el sistema de archivos efímero de Cloud Run.

## Alcance

- **Incluye**: bucket privado, adaptador `AlmacenamientoS3Adapter`, validación de tipo y tamaño, hash SHA-256, metadatos en PostgreSQL, URL firmadas, auditoría de subida y descarga.
- **No incluye**: análisis antivirus (evolución).

## Preparación

- Bucket privado en Supabase y claves S3 en variables de entorno.
- Archivos de prueba: PDF válido, PNG válido, ejecutable renombrado a `.pdf`, archivo de 15 MB.

## Procedimiento

1. Subir el PDF y el PNG válidos y verificar el objeto en el bucket y los metadatos en la base de datos.
2. Subir el ejecutable renombrado y verificar el rechazo por contenido.
3. Subir el archivo que supera el tamaño máximo y verificar el rechazo.
4. Intentar acceder al objeto sin URL firmada y verificar que se niega el acceso.
5. Generar una URL firmada, descargarla, esperar a que expire y verificar que ya no funciona.
6. Verificar que el hash del archivo descargado coincide con el registrado.
7. Verificar los eventos de auditoría `ADJUNTO_SUBIDO` y `ADJUNTO_DESCARGADO`.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Almacenamiento | Archivos válidos guardados con metadatos | 100 % | Cumplido |
| 2 | Validación por contenido | Archivos disfrazados aceptados | 0 | Cumplido (validación por firma) |
| 3 | Tamaño | Archivos sobre el límite aceptados | 0 | Cumplido |
| 4 | Privacidad | Accesos sin URL firmada | 0 | Cumplido (bucket privado) |
| 5 | Expiración | URL usable después de su vigencia | No | Cumplido (URL firmada con expiración) |
| 6 | Integridad | Hash descargado = hash registrado | Sí | Cumplido (pruebas automatizadas) |
| 7 | Trazabilidad | Subidas y descargas auditadas | 100 % | Cumplido |

## Evidencias a recolectar

- Capturas del bucket y de la tabla `adjunto`.
- Respuesta de error al subir el archivo disfrazado.
- Captura de la URL expirada.

## Resultados

Un Kudo con imagen adjunta se publicó en producción y el archivo quedó en el bucket privado `kudos-adjuntos` de Supabase Storage; la descarga se hace con URL firmada.

## Conclusión

Aprobado.

## Impacto en el ADR

ADR-007 pasa a Aceptado.
