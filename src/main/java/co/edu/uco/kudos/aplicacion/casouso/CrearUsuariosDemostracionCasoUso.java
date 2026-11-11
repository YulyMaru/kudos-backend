package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.UsuarioDemostracionComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CrearUsuariosDemostracionPuerto;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CrearUsuariosDemostracionCasoUso implements CrearUsuariosDemostracionPuerto {

    private static final Pattern CELULAR_E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RepositorioCuentas cuentas;
    private final CodificadorContrasenas codificador;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public CrearUsuariosDemostracionCasoUso(RepositorioCuentas cuentas, CodificadorContrasenas codificador,
                                            RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                            Clock reloj) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.codificador = Objects.requireNonNull(codificador);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public int crearFaltantes(List<UsuarioDemostracionComando> usuarios) {
        int creados = 0;
        for (UsuarioDemostracionComando comando : usuarios) {
            validar(comando);
            String correo = comando.correo().trim().toLowerCase(Locale.ROOT);
            if (cuentas.buscarPorCorreo(correo).isPresent()) {
                continue;
            }
            Usuario usuario = new Usuario(UUID.randomUUID(), comando.nombreCompleto().trim(), correo,
                    comando.celular().trim(), true, comando.rol(), EstadoUsuario.ACTIVO,
                    codificador.codificar(comando.contrasena()), 0, null);
            Instant ahora = Instant.now(reloj);
            transacciones.enTransaccion(() -> {
                cuentas.guardar(usuario);
                auditoria.registrar(EntradaAuditoria.de(ahora, usuario.getId(), usuario,
                        AccionAuditoria.USUARIO_DEMOSTRACION_CREADO, "USUARIO", usuario.getId(), null,
                        ResultadoAuditoria.EXITOSO, null));
            });
            creados++;
        }
        return creados;
    }

    private static void validar(UsuarioDemostracionComando comando) {
        if (comando.nombreCompleto() == null || comando.nombreCompleto().isBlank()) {
            throw new CampoObligatorioExcepcion("nombreCompleto");
        }
        if (comando.correo() == null || !CORREO.matcher(comando.correo().trim()).matches()) {
            throw new ReglaNegocioExcepcion("CORREO_INVALIDO", "correo",
                    "El correo del usuario de demostración no es válido");
        }
        if (comando.celular() == null || !CELULAR_E164.matcher(comando.celular().trim()).matches()) {
            throw new ReglaNegocioExcepcion("CELULAR_INVALIDO", "celular",
                    "El celular debe estar en formato internacional, por ejemplo +573001234567");
        }
        if (comando.rol() == null || comando.rol() == Rol.ADMINISTRADOR) {
            throw new ReglaNegocioExcepcion("ROL_NO_PERMITIDO", "rol",
                    "Los usuarios de demostración no pueden ser administradores");
        }
        PoliticaContrasena.validar(comando.contrasena());
    }
}
