package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.entrada.RestablecerContrasenaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCodigos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.PoliticaContrasena;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class RestablecerContrasenaCasoUso implements RestablecerContrasenaPuerto {

    public static final int MAXIMO_SOLICITUDES = 3;
    public static final Duration VENTANA_SOLICITUDES = Duration.ofMinutes(15);

    private final RepositorioCuentas cuentas;
    private final RepositorioUsuarios usuarios;
    private final RepositorioCodigos codigos;
    private final RepositorioTokensRefresco tokens;
    private final RepositorioOutbox outbox;
    private final CodificadorContrasenas codificador;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;
    private final Duration vigenciaEnlace;

    public RestablecerContrasenaCasoUso(RepositorioCuentas cuentas, RepositorioUsuarios usuarios,
                                        RepositorioCodigos codigos, RepositorioTokensRefresco tokens,
                                        RepositorioOutbox outbox, CodificadorContrasenas codificador,
                                        RegistroAuditoria auditoria, GestorTransacciones transacciones, Clock reloj,
                                        Duration vigenciaEnlace) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.codigos = Objects.requireNonNull(codigos);
        this.tokens = Objects.requireNonNull(tokens);
        this.outbox = Objects.requireNonNull(outbox);
        this.codificador = Objects.requireNonNull(codificador);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
        this.vigenciaEnlace = Objects.requireNonNull(vigenciaEnlace);
    }

    @Override
    public void solicitar(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new CampoObligatorioExcepcion("correo");
        }
        Optional<Usuario> encontrado = cuentas.buscarPorCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::estaActivo);
        if (encontrado.isEmpty()) {
            return;
        }
        Usuario usuario = encontrado.get();
        Instant ahora = Instant.now(reloj);
        if (codigos.contarEmitidosDesde(usuario.getId(), PropositoCodigo.RESTABLECER_CONTRASENA,
                ahora.minus(VENTANA_SOLICITUDES)) >= MAXIMO_SOLICITUDES) {
            return;
        }
        String token = TokensSeguros.generar();
        CodigoVerificacion codigo = CodigoVerificacion.emitir(usuario.getId(), PropositoCodigo.RESTABLECER_CONTRASENA,
                TokensSeguros.sha256(token), ahora, vigenciaEnlace);
        transacciones.enTransaccion(() -> {
            codigos.invalidarVigentes(usuario.getId(), PropositoCodigo.RESTABLECER_CONTRASENA, ahora);
            codigos.guardar(codigo);
            outbox.guardarTodas(List.of(NotificacionesCuenta.restablecimiento(usuario, token, ahora)));
            auditoria.registrar(entrada(usuario, ahora, AccionAuditoria.RESTABLECIMIENTO_SOLICITADO));
        });
    }

    @Override
    public void confirmar(String token, String nuevaContrasena) {
        if (token == null || token.isBlank()) {
            throw enlaceInvalido();
        }
        PoliticaContrasena.validar(nuevaContrasena);
        Instant ahora = Instant.now(reloj);
        CodigoVerificacion codigo = codigos.buscarPorHash(PropositoCodigo.RESTABLECER_CONTRASENA,
                        TokensSeguros.sha256(token.trim()))
                .filter(candidato -> candidato.estaVigente(ahora))
                .orElseThrow(RestablecerContrasenaCasoUso::enlaceInvalido);
        Usuario usuario = usuarios.buscarPorId(codigo.usuarioId())
                .filter(Usuario::estaActivo)
                .orElseThrow(RestablecerContrasenaCasoUso::enlaceInvalido);
        Usuario actualizado = usuario.conContrasena(codificador.codificar(nuevaContrasena));
        transacciones.enTransaccion(() -> {
            cuentas.guardar(actualizado);
            codigos.actualizar(codigo.usado(ahora));
            tokens.revocarTodosDeUsuario(usuario.getId(), ahora);
            auditoria.registrar(entrada(usuario, ahora, AccionAuditoria.CONTRASENA_RESTABLECIDA));
        });
    }

    private static ReglaNegocioExcepcion enlaceInvalido() {
        return new ReglaNegocioExcepcion("ENLACE_INVALIDO", "token",
                "El enlace de restablecimiento no es válido o ya expiró");
    }

    private static EntradaAuditoria entrada(Usuario usuario, Instant ahora, AccionAuditoria accion) {
        return EntradaAuditoria.de(ahora, usuario.getId(), usuario, accion, NotificacionesCuenta.RECURSO_USUARIO,
                usuario.getId(), null, ResultadoAuditoria.EXITOSO, null);
    }
}
