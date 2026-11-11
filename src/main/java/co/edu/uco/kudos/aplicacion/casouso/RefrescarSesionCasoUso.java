package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.SesionResultado;
import co.edu.uco.kudos.aplicacion.dto.TokenAcceso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RefrescarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefrescarSesionCasoUso implements RefrescarSesionPuerto {

    private static final String RECURSO_USUARIO = "USUARIO";

    private final RepositorioTokensRefresco tokens;
    private final RepositorioUsuarios usuarios;
    private final EmisorTokens emisor;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;
    private final Duration vigenciaRefresco;

    public RefrescarSesionCasoUso(RepositorioTokensRefresco tokens, RepositorioUsuarios usuarios,
                                  EmisorTokens emisor, RegistroAuditoria auditoria,
                                  GestorTransacciones transacciones, Clock reloj, Duration vigenciaRefresco) {
        this.tokens = Objects.requireNonNull(tokens);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.emisor = Objects.requireNonNull(emisor);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
        this.vigenciaRefresco = Objects.requireNonNull(vigenciaRefresco);
    }

    @Override
    public SesionResultado refrescar(String tokenRefresco) {
        if (tokenRefresco == null || tokenRefresco.isBlank()) {
            throw new CredencialesInvalidasExcepcion();
        }
        Instant ahora = Instant.now(reloj);
        TokenRefresco actual = tokens.buscarPorHash(TokensSeguros.sha256(tokenRefresco))
                .orElseThrow(CredencialesInvalidasExcepcion::new);

        if (actual.estaRevocado()) {
            throw reusoDetectado(actual, ahora);
        }
        if (actual.estaExpirado(ahora)) {
            throw new CredencialesInvalidasExcepcion();
        }
        Usuario usuario = usuarios.buscarPorId(actual.usuarioId())
                .filter(encontrado -> encontrado.estaActivo() && !encontrado.estaBloqueado(ahora))
                .orElse(null);
        if (usuario == null) {
            transacciones.enTransaccion(() -> tokens.revocarTodosDeUsuario(actual.usuarioId(), ahora));
            throw new CredencialesInvalidasExcepcion();
        }

        String nuevoPlano = TokensSeguros.generar();
        TokenRefresco nuevo = TokenRefresco.emitir(usuario.getId(), TokensSeguros.sha256(nuevoPlano), ahora,
                vigenciaRefresco);
        try {
            transacciones.enTransaccion(() -> {
                if (!tokens.revocarSiVigente(actual.id(), ahora, nuevo.id())) {
                    throw new RotacionConcurrente();
                }
                tokens.guardar(nuevo);
                auditoria.registrar(entrada(ahora, usuario.getId(), usuario, AccionAuditoria.SESION_REFRESCADA,
                        ResultadoAuditoria.EXITOSO, null));
            });
        } catch (RotacionConcurrente error) {
            throw reusoDetectado(actual, ahora);
        }
        TokenAcceso acceso = emisor.emitir(usuario, ahora);
        return new SesionResultado(acceso.valor(), acceso.expiraEn(), nuevoPlano, nuevo.expiraEn(),
                usuario.getId(), usuario.getNombreCompleto(), usuario.getRol());
    }

    private CredencialesInvalidasExcepcion reusoDetectado(TokenRefresco token, Instant ahora) {
        transacciones.enTransaccion(() -> {
            tokens.revocarTodosDeUsuario(token.usuarioId(), ahora);
            auditoria.registrar(entrada(ahora, token.usuarioId(), null, AccionAuditoria.REUSO_TOKEN_REFRESCO,
                    ResultadoAuditoria.FALLIDO, "se revocaron todas las sesiones del usuario"));
        });
        return new CredencialesInvalidasExcepcion();
    }

    private static EntradaAuditoria entrada(Instant ahora, UUID usuarioId, Usuario usuario, AccionAuditoria accion,
                                            ResultadoAuditoria resultado, String detalle) {
        return new EntradaAuditoria(UUID.randomUUID(), ahora, usuarioId, usuarioId,
                usuario == null ? null : usuario.getRol(), accion, RECURSO_USUARIO, usuarioId, null, resultado,
                detalle);
    }

    private static final class RotacionConcurrente extends RuntimeException {
        private RotacionConcurrente() {
            super("El token de refresco ya fue rotado por otra petición", null, false, false);
        }
    }
}
