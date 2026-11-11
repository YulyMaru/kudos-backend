package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasRegistroYAprobacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Test
    void laJerarquiaDeAprobacionSigueLaEstructuraInstitucional() {
        assertThat(ReglaAprobacion.rolAprobador(Rol.ESTUDIANTE)).isEqualTo(Rol.COORDINADOR_PROGRAMA);
        assertThat(ReglaAprobacion.rolAprobador(Rol.DOCENTE)).isEqualTo(Rol.COORDINADOR_PROGRAMA);
        assertThat(ReglaAprobacion.rolAprobador(Rol.COORDINADOR_PROGRAMA)).isEqualTo(Rol.COORDINADOR_FACULTAD);
        assertThat(ReglaAprobacion.rolAprobador(Rol.COORDINADOR_FACULTAD)).isEqualTo(Rol.COORDINADOR_ACADEMICO);
        assertThat(ReglaAprobacion.rolAprobador(Rol.COORDINADOR_ACADEMICO)).isEqualTo(Rol.RECTOR);
        assertThat(ReglaAprobacion.rolAprobador(Rol.RECTOR)).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(ReglaAprobacion.rolesQueAprueba(Rol.ADMINISTRADOR))
                .containsExactlyInAnyOrder(Rol.RECTOR, Rol.ADMINISTRADOR);
        assertThat(ReglaAprobacion.rolesQueAprueba(Rol.ESTUDIANTE)).isEmpty();
    }

    @Test
    void elCoordinadorDeFacultadSoloApruebaProgramasDeSuFacultad() {
        UUID facultad = UUID.randomUUID();
        UUID programa = UUID.randomUUID();
        Usuario coordinadorFacultad = new Usuario(UUID.randomUUID(), "Coord", "c@uco.edu.co", null, true, false,
                Rol.COORDINADOR_FACULTAD, EstadoUsuario.ACTIVO, null, 0, null, facultad, null);
        Usuario solicitante = Usuario.registrar("Nuevo", "n@uco.edu.co", "+573001112233", Rol.COORDINADOR_PROGRAMA,
                "hash", null, programa);
        SolicitudRegistro solicitud = SolicitudRegistro.crear(solicitante, null, programa, AHORA);

        assertThatCode(() -> ReglaAprobacion.validarPuedeAprobar(coordinadorFacultad, solicitud, facultad))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglaAprobacion.validarPuedeAprobar(coordinadorFacultad, solicitud, UUID.randomUUID()))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
    }

    @Test
    void validaCorreoInstitucionalYCelularInternacional() {
        List<String> dominios = List.of("uco.edu.co");

        assertThat(ReglaRegistro.normalizarCorreoInstitucional(" Ana@UCO.edu.co ", dominios)).isEqualTo("ana@uco.edu.co");
        assertThat(ReglaRegistro.normalizarCorreoInstitucional("ana@soy.uco.edu.co", dominios))
                .isEqualTo("ana@soy.uco.edu.co");
        assertThatThrownBy(() -> ReglaRegistro.normalizarCorreoInstitucional("ana@uco.edu.co.falso.com", dominios))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CORREO_NO_INSTITUCIONAL");
        assertThat(ReglaRegistro.normalizarCelular("+57 300 111-2233")).isEqualTo("+573001112233");
        assertThatThrownBy(() -> ReglaRegistro.normalizarCelular("3001112233"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CELULAR_INVALIDO");
    }

    @Test
    void cadaRolExigeSuAlcanceOrganizacional() {
        assertThatThrownBy(() -> ReglaRegistro.validarAlcance(Rol.ESTUDIANTE, null, null))
                .isInstanceOf(CampoObligatorioExcepcion.class).extracting("campo").isEqualTo("programaId");
        assertThatThrownBy(() -> ReglaRegistro.validarAlcance(Rol.COORDINADOR_FACULTAD, null, null))
                .isInstanceOf(CampoObligatorioExcepcion.class).extracting("campo").isEqualTo("facultadId");
        assertThatCode(() -> ReglaRegistro.validarAlcance(Rol.RECTOR, null, null)).doesNotThrowAnyException();
    }

    @Test
    void laModeracionEsExclusivaDelRectorYElCoordinadorAcademico() {
        assertThat(ReglaModeracion.puedeModerar(usuario(Rol.RECTOR))).isTrue();
        assertThat(ReglaModeracion.puedeModerar(usuario(Rol.COORDINADOR_ACADEMICO))).isTrue();
        assertThat(ReglaModeracion.puedeModerar(usuario(Rol.ADMINISTRADOR))).isFalse();
        assertThat(ReglaModeracion.puedeModerar(usuario(Rol.DOCENTE))).isFalse();
    }

    private static Usuario usuario(Rol rol) {
        return new Usuario(UUID.randomUUID(), "Persona", "p@uco.edu.co", null, false, rol, EstadoUsuario.ACTIVO);
    }
}
