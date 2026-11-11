package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrearAdministradorInicialCasoUsoTest {

    private DoblesAutenticacion.CuentasEnMemoria cuentas;
    private List<EntradaAuditoria> auditoria;
    private CrearAdministradorInicialCasoUso casoUso;

    @BeforeEach
    void preparar() {
        cuentas = new DoblesAutenticacion.CuentasEnMemoria();
        auditoria = new ArrayList<>();
        GestorTransacciones transacciones = Runnable::run;
        casoUso = new CrearAdministradorInicialCasoUso(cuentas, new DoblesAutenticacion.CodificadorFalso(),
                auditoria::add, transacciones,
                new DoblesAutenticacion.RelojAjustable(Instant.parse("2026-11-11T10:00:00Z")));
    }

    @Test
    void creaElAdministradorActivoConLaContrasenaCodificada() {
        boolean creado = casoUso.crearSiNoExiste("Administrador", " Admin@UCO.edu.co ", "Clave-Segura-2026");

        assertThat(creado).isTrue();
        Usuario administrador = cuentas.usuarios.values().iterator().next();
        assertThat(administrador.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(administrador.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(administrador.getCorreo()).isEqualTo("admin@uco.edu.co");
        assertThat(administrador.getHashContrasena()).isEqualTo("hash:Clave-Segura-2026");
        assertThat(auditoria).singleElement()
                .satisfies(e -> assertThat(e.accion()).isEqualTo(AccionAuditoria.ADMINISTRADOR_INICIAL_CREADO));
    }

    @Test
    void noHaceNadaSiYaExisteUnAdministrador() {
        cuentas.guardar(new Usuario(UUID.randomUUID(), "Otro", "otro@uco.edu.co", null, false, Rol.ADMINISTRADOR,
                EstadoUsuario.ACTIVO));

        boolean creado = casoUso.crearSiNoExiste("Administrador", "admin@uco.edu.co", "Clave-Segura-2026");

        assertThat(creado).isFalse();
        assertThat(cuentas.usuarios).hasSize(1);
        assertThat(auditoria).isEmpty();
    }

    @Test
    void rechazaContrasenasDebiles() {
        assertThatThrownBy(() -> casoUso.crearSiNoExiste("Administrador", "admin@uco.edu.co", "123456"))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CONTRASENA_DEBIL");
        assertThat(cuentas.usuarios).isEmpty();
    }

    @Test
    void rechazaUnCorreoQueYaUsaOtraCuenta() {
        cuentas.guardar(new Usuario(UUID.randomUUID(), "Docente", "admin@uco.edu.co", null, false, Rol.DOCENTE,
                EstadoUsuario.ACTIVO));

        assertThatThrownBy(() -> casoUso.crearSiNoExiste("Administrador", "admin@uco.edu.co", "Clave-Segura-2026"))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CORREO_EN_USO");
    }
}
