package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.entrada.CrearAdministradorInicialPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.PoliticaContrasena;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class CrearAdministradorInicialCasoUso implements CrearAdministradorInicialPuerto {

    private final RepositorioCuentas cuentas;
    private final CodificadorContrasenas codificador;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public CrearAdministradorInicialCasoUso(RepositorioCuentas cuentas, CodificadorContrasenas codificador,
                                            RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                            Clock reloj) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.codificador = Objects.requireNonNull(codificador);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public boolean crearSiNoExiste(String nombreCompleto, String correo, String contrasena) {
        if (cuentas.existeConRol(Rol.ADMINISTRADOR)) {
            return false;
        }
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new CampoObligatorioExcepcion("nombreCompleto");
        }
        if (correo == null || correo.isBlank()) {
            throw new CampoObligatorioExcepcion("correo");
        }
        PoliticaContrasena.validar(contrasena);
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (cuentas.buscarPorCorreo(correoNormalizado).isPresent()) {
            throw new ReglaNegocioExcepcion("CORREO_EN_USO", "correo",
                    "Ya existe una cuenta con el correo del administrador inicial");
        }
        Usuario administrador = new Usuario(UUID.randomUUID(), nombreCompleto.trim(), correoNormalizado, null, false,
                Rol.ADMINISTRADOR, EstadoUsuario.ACTIVO, codificador.codificar(contrasena), 0, null);
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            cuentas.guardar(administrador);
            auditoria.registrar(EntradaAuditoria.de(ahora, administrador.getId(), administrador,
                    AccionAuditoria.ADMINISTRADOR_INICIAL_CREADO, "USUARIO", administrador.getId(), null,
                    ResultadoAuditoria.EXITOSO, null));
        });
        return true;
    }
}
