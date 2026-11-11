package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.RegistrarUsuarioComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RegistrarUsuarioPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCodigos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.PoliticaContrasena;
import co.edu.uco.kudos.dominio.regla.ReglaRegistro;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RegistrarUsuarioCasoUso implements RegistrarUsuarioPuerto {

    private final RepositorioCuentas cuentas;
    private final RepositorioSolicitudes solicitudes;
    private final RepositorioCodigos codigos;
    private final RepositorioEstructura estructura;
    private final RepositorioOutbox outbox;
    private final CodificadorContrasenas codificador;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;
    private final List<String> dominiosInstitucionales;
    private final Duration vigenciaCodigos;

    public RegistrarUsuarioCasoUso(RepositorioCuentas cuentas, RepositorioSolicitudes solicitudes,
                                   RepositorioCodigos codigos, RepositorioEstructura estructura,
                                   RepositorioOutbox outbox, CodificadorContrasenas codificador,
                                   RegistroAuditoria auditoria, GestorTransacciones transacciones, Clock reloj,
                                   List<String> dominiosInstitucionales, Duration vigenciaCodigos) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.solicitudes = Objects.requireNonNull(solicitudes);
        this.codigos = Objects.requireNonNull(codigos);
        this.estructura = Objects.requireNonNull(estructura);
        this.outbox = Objects.requireNonNull(outbox);
        this.codificador = Objects.requireNonNull(codificador);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
        this.dominiosInstitucionales = List.copyOf(dominiosInstitucionales);
        this.vigenciaCodigos = Objects.requireNonNull(vigenciaCodigos);
    }

    @Override
    public void registrar(RegistrarUsuarioComando comando) {
        Objects.requireNonNull(comando, "comando");
        if (!comando.aceptaPolitica()) {
            throw new ReglaNegocioExcepcion("POLITICA_NO_ACEPTADA", "aceptaPolitica",
                    "Debe aceptar la política de tratamiento de datos personales para registrarse");
        }
        if (comando.nombreCompleto() == null || comando.nombreCompleto().isBlank()) {
            throw new CampoObligatorioExcepcion("nombreCompleto");
        }
        String correo = ReglaRegistro.normalizarCorreoInstitucional(comando.correo(), dominiosInstitucionales);
        String celular = ReglaRegistro.normalizarCelular(comando.celular());
        ReglaRegistro.validarAlcance(comando.rol(), comando.facultadId(), comando.programaId());
        PoliticaContrasena.validar(comando.contrasena());
        UUID facultadId = validarFacultad(comando.rol(), comando.facultadId());
        UUID programaId = validarPrograma(comando.rol(), comando.programaId());
        if (cuentas.existePorCorreoOCelular(correo, celular)) {
            throw new ReglaNegocioExcepcion("CUENTA_EXISTENTE", "correo",
                    "Ya existe una cuenta asociada a ese correo o celular");
        }

        Instant ahora = Instant.now(reloj);
        Usuario usuario = Usuario.registrar(comando.nombreCompleto().trim(), correo, celular, comando.rol(),
                codificador.codificar(comando.contrasena()), facultadId, programaId);
        SolicitudRegistro solicitud = SolicitudRegistro.crear(usuario, facultadId, programaId, ahora);
        List<CodigoVerificacion> nuevosCodigos = new ArrayList<>();
        List<Notificacion> notificaciones = new ArrayList<>();
        for (PropositoCodigo proposito : List.of(PropositoCodigo.VERIFICAR_CORREO, PropositoCodigo.VERIFICAR_CELULAR)) {
            String codigo = TokensSeguros.generarCodigoNumerico();
            nuevosCodigos.add(CodigoVerificacion.emitir(usuario.getId(), proposito,
                    TokensSeguros.hashCodigo(usuario.getId(), codigo), ahora, vigenciaCodigos));
            notificaciones.add(NotificacionesCuenta.codigo(usuario, proposito, codigo, ahora));
        }

        transacciones.enTransaccion(() -> {
            cuentas.guardar(usuario);
            solicitudes.guardar(solicitud);
            nuevosCodigos.forEach(codigos::guardar);
            outbox.guardarTodas(notificaciones);
            auditoria.registrar(EntradaAuditoria.de(ahora, usuario.getId(), usuario,
                    AccionAuditoria.REGISTRO_SOLICITADO, NotificacionesCuenta.RECURSO_USUARIO, usuario.getId(), null,
                    ResultadoAuditoria.EXITOSO, "rolSolicitado=" + usuario.getRol()));
        });
    }

    private UUID validarFacultad(Rol rol, UUID facultadId) {
        if (rol != Rol.COORDINADOR_FACULTAD) {
            return null;
        }
        return estructura.facultad(facultadId)
                .filter(Facultad::activa)
                .map(Facultad::id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("facultad", String.valueOf(facultadId)));
    }

    private UUID validarPrograma(Rol rol, UUID programaId) {
        if (rol != Rol.ESTUDIANTE && rol != Rol.DOCENTE && rol != Rol.COORDINADOR_PROGRAMA) {
            return null;
        }
        return estructura.programa(programaId)
                .filter(Programa::activo)
                .map(Programa::id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("programa", String.valueOf(programaId)));
    }
}
