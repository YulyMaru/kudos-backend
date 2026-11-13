package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.UsuarioDemostracionComando;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrearUsuariosDemostracionCasoUsoTest {

    private DoblesAutenticacion.CuentasEnMemoria cuentas;
    private CrearUsuariosDemostracionCasoUso casoUso;

    @BeforeEach
    void preparar() {
        cuentas = new DoblesAutenticacion.CuentasEnMemoria();
        GestorTransacciones transacciones = Runnable::run;
        casoUso = new CrearUsuariosDemostracionCasoUso(cuentas, new DoblesAutenticacion.CodificadorFalso(),
                new ArrayList<>()::add, transacciones,
                new DoblesAutenticacion.RelojAjustable(Instant.parse("2026-11-11T10:00:00Z")));
    }

    @Test
    void creaUsuariosActivosConCelularVerificadoYEsIdempotente() {
        List<UsuarioDemostracionComando> usuarios = List.of(
                comando("Docente Evaluador", "Docente@UCO.edu.co", "+573001234567", Rol.DOCENTE),
                comando("Estudiante Demo", "estudiante@uco.edu.co", "+573007654321", Rol.ESTUDIANTE));

        int primeraVez = casoUso.crearFaltantes(usuarios);
        int segundaVez = casoUso.crearFaltantes(usuarios);

        assertThat(primeraVez).isEqualTo(2);
        assertThat(segundaVez).isZero();
        assertThat(cuentas.usuarios.values()).hasSize(2).allSatisfy(usuario -> {
            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(usuario.puedeRecibirSms()).isTrue();
            assertThat(usuario.getHashContrasena()).isEqualTo("hash:Clave-Segura-2026");
        });
        assertThat(cuentas.usuarios.values()).extracting(Usuario::getCorreo)
                .containsExactlyInAnyOrder("docente@uco.edu.co", "estudiante@uco.edu.co");
    }

    @Test
    void rechazaCelularesQueNoEstanEnFormatoInternacional() {
        List<UsuarioDemostracionComando> usuarios = List.of(comando("Demo", "demo@uco.edu.co", "3001234567", Rol.DOCENTE));

        assertThatThrownBy(() -> casoUso.crearFaltantes(usuarios))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CELULAR_INVALIDO");
    }

    @Test
    void noPermiteCrearAdministradoresNiCorreosInvalidos() {
        assertThatThrownBy(() -> casoUso.crearFaltantes(
                List.of(comando("Demo", "demo@uco.edu.co", "+573001234567", Rol.ADMINISTRADOR))))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("ROL_NO_PERMITIDO");
        assertThatThrownBy(() -> casoUso.crearFaltantes(
                List.of(comando("Demo", "no-es-correo", "+573001234567", Rol.DOCENTE))))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CORREO_INVALIDO");
    }

    private static UsuarioDemostracionComando comando(String nombre, String correo, String celular, Rol rol) {
        return new UsuarioDemostracionComando(nombre, correo, celular, rol, "Clave-Segura-2026");
    }
}
