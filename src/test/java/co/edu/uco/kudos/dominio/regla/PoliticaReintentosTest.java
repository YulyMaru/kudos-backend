package co.edu.uco.kudos.dominio.regla;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaReintentosTest {

    private final PoliticaReintentos politica = new PoliticaReintentos(5, Duration.ofSeconds(10), Duration.ofMinutes(10));

    @Test
    void elRetrasoCreceDeFormaExponencial() {
        assertThat(politica.calcularRetraso(1, 1.0)).isEqualTo(Duration.ofSeconds(10));
        assertThat(politica.calcularRetraso(2, 1.0)).isEqualTo(Duration.ofSeconds(20));
        assertThat(politica.calcularRetraso(3, 1.0)).isEqualTo(Duration.ofSeconds(40));
        assertThat(politica.calcularRetraso(4, 1.0)).isEqualTo(Duration.ofSeconds(80));
    }

    @Test
    void elJitterReduceElRetrasoHastaLaMitad() {
        assertThat(politica.calcularRetraso(2, 0.0)).isEqualTo(Duration.ofSeconds(10));
        assertThat(politica.calcularRetraso(2, 0.5)).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void elRetrasoNuncaSuperaElMaximo() {
        assertThat(politica.calcularRetraso(30, 1.0)).isEqualTo(Duration.ofMinutes(10));
        assertThat(politica.calcularRetraso(1000, 1.0)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void seAgotaAlAlcanzarElMaximoDeIntentos() {
        assertThat(politica.agotado(4)).isFalse();
        assertThat(politica.agotado(5)).isTrue();
    }

    @Test
    void rechazaConfiguracionesInvalidas() {
        assertThatThrownBy(() -> new PoliticaReintentos(0, Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PoliticaReintentos(3, Duration.ZERO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PoliticaReintentos(3, Duration.ofSeconds(10), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> politica.calcularRetraso(1, 1.5)).isInstanceOf(IllegalArgumentException.class);
    }
}
