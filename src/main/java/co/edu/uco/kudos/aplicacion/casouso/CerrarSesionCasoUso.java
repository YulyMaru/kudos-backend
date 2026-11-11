package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.entrada.CerrarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class CerrarSesionCasoUso implements CerrarSesionPuerto {

    private final RepositorioTokensRefresco tokens;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public CerrarSesionCasoUso(RepositorioTokensRefresco tokens, RegistroAuditoria auditoria,
                               GestorTransacciones transacciones, Clock reloj) {
        this.tokens = Objects.requireNonNull(tokens);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void cerrar(String tokenRefresco) {
        if (tokenRefresco == null || tokenRefresco.isBlank()) {
            return;
        }
        tokens.buscarPorHash(TokensSeguros.sha256(tokenRefresco.trim()))
                .filter(token -> !token.estaRevocado())
                .ifPresent(this::revocar);
    }

    private void revocar(TokenRefresco token) {
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            if (tokens.revocarSiVigente(token.id(), ahora, null)) {
                auditoria.registrar(new EntradaAuditoria(UUID.randomUUID(), ahora, token.usuarioId(),
                        token.usuarioId(), null, AccionAuditoria.SESION_CERRADA, NotificacionesCuenta.RECURSO_USUARIO,
                        token.usuarioId(), null, ResultadoAuditoria.EXITOSO, null));
            }
        });
    }
}
