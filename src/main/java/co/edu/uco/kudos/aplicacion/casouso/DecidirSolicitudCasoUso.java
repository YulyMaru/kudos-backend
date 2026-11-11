package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.DecidirSolicitudPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.ReglaAprobacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DecidirSolicitudCasoUso implements DecidirSolicitudPuerto {

    private static final int LIMITE_PENDIENTES = 100;

    private final RepositorioSolicitudes solicitudes;
    private final RepositorioUsuarios usuarios;
    private final RepositorioEstructura estructura;
    private final RepositorioOutbox outbox;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final ActivadorCuentas activador;
    private final Clock reloj;

    public DecidirSolicitudCasoUso(RepositorioSolicitudes solicitudes, RepositorioUsuarios usuarios,
                                   RepositorioCuentas cuentas, RepositorioEstructura estructura,
                                   RepositorioVinculos vinculos, RepositorioOutbox outbox, RegistroAuditoria auditoria,
                                   GestorTransacciones transacciones, Clock reloj) {
        this.solicitudes = Objects.requireNonNull(solicitudes);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.estructura = Objects.requireNonNull(estructura);
        this.outbox = Objects.requireNonNull(outbox);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.activador = new ActivadorCuentas(cuentas, vinculos, outbox, auditoria);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public List<SolicitudPendienteVista> pendientes(UUID aprobadorId) {
        Usuario aprobador = Actores.activo(usuarios, aprobadorId);
        List<Rol> roles = ReglaAprobacion.rolesQueAprueba(aprobador.getRol());
        if (roles.isEmpty()) {
            return List.of();
        }
        UUID programa = aprobador.getRol() == Rol.COORDINADOR_PROGRAMA ? aprobador.getProgramaId() : null;
        UUID facultad = aprobador.getRol() == Rol.COORDINADOR_FACULTAD ? aprobador.getFacultadId() : null;
        return solicitudes.pendientes(roles, programa, facultad, LIMITE_PENDIENTES);
    }

    @Override
    public void aprobar(UUID aprobadorId, UUID solicitudId) {
        Usuario aprobador = Actores.activo(usuarios, aprobadorId);
        SolicitudRegistro solicitud = cargar(solicitudId);
        ReglaAprobacion.validarPuedeAprobar(aprobador, solicitud, facultadDelPrograma(solicitud));
        Instant ahora = Instant.now(reloj);
        SolicitudRegistro aprobada = solicitud.aprobar(aprobador.getId(), ahora);
        Usuario solicitante = solicitante(solicitud);
        transacciones.enTransaccion(() -> {
            solicitudes.actualizar(aprobada);
            auditoria.registrar(entrada(aprobador, solicitante, ahora, AccionAuditoria.SOLICITUD_APROBADA, null));
            activador.activarSiCorresponde(solicitante, aprobada, ahora);
        });
    }

    @Override
    public void rechazar(UUID aprobadorId, UUID solicitudId, String motivo) {
        Usuario aprobador = Actores.activo(usuarios, aprobadorId);
        SolicitudRegistro solicitud = cargar(solicitudId);
        ReglaAprobacion.validarPuedeAprobar(aprobador, solicitud, facultadDelPrograma(solicitud));
        Instant ahora = Instant.now(reloj);
        SolicitudRegistro rechazada = solicitud.rechazar(aprobador.getId(), motivo, ahora);
        Usuario solicitante = solicitante(solicitud);
        transacciones.enTransaccion(() -> {
            solicitudes.actualizar(rechazada);
            outbox.guardarTodas(List.of(NotificacionesCuenta.solicitudRechazada(solicitante,
                    rechazada.motivoRechazo(), ahora)));
            auditoria.registrar(entrada(aprobador, solicitante, ahora, AccionAuditoria.SOLICITUD_RECHAZADA,
                    "motivo registrado"));
        });
    }

    private SolicitudRegistro cargar(UUID solicitudId) {
        return solicitudes.buscarPorId(Objects.requireNonNull(solicitudId, "solicitudId"))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("solicitud", solicitudId.toString()));
    }

    private Usuario solicitante(SolicitudRegistro solicitud) {
        return usuarios.buscarPorId(solicitud.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("usuario", solicitud.usuarioId().toString()));
    }

    private UUID facultadDelPrograma(SolicitudRegistro solicitud) {
        if (solicitud.programaId() == null) {
            return null;
        }
        return estructura.programa(solicitud.programaId()).map(Programa::facultadId).orElse(null);
    }

    private static EntradaAuditoria entrada(Usuario aprobador, Usuario solicitante, Instant ahora,
                                            AccionAuditoria accion, String detalle) {
        return EntradaAuditoria.de(ahora, solicitante.getId(), aprobador, accion, NotificacionesCuenta.RECURSO_USUARIO,
                solicitante.getId(), null, ResultadoAuditoria.EXITOSO, detalle);
    }
}
