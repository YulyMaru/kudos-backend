package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.regla.PoliticaReintentos;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private final PoliticaReintentos politica = new PoliticaReintentos(3, Duration.ofSeconds(10), Duration.ofMinutes(5));

    @Test
    void alEnviarseQuedaEnviadaYSumaUnIntento() {
        Notificacion enviada = pendiente().marcarEnviada();

        assertThat(enviada.getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
        assertThat(enviada.getIntentos()).isEqualTo(1);
        assertThat(enviada.getProximoIntento()).isNull();
        assertThat(enviada.puedeProcesarse()).isFalse();
    }

    @Test
    void unFalloProgramaUnReintentoConBackoff() {
        Notificacion fallida = pendiente().registrarFallo("timeout", AHORA, politica, 1.0);

        assertThat(fallida.getEstado()).isEqualTo(EstadoNotificacion.REINTENTANDO);
        assertThat(fallida.getIntentos()).isEqualTo(1);
        assertThat(fallida.getProximoIntento()).isEqualTo(AHORA.plusSeconds(10));
        assertThat(fallida.getUltimoError()).isEqualTo("timeout");
        assertThat(fallida.puedeProcesarse()).isTrue();
    }

    @Test
    void alAgotarLosIntentosQuedaFallida() {
        Notificacion notificacion = pendiente()
                .registrarFallo("error 1", AHORA, politica, 1.0)
                .registrarFallo("error 2", AHORA, politica, 1.0)
                .registrarFallo("error 3", AHORA, politica, 1.0);

        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.FALLIDO);
        assertThat(notificacion.getIntentos()).isEqualTo(3);
        assertThat(notificacion.getProximoIntento()).isNull();
    }

    @Test
    void unaNotificacionFinalizadaNoPuedeVolverAEnviarse() {
        Notificacion enviada = pendiente().marcarEnviada();

        assertThatThrownBy(enviada::marcarEnviada).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> enviada.registrarFallo("x", AHORA, politica, 0.5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void puedeMarcarseComoNoAplicaConSuMotivo() {
        Notificacion noAplica = pendiente().marcarNoAplica("Usuario desconectado");

        assertThat(noAplica.getEstado()).isEqualTo(EstadoNotificacion.NO_APLICA);
        assertThat(noAplica.getUltimoError()).isEqualTo("Usuario desconectado");
    }

    @Test
    void conservaSuIdentidadEntreTransiciones() {
        Notificacion original = pendiente();

        Notificacion enviada = original.marcarEnviada();

        assertThat(enviada.getId()).isEqualTo(original.getId());
        assertThat(enviada.getIdEvento()).isEqualTo(original.getIdEvento());
        assertThat(enviada.getDatos()).isEqualTo(original.getDatos());
        assertThat(original.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
    }

    private static Notificacion pendiente() {
        return Notificacion.pendiente(UUID.randomUUID(), UUID.randomUUID(), Canal.EMAIL, "ana@uco.net.co",
                Map.of("nombreAutor", "Ana"), AHORA);
    }
}
