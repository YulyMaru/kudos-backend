package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.entrada.VerificarContactoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCodigos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class VerificarContactoCasoUso implements VerificarContactoPuerto {

    public static final int MAXIMO_REENVIOS = 3;
    public static final Duration VENTANA_REENVIOS = Duration.ofMinutes(15);

    private final RepositorioCuentas cuentas;
    private final RepositorioCodigos codigos;
    private final RepositorioSolicitudes solicitudes;
    private final RepositorioOutbox outbox;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final ActivadorCuentas activador;
    private final Clock reloj;
    private final Duration vigenciaCodigos;

    public VerificarContactoCasoUso(RepositorioCuentas cuentas, RepositorioCodigos codigos,
                                    RepositorioSolicitudes solicitudes, RepositorioVinculos vinculos,
                                    RepositorioOutbox outbox, RegistroAuditoria auditoria,
                                    GestorTransacciones transacciones, Clock reloj, Duration vigenciaCodigos) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.codigos = Objects.requireNonNull(codigos);
        this.solicitudes = Objects.requireNonNull(solicitudes);
        this.outbox = Objects.requireNonNull(outbox);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.activador = new ActivadorCuentas(cuentas, vinculos, outbox, auditoria);
        this.reloj = Objects.requireNonNull(reloj);
        this.vigenciaCodigos = Objects.requireNonNull(vigenciaCodigos);
    }

    @Override
    public void verificar(String correo, PropositoCodigo proposito, String codigo) {
        validarProposito(proposito);
        if (codigo == null || codigo.isBlank()) {
            throw new CampoObligatorioExcepcion("codigo");
        }
        Usuario usuario = buscar(correo).orElseThrow(VerificarContactoCasoUso::codigoInvalido);
        if (yaVerificado(usuario, proposito)) {
            return;
        }
        Instant ahora = Instant.now(reloj);
        CodigoVerificacion vigente = codigos.ultimo(usuario.getId(), proposito)
                .filter(candidato -> candidato.estaVigente(ahora))
                .orElseThrow(() -> new ReglaNegocioExcepcion("CODIGO_EXPIRADO", "codigo",
                        "El código expiró o ya no es válido; solicita uno nuevo"));

        if (!vigente.hashCodigo().equals(TokensSeguros.hashCodigo(usuario.getId(), codigo.trim()))) {
            CodigoVerificacion fallido = vigente.conIntentoFallido();
            transacciones.enTransaccion(() -> {
                codigos.actualizar(fallido);
                auditoria.registrar(entrada(usuario, ahora, AccionAuditoria.VERIFICACION_FALLIDA,
                        ResultadoAuditoria.FALLIDO, proposito + " intento=" + fallido.intentos()));
            });
            if (!fallido.estaVigente(ahora)) {
                throw new ReglaNegocioExcepcion("VERIFICACION_BLOQUEADA", "codigo",
                        "Superaste los intentos permitidos; solicita un nuevo código");
            }
            throw codigoInvalido();
        }

        Usuario verificado = proposito == PropositoCodigo.VERIFICAR_CORREO
                ? usuario.conCorreoVerificado() : usuario.conCelularVerificado();
        AccionAuditoria accion = proposito == PropositoCodigo.VERIFICAR_CORREO
                ? AccionAuditoria.CORREO_VERIFICADO : AccionAuditoria.CELULAR_VERIFICADO;
        transacciones.enTransaccion(() -> {
            codigos.actualizar(vigente.usado(ahora));
            cuentas.guardar(verificado);
            auditoria.registrar(entrada(verificado, ahora, accion, ResultadoAuditoria.EXITOSO, null));
            activador.activarSiCorresponde(verificado, solicitudes.buscarPorUsuario(verificado.getId()).orElse(null),
                    ahora);
        });
    }

    @Override
    public void reenviar(String correo, PropositoCodigo proposito) {
        validarProposito(proposito);
        Optional<Usuario> encontrado = buscar(correo);
        if (encontrado.isEmpty() || yaVerificado(encontrado.get(), proposito)) {
            return;
        }
        Usuario usuario = encontrado.get();
        Instant ahora = Instant.now(reloj);
        if (codigos.contarEmitidosDesde(usuario.getId(), proposito, ahora.minus(VENTANA_REENVIOS)) >= MAXIMO_REENVIOS) {
            throw new ReglaNegocioExcepcion("REENVIOS_EXCEDIDOS", "codigo",
                    "Alcanzaste el límite de reenvíos; intenta de nuevo en unos minutos");
        }
        String codigo = TokensSeguros.generarCodigoNumerico();
        CodigoVerificacion nuevo = CodigoVerificacion.emitir(usuario.getId(), proposito,
                TokensSeguros.hashCodigo(usuario.getId(), codigo), ahora, vigenciaCodigos);
        transacciones.enTransaccion(() -> {
            codigos.invalidarVigentes(usuario.getId(), proposito, ahora);
            codigos.guardar(nuevo);
            outbox.guardarTodas(List.of(NotificacionesCuenta.codigo(usuario, proposito, codigo, ahora)));
        });
    }

    private Optional<Usuario> buscar(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new CampoObligatorioExcepcion("correo");
        }
        return cuentas.buscarPorCorreo(correo.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean yaVerificado(Usuario usuario, PropositoCodigo proposito) {
        return proposito == PropositoCodigo.VERIFICAR_CORREO ? usuario.isCorreoVerificado() : usuario.isCelularVerificado();
    }

    private static void validarProposito(PropositoCodigo proposito) {
        if (proposito != PropositoCodigo.VERIFICAR_CORREO && proposito != PropositoCodigo.VERIFICAR_CELULAR) {
            throw new CampoObligatorioExcepcion("canal");
        }
    }

    private static ReglaNegocioExcepcion codigoInvalido() {
        return new ReglaNegocioExcepcion("CODIGO_INVALIDO", "codigo", "El código no es correcto");
    }

    private static EntradaAuditoria entrada(Usuario usuario, Instant ahora, AccionAuditoria accion,
                                            ResultadoAuditoria resultado, String detalle) {
        return EntradaAuditoria.de(ahora, usuario.getId(), usuario, accion, NotificacionesCuenta.RECURSO_USUARIO,
                usuario.getId(), null, resultado, detalle);
    }
}
