package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.evento.EventoDominio;
import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KudoTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T15:00:00Z");

    private final Usuario autor = usuario(EstadoUsuario.ACTIVO);
    private final Usuario destinatario = usuario(EstadoUsuario.ACTIVO);
    private final Categoria categoria = new Categoria(UUID.randomUUID(), "Liderazgo", true);

    @Test
    void publicaKudoValidoYEmiteEventoKudoPublicado() {
        UUID id = UUID.randomUUID();

        Kudo kudo = Kudo.publicar(id, autor, destinatario, categoria, "Gracias por tu apoyo", List.of(), AHORA);

        assertThat(kudo.getAutorId()).isEqualTo(autor.getId());
        assertThat(kudo.getDestinatarioId()).isEqualTo(destinatario.getId());
        assertThat(kudo.getFechaPublicacion()).isEqualTo(AHORA);
        List<EventoDominio> eventos = kudo.extraerEventos();
        assertThat(eventos).singleElement().isInstanceOf(KudoPublicado.class);
        assertThat(eventos.getFirst().idCorrelacion()).isEqualTo(id);
        assertThat(kudo.extraerEventos()).isEmpty();
    }

    @Test
    void conservaContenidoExtensoSinTruncar() {
        String contenido = "a".repeat(100_000);

        Kudo kudo = Kudo.publicar(UUID.randomUUID(), autor, destinatario, categoria, contenido, List.of(), AHORA);

        assertThat(kudo.getContenido()).hasSize(100_000);
    }

    @Test
    void rechazaContenidoVacio() {
        assertThatThrownBy(() -> Kudo.publicar(UUID.randomUUID(), autor, destinatario, categoria, "  ", List.of(), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("campo").isEqualTo("contenido");
    }

    @Test
    void rechazaDestinatarioAusente() {
        assertThatThrownBy(() -> Kudo.publicar(UUID.randomUUID(), autor, null, categoria, "Hola", List.of(), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("campo").isEqualTo("destinatario");
    }

    @Test
    void rechazaAutoReconocimiento() {
        assertThatThrownBy(() -> Kudo.publicar(UUID.randomUUID(), autor, autor, categoria, "Hola", List.of(), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("AUTO_RECONOCIMIENTO");
    }

    @Test
    void rechazaDestinatarioInactivo() {
        Usuario inactivo = usuario(EstadoUsuario.INACTIVO);

        assertThatThrownBy(() -> Kudo.publicar(UUID.randomUUID(), autor, inactivo, categoria, "Hola", List.of(), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("DESTINATARIO_INACTIVO");
    }

    @Test
    void rechazaCategoriaInactiva() {
        Categoria inactiva = new Categoria(UUID.randomUUID(), "Antigua", false);

        assertThatThrownBy(() -> Kudo.publicar(UUID.randomUUID(), autor, destinatario, inactiva, "Hola", List.of(), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CATEGORIA_INACTIVA");
    }

    private static Usuario usuario(EstadoUsuario estado) {
        return new Usuario(UUID.randomUUID(), "Persona UCO", "persona@uco.net.co", "+573000000000",
                true, Rol.ESTUDIANTE, estado);
    }
}
