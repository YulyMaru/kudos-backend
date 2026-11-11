package co.edu.uco.kudos.dominio.regla;

import java.time.Duration;
import java.util.Objects;

public record PoliticaReintentos(int maximoIntentos, Duration retrasoBase, Duration retrasoMaximo) {

    public PoliticaReintentos {
        Objects.requireNonNull(retrasoBase, "retrasoBase");
        Objects.requireNonNull(retrasoMaximo, "retrasoMaximo");
        if (maximoIntentos < 1) {
            throw new IllegalArgumentException("maximoIntentos debe ser al menos 1");
        }
        if (retrasoBase.isNegative() || retrasoBase.isZero()) {
            throw new IllegalArgumentException("retrasoBase debe ser positivo");
        }
        if (retrasoMaximo.compareTo(retrasoBase) < 0) {
            throw new IllegalArgumentException("retrasoMaximo no puede ser menor que retrasoBase");
        }
    }

    public boolean agotado(int intentosRealizados) {
        return intentosRealizados >= maximoIntentos;
    }

    public Duration calcularRetraso(int intentoFallido, double aleatorio) {
        if (intentoFallido < 1) {
            throw new IllegalArgumentException("intentoFallido debe ser al menos 1");
        }
        if (aleatorio < 0 || aleatorio > 1) {
            throw new IllegalArgumentException("aleatorio debe estar entre 0 y 1");
        }
        double exponencial = retrasoBase.toMillis() * Math.pow(2, Math.min(intentoFallido - 1, 30));
        double limitado = Math.min(exponencial, retrasoMaximo.toMillis());
        long conJitter = (long) (limitado * (0.5 + aleatorio * 0.5));
        return Duration.ofMillis(Math.max(conJitter, 1));
    }
}
