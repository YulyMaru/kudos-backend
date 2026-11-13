# SPIKE-008: Rendimiento del feed con paginación por cursor

- **ADR que valida**: ADR-003 (PostgreSQL), ADR-009 (Estrategia de alta lectura).
- **Estado**: Aprobado con cambios.
- **Timebox**: 1,5 horas.
- **Driver relacionado**: DRV-002 (Alta demanda de lectura).
- **Funcionalidades relacionadas**: HU-23.

## Pregunta a responder

¿La paginación por cursor con un índice compuesto y DTO de lectura mantiene un tiempo de respuesta estable en páginas profundas, frente a la paginación por *offset*, y evita elementos repetidos cuando se publican Kudos mientras se navega?

## Hipótesis

Con 10 000 Kudos, la consulta por cursor responde en tiempo prácticamente constante en cualquier página, mientras que *offset* se degrada en páginas profundas; además, el cursor no repite elementos al insertar nuevos Kudos.

## Riesgo que mitiga

Un feed lento en móviles cuando crezca el volumen de Kudos, y duplicados en el desplazamiento infinito.

## Alcance

- **Incluye**: script de carga de 10 000 Kudos, endpoints con cursor y con *offset*, `EXPLAIN ANALYZE`, prueba de carga ligera.
- **No incluye**: caché distribuida.

## Preparación

- Base de datos de prueba (Testcontainers o Supabase) con datos sintéticos.
- Herramienta de carga (k6 o JMeter).

## Procedimiento

1. Cargar 10 000 Kudos con fechas distribuidas.
2. Medir la página 1 y la página 400 con *offset* y con cursor (`EXPLAIN ANALYZE`).
3. Verificar que la consulta por cursor usa el índice `(fecha_publicacion DESC, id DESC)`.
4. Navegar 3 páginas, insertar 5 Kudos nuevos y seguir navegando; contar repetidos en cada estrategia.
5. Ejecutar una prueba de carga con 200 usuarios virtuales sobre la primera página del feed y medir el p95.
6. Medir el tamaño de la respuesta comprimida de una página de 20 Kudos.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Estabilidad | Tiempo página 400 / página 1 (cursor) | ≈ 1 (< 1,5) | No medido |
| 2 | Uso de índice | Plan de ejecución con *Index Scan* | Sí | No medido |
| 3 | Sin duplicados | Elementos repetidos con cursor | 0 | Cumplido (pruebas automatizadas) |
| 4 | Carga | p95 con 200 usuarios concurrentes | < 500 ms | No medido |
| 5 | Tamaño | Respuesta comprimida por página | < 50 KB | No medido |

## Evidencias a recolectar

- Salida de `EXPLAIN ANALYZE` de ambas estrategias.
- Reporte de la prueba de carga.
- Tabla comparativa de tiempos.

## Resultados

La paginación por cursor está implementada y probada funcionalmente. La prueba de carga con 10 000 Kudos no se ejecutó dentro del plazo.

## Conclusión

Aprobado con cambios.

## Impacto en el ADR

ADR-003 pasa a Aceptado y ADR-009 a Aceptado con cambios; la prueba de carga queda como trabajo futuro.
