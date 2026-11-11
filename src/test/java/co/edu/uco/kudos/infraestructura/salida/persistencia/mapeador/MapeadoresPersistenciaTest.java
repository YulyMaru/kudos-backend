package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.KudoEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.NotificacionOutboxEntidad;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapeadoresPersistenciaTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T15:00:00Z");

    @Test
    void usuarioConservaTodosSusDatosAlIrYVolverDeLaEntidad() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "Ana", "ana@uco.net.co", "+573001111111", true,
                Rol.DOCENTE, EstadoUsuario.ACTIVO);

        Usuario reconstruido = UsuarioMapeador.aDominio(UsuarioMapeador.aEntidad(usuario));

        assertThat(reconstruido).usingRecursiveComparison().isEqualTo(usuario);
    }

    @Test
    void kudoConAdjuntosConservaSusDatosYEnlazaLosAdjuntosAlKudo() {
        Usuario autor = usuario();
        Usuario destinatario = usuario();
        Categoria categoria = new Categoria(UUID.randomUUID(), "Liderazgo", true);
        Adjunto adjunto = new Adjunto("foto.png", "image/png", 120, "a".repeat(64), "kudos/1/foto.png");
        Kudo kudo = Kudo.publicar(UUID.randomUUID(), autor, destinatario, categoria, "Gracias", List.of(adjunto), AHORA);

        KudoEntidad entidad = KudoMapeador.aEntidad(kudo);
        Kudo reconstruido = KudoMapeador.aDominio(entidad);

        assertThat(entidad.getAdjuntos()).singleElement()
                .satisfies(adjuntoEntidad -> assertThat(adjuntoEntidad.getKudo()).isSameAs(entidad));
        assertThat(reconstruido.getId()).isEqualTo(kudo.getId());
        assertThat(reconstruido.getAutorId()).isEqualTo(autor.getId());
        assertThat(reconstruido.getDestinatarioId()).isEqualTo(destinatario.getId());
        assertThat(reconstruido.getContenido()).isEqualTo("Gracias");
        assertThat(reconstruido.getAdjuntos()).containsExactly(adjunto);
        assertThat(reconstruido.extraerEventos()).isEmpty();
    }

    @Test
    void notificacionSeConvierteEnRegistroDeOutboxConSuPayload() {
        Notificacion notificacion = Notificacion.pendiente(UUID.randomUUID(), UUID.randomUUID(), Canal.EMAIL,
                "ana@uco.net.co", Map.of("nombreAutor", "Ana"), AHORA);

        NotificacionOutboxEntidad entidad = NotificacionMapeador.aEntidad(notificacion);

        assertThat(entidad.getId()).isEqualTo(notificacion.getId());
        assertThat(entidad.getIdEvento()).isEqualTo(notificacion.getIdEvento());
        assertThat(entidad.getCanal()).isEqualTo(Canal.EMAIL);
        assertThat(entidad.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(entidad.getPayload()).containsEntry("nombreAutor", "Ana");
        assertThat(entidad.getProximoIntento()).isEqualTo(AHORA);
        assertThat(entidad.isNew()).isTrue();
    }

    @Test
    void entradaDeAuditoriaConservaTodosSusCampos() {
        EntradaAuditoria entrada = EntradaAuditoria.de(AHORA, UUID.randomUUID(), usuario(),
                AccionAuditoria.KUDO_PUBLICADO, "KUDO", UUID.randomUUID(), Canal.PUSH, ResultadoAuditoria.EXITOSO,
                "detalle");

        EntradaAuditoria reconstruida = AuditoriaMapeador.aDominio(AuditoriaMapeador.aEntidad(entrada));

        assertThat(reconstruida).isEqualTo(entrada);
    }

    private static Usuario usuario() {
        return new Usuario(UUID.randomUUID(), "Persona", UUID.randomUUID() + "@uco.net.co", null, false,
                Rol.ESTUDIANTE, EstadoUsuario.ACTIVO);
    }
}
