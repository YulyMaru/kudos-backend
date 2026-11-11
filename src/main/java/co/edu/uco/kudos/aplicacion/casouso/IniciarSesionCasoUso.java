package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.IniciarSesionComando;
import co.edu.uco.kudos.aplicacion.dto.SesionResultado;
import co.edu.uco.kudos.aplicacion.dto.TokenAcceso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.IniciarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.dominio.modelo.EstadoAprobacion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import java.util.ArrayList;
import java.util.List;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaBloqueadaExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaNoActivaExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.PoliticaBloqueo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class IniciarSesionCasoUso implements IniciarSesionPuerto {

    private static final String RECURSO_USUARIO = "USUARIO";

    private final RepositorioCuentas cuentas;
    private final RepositorioSolicitudes solicitudes;
    private final RepositorioTokensRefresco tokens;
    private final CodificadorContrasenas codificador;
    private final EmisorTokens emisor;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;
    private final PoliticaBloqueo politica;
    private final Duration vigenciaRefresco;
    private final String hashFicticio;

    public IniciarSesionCasoUso(RepositorioCuentas cuentas, RepositorioSolicitudes solicitudes,
                                RepositorioTokensRefresco tokens, CodificadorContrasenas codificador,
                                EmisorTokens emisor, RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                Clock reloj, PoliticaBloqueo politica, Duration vigenciaRefresco) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.solicitudes = Objects.requireNonNull(solicitudes);
        this.tokens = Objects.requireNonNull(tokens);
        this.codificador = Objects.requireNonNull(codificador);
        this.emisor = Objects.requireNonNull(emisor);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
        this.politica = Objects.requireNonNull(politica);
        this.vigenciaRefresco = Objects.requireNonNull(vigenciaRefresco);
        this.hashFicticio = codificador.codificar(UUID.randomUUID().toString());
    }

    @Override
    public SesionResultado iniciarSesion(IniciarSesionComando comando) {
        Objects.requireNonNull(comando, "comando");
        if (comando.correo() == null || comando.correo().isBlank()) {
            throw new CampoObligatorioExcepcion("correo");
        }
        if (comando.contrasena() == null || comando.contrasena().isEmpty()) {
            throw new CampoObligatorioExcepcion("contrasena");
        }
        Instant ahora = Instant.now(reloj);
        Optional<Usuario> encontrado = cuentas.buscarPorCorreo(comando.correo().trim());

        if (encontrado.isEmpty() || !encontrado.get().tieneContrasena()) {
            codificador.coincide(comando.contrasena(), hashFicticio);
            auditoria.registrar(entrada(ahora, null, AccionAuditoria.INICIO_SESION_FALLIDO,
                    ResultadoAuditoria.FALLIDO, "credenciales invalidas"));
            throw new CredencialesInvalidasExcepcion();
        }

        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueado(ahora)) {
            auditoria.registrar(entrada(ahora, usuario, AccionAuditoria.INICIO_SESION_FALLIDO,
                    ResultadoAuditoria.FALLIDO, "cuenta bloqueada temporalmente"));
            throw new CuentaBloqueadaExcepcion(usuario.getBloqueadoHasta());
        }

        if (!codificador.coincide(comando.contrasena(), usuario.getHashContrasena())) {
            return rechazarContrasena(usuario, ahora);
        }

        if (!usuario.estaActivo()) {
            auditoria.registrar(entrada(ahora, usuario, AccionAuditoria.INICIO_SESION_FALLIDO,
                    ResultadoAuditoria.FALLIDO, "cuenta no activa: " + usuario.getEstado()));
            throw new CuentaNoActivaExcepcion(usuario.getEstado(), condicionesPendientes(usuario));
        }

        return abrirSesion(usuario, ahora);
    }

    private List<String> condicionesPendientes(Usuario usuario) {
        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            return List.of();
        }
        List<String> pendientes = new ArrayList<>();
        if (!usuario.isCorreoVerificado()) {
            pendientes.add("VERIFICAR_CORREO");
        }
        if (!usuario.isCelularVerificado()) {
            pendientes.add("VERIFICAR_CELULAR");
        }
        solicitudes.buscarPorUsuario(usuario.getId()).ifPresent(solicitud -> {
            if (solicitud.estado() == EstadoAprobacion.PENDIENTE) {
                pendientes.add("APROBACION");
            } else if (solicitud.estado() == EstadoAprobacion.RECHAZADA) {
                pendientes.add("SOLICITUD_RECHAZADA");
            }
        });
        return pendientes;
    }

    private SesionResultado rechazarContrasena(Usuario usuario, Instant ahora) {
        Usuario actualizado = usuario.registrarIntentoFallido(ahora, politica);
        boolean bloqueado = actualizado.estaBloqueado(ahora);
        String detalle = "intentosFallidos=" + (usuario.getIntentosFallidos() + 1) + ", bloqueada=" + bloqueado;
        transacciones.enTransaccion(() -> {
            cuentas.guardar(actualizado);
            auditoria.registrar(entrada(ahora, usuario, AccionAuditoria.INICIO_SESION_FALLIDO,
                    ResultadoAuditoria.FALLIDO, detalle));
        });
        if (bloqueado) {
            throw new CuentaBloqueadaExcepcion(actualizado.getBloqueadoHasta());
        }
        throw new CredencialesInvalidasExcepcion();
    }

    private SesionResultado abrirSesion(Usuario usuario, Instant ahora) {
        Usuario conectado = usuario.registrarInicioExitoso();
        TokenAcceso acceso = emisor.emitir(conectado, ahora);
        String refrescoPlano = TokensSeguros.generar();
        TokenRefresco refresco = TokenRefresco.emitir(conectado.getId(), TokensSeguros.sha256(refrescoPlano),
                ahora, vigenciaRefresco);
        transacciones.enTransaccion(() -> {
            if (usuario.requiereLimpiarIntentos()) {
                cuentas.guardar(conectado);
            }
            tokens.guardar(refresco);
            auditoria.registrar(entrada(ahora, conectado, AccionAuditoria.INICIO_SESION,
                    ResultadoAuditoria.EXITOSO, null));
        });
        return new SesionResultado(acceso.valor(), acceso.expiraEn(), refrescoPlano, refresco.expiraEn(),
                conectado.getId(), conectado.getNombreCompleto(), conectado.getRol());
    }

    private static EntradaAuditoria entrada(Instant ahora, Usuario usuario, AccionAuditoria accion,
                                            ResultadoAuditoria resultado, String detalle) {
        UUID idUsuario = usuario == null ? null : usuario.getId();
        return EntradaAuditoria.de(ahora, idUsuario, usuario, accion, RECURSO_USUARIO, idUsuario, null, resultado,
                detalle);
    }
}
