package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.ReglaAprobacion;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class ActivadorCuentas {

    private final RepositorioCuentas cuentas;
    private final RepositorioVinculos vinculos;
    private final RepositorioOutbox outbox;
    private final RegistroAuditoria auditoria;

    ActivadorCuentas(RepositorioCuentas cuentas, RepositorioVinculos vinculos, RepositorioOutbox outbox,
                     RegistroAuditoria auditoria) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.vinculos = Objects.requireNonNull(vinculos);
        this.outbox = Objects.requireNonNull(outbox);
        this.auditoria = Objects.requireNonNull(auditoria);
    }

    Usuario activarSiCorresponde(Usuario usuario, SolicitudRegistro solicitud, Instant ahora) {
        if (usuario.estaActivo() || solicitud == null || !solicitud.estaAprobada() || !usuario.contactosVerificados()) {
            return usuario;
        }
        if (ReglaAprobacion.esRolUnico(usuario.getRol())) {
            cuentas.buscarActivoConRol(usuario.getRol(), usuario.getFacultadId(), usuario.getProgramaId())
                    .filter(anterior -> !anterior.esElMismoQue(usuario))
                    .ifPresent(anterior -> {
                        cuentas.guardar(anterior.inactivar());
                        auditoria.registrar(entrada(anterior, ahora, AccionAuditoria.CUENTA_REEMPLAZADA,
                                "reemplazado por " + usuario.getId()));
                    });
        }
        Usuario activo = usuario.activar();
        cuentas.guardar(activo);
        if ((activo.getRol() == Rol.ESTUDIANTE || activo.getRol() == Rol.DOCENTE) && solicitud.programaId() != null) {
            vinculos.vincular(activo.getId(), solicitud.programaId());
        }
        outbox.guardarTodas(List.of(NotificacionesCuenta.cuentaActivada(activo, ahora)));
        auditoria.registrar(entrada(activo, ahora, AccionAuditoria.CUENTA_ACTIVADA, null));
        return activo;
    }

    private static EntradaAuditoria entrada(Usuario usuario, Instant ahora, AccionAuditoria accion, String detalle) {
        return EntradaAuditoria.de(ahora, usuario.getId(), usuario, accion, NotificacionesCuenta.RECURSO_USUARIO,
                usuario.getId(), null, ResultadoAuditoria.EXITOSO, detalle);
    }
}
