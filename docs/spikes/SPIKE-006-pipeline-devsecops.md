# SPIKE-006: Pipeline DevSecOps con compuertas de calidad y seguridad

- **ADR que valida**: ADR-011 (CI/CD con GitHub Actions, SonarQube Cloud y Trivy).
- **Estado**: Aprobado.
- **Timebox**: 2 horas.
- **Driver relacionado**: DRV-007 (Automatización).

## Pregunta a responder

¿El pipeline de GitHub Actions **bloquea** el despliegue cuando falla una prueba, cuando no se cumple la compuerta de calidad de SonarQube Cloud o cuando Trivy detecta una vulnerabilidad crítica o alta corregible, y **despliega automáticamente** cuando todo pasa?

## Hipótesis

Encadenando los trabajos con dependencias (`needs`) y configurando Sonar con espera de la compuerta y Trivy con `exit-code: 1` para CRITICAL/HIGH, cualquier falla detiene el pipeline antes del despliegue.

## Riesgo que mitiga

Llegar a la entrega sin evidencias de CI, CD, Sonar y Trivy, o con compuertas que nunca fallan y, por tanto, no demuestran nada.

## Alcance

- **Incluye**: flujos de CI y CD del back-end y del front-end, SonarQube Cloud, Trivy (`fs`, `image`, `config`), Workload Identity Federation hacia GCP.
- **No incluye**: entornos adicionales (staging).

## Preparación

- Repositorios públicos en GitHub, proyectos en SonarQube Cloud, proyecto de GCP con Artifact Registry y Cloud Run.

## Procedimiento

1. Hacer push de un «hola mundo» y verificar que el pipeline completo despliega.
2. Introducir una prueba que falla; verificar que el pipeline se detiene y no despliega.
3. Introducir código con un problema bloqueante (por ejemplo, credencial en el código); verificar que la compuerta de Sonar falla.
4. Agregar una dependencia con una vulnerabilidad crítica conocida; verificar que Trivy detiene el pipeline.
5. Corregir cada falla y verificar el despliegue.
6. Medir la duración total del pipeline.

## Criterios de éxito

| # | Criterio | Métrica | Umbral | Resultado |
|---|----------|---------|--------|-----------|
| 1 | Despliegue automático | Merge a `main` → versión desplegada | Sí | Cumplido |
| 2 | Compuerta de pruebas | Despliegues con pruebas fallidas | 0 | Cumplido |
| 3 | Compuerta de Sonar | Despliegues con compuerta fallida | 0 | Cumplido (bloqueó con cobertura 63,6 %) |
| 4 | Compuerta de Trivy | Despliegues con CRITICAL/HIGH corregibles | 0 | Cumplido |
| 5 | Duración | Tiempo del pipeline completo | < 15 min | Cumplido (ver Actions) |
| 6 | Secretos | Llaves JSON de GCP en el repositorio | 0 | Cumplido (Workload Identity Federation) |

## Evidencias a recolectar

- Capturas de ejecuciones exitosas y de ejecuciones bloqueadas por cada compuerta.
- Captura del panel de SonarQube Cloud.
- Reporte de Trivy (SARIF en la pestaña de seguridad de GitHub).

## Resultados

Los pipelines de los dos repositorios despliegan automáticamente al hacer *push* a `main`. La compuerta de SonarQube Cloud detuvo un despliegue real por cobertura insuficiente del código nuevo hasta que se agregaron pruebas (98 %). No hay llaves de GCP en los repositorios.

## Conclusión

Aprobado.

## Impacto en el ADR

ADR-011 pasa a Aceptado.
