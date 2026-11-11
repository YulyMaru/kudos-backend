package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioNotificacionesPendientes;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.regla.PoliticaReintentos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcesarNotificacionesCasoUsoTest {

    private static final Instant INICIO = Instant.parse("2026-11-11T10:00:00Z");
    private static final UUID ID_KUDO = UUID.randomUUID();
    private static final UUID ID_EVENTO = UUID.randomUUID();

    private final PoliticaReintentos politica = new PoliticaReintentos(5, Duration.ofSeconds(10), Duration.ofMinutes(10));

    private RelojAjustable reloj;
    private PendientesEnMemoria pendientes;
    private List<EntradaAuditoria> auditoria;
    private CanalFalso correo;
    private CanalFalso whatsapp;
    private CanalFalso push;

    @BeforeEach
    void preparar() {
        reloj = new RelojAjustable(INICIO);
        pendientes = new PendientesEnMemoria();
        auditoria = new ArrayList<>();
        correo = new CanalFalso(Canal.EMAIL);
        whatsapp = new CanalFalso(Canal.WHATSAPP);
        push = new CanalFalso(Canal.PUSH);
        pendientes.agregar(notificacion(Canal.EMAIL, "diego@uco.net.co"));
        pendientes.agregar(notificacion(Canal.WHATSAPP, "+573002222222"));
        pendientes.agregar(notificacion(Canal.PUSH, UUID.randomUUID().toString()));
    }

    @Test
    void enviaLosTresCanalesYAuditaCadaEnvio() {
        int procesadas = casoUso(List.of(correo, whatsapp, push)).procesarPendientes();

        assertThat(procesadas).isEqualTo(3);
        assertThat(pendientes.estados()).containsOnly(EstadoNotificacion.ENVIADO);
        assertThat(correo.enviosExitosos).isEqualTo(1);
        assertThat(whatsapp.enviosExitosos).isEqualTo(1);
        assertThat(push.enviosExitosos).isEqualTo(1);
        assertThat(auditoria).hasSize(3).allSatisfy(entrada -> {
            assertThat(entrada.accion()).isEqualTo(AccionAuditoria.NOTIFICACION_ENVIADA);
            assertThat(entrada.idCorrelacion()).isEqualTo(ID_KUDO);
        });
        assertThat(auditoria).extracting(EntradaAuditoria::canal)
                .containsExactlyInAnyOrder(Canal.EMAIL, Canal.WHATSAPP, Canal.PUSH);
    }

    @Test
    void laFallaDeUnCanalNoAfectaALosDemas() {
        whatsapp.fallarSiempre();

        casoUso(List.of(correo, whatsapp, push)).procesarPendientes();

        assertThat(pendientes.de(Canal.EMAIL).getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
        assertThat(pendientes.de(Canal.PUSH).getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
        Notificacion notificacionWhatsapp = pendientes.de(Canal.WHATSAPP);
        assertThat(notificacionWhatsapp.getEstado()).isEqualTo(EstadoNotificacion.REINTENTANDO);
        assertThat(notificacionWhatsapp.getIntentos()).isEqualTo(1);
        assertThat(notificacionWhatsapp.getProximoIntento()).isEqualTo(INICIO.plusSeconds(5));
        assertThat(notificacionWhatsapp.getUltimoError()).contains("Proveedor caído");
    }

    @Test
    void unaFallaTemporalSeEntregaEnElCuartoIntentoYQuedaAuditadoCadaIntento() {
        whatsapp.fallarLasPrimeras(3);
        ProcesarNotificacionesCasoUso casoUso = casoUso(List.of(correo, whatsapp, push));

        for (int ronda = 0; ronda < 6; ronda++) {
            casoUso.procesarPendientes();
            reloj.avanzar(Duration.ofHours(1));
        }

        Notificacion notificacionWhatsapp = pendientes.de(Canal.WHATSAPP);
        assertThat(notificacionWhatsapp.getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
        assertThat(notificacionWhatsapp.getIntentos()).isEqualTo(4);
        assertThat(whatsapp.intentos).isEqualTo(4);
        assertThat(whatsapp.enviosExitosos).isEqualTo(1);
        assertThat(auditoria).filteredOn(entrada -> entrada.canal() == Canal.WHATSAPP)
                .extracting(EntradaAuditoria::accion)
                .containsExactly(AccionAuditoria.NOTIFICACION_FALLIDA, AccionAuditoria.NOTIFICACION_FALLIDA,
                        AccionAuditoria.NOTIFICACION_FALLIDA, AccionAuditoria.NOTIFICACION_ENVIADA);
    }

    @Test
    void unaFallaPermanenteQuedaFallidaTrasCincoIntentosYNoSeReintentaMas() {
        correo.fallarSiempre();
        ProcesarNotificacionesCasoUso casoUso = casoUso(List.of(correo, whatsapp, push));

        for (int ronda = 0; ronda < 8; ronda++) {
            casoUso.procesarPendientes();
            reloj.avanzar(Duration.ofHours(1));
        }

        Notificacion notificacionCorreo = pendientes.de(Canal.EMAIL);
        assertThat(notificacionCorreo.getEstado()).isEqualTo(EstadoNotificacion.FALLIDO);
        assertThat(notificacionCorreo.getIntentos()).isEqualTo(5);
        assertThat(correo.intentos).isEqualTo(5);
        assertThat(auditoria).filteredOn(entrada -> entrada.canal() == Canal.EMAIL).last()
                .satisfies(entrada -> assertThat(entrada.detalle()).contains("estado=FALLIDO"));
    }

    @Test
    void unaNotificacionYaEnviadaNoSeEnviaDeNuevo() {
        ProcesarNotificacionesCasoUso casoUso = casoUso(List.of(correo, whatsapp, push));

        casoUso.procesarPendientes();
        reloj.avanzar(Duration.ofHours(1));
        casoUso.procesarPendientes();
        casoUso.procesarPendientes();

        assertThat(correo.intentos).isEqualTo(1);
        assertThat(whatsapp.intentos).isEqualTo(1);
        assertThat(push.intentos).isEqualTo(1);
    }

    @Test
    void noReintentaAntesDeLaFechaProgramada() {
        whatsapp.fallarLasPrimeras(1);
        ProcesarNotificacionesCasoUso casoUso = casoUso(List.of(correo, whatsapp, push));

        casoUso.procesarPendientes();
        reloj.avanzar(Duration.ofSeconds(1));
        casoUso.procesarPendientes();

        assertThat(whatsapp.intentos).isEqualTo(1);
        assertThat(pendientes.de(Canal.WHATSAPP).getEstado()).isEqualTo(EstadoNotificacion.REINTENTANDO);
    }

    @Test
    void elPushParaUnUsuarioDesconectadoQuedaComoNoAplica() {
        push.responderNoAplica("El destinatario no está conectado");

        casoUso(List.of(correo, whatsapp, push)).procesarPendientes();

        assertThat(pendientes.de(Canal.PUSH).getEstado()).isEqualTo(EstadoNotificacion.NO_APLICA);
        assertThat(auditoria).filteredOn(entrada -> entrada.canal() == Canal.PUSH).singleElement()
                .satisfies(entrada -> assertThat(entrada.accion()).isEqualTo(AccionAuditoria.NOTIFICACION_NO_APLICA));
    }

    @Test
    void unCanalSinAdaptadorSeRegistraComoFallo() {
        casoUso(List.of(correo, push)).procesarPendientes();

        Notificacion notificacionWhatsapp = pendientes.de(Canal.WHATSAPP);
        assertThat(notificacionWhatsapp.getEstado()).isEqualTo(EstadoNotificacion.REINTENTANDO);
        assertThat(notificacionWhatsapp.getUltimoError()).contains("No hay adaptador");
    }

    @Test
    void siFallaGuardarElResultadoDeUnaNotificacionLasDemasSiguen() {
        pendientes.fallarAlActualizar(pendientes.de(Canal.EMAIL).getId());

        int procesadas = casoUso(List.of(correo, whatsapp, push)).procesarPendientes();

        assertThat(procesadas).isEqualTo(2);
        assertThat(pendientes.de(Canal.WHATSAPP).getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
        assertThat(pendientes.de(Canal.PUSH).getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
    }

    @Test
    void rechazaDosAdaptadoresParaElMismoCanal() {
        List<CanalNotificacion> canales = List.of(correo, new CanalFalso(Canal.EMAIL));

        assertThatThrownBy(() -> casoUso(canales)).isInstanceOf(IllegalArgumentException.class);
    }

    private ProcesarNotificacionesCasoUso casoUso(List<CanalNotificacion> canales) {
        RegistroAuditoria registro = auditoria::add;
        GestorTransacciones transacciones = Runnable::run;
        return new ProcesarNotificacionesCasoUso(pendientes, canales, registro, transacciones, reloj, politica,
                10, Duration.ofMinutes(2), () -> 0.0);
    }

    private static Notificacion notificacion(Canal canal, String destinatario) {
        return Notificacion.pendiente(ID_EVENTO, ID_KUDO, canal, destinatario,
                Map.of("nombreAutor", "Ana", "nombreCategoria", "Liderazgo", "idKudo", ID_KUDO.toString()), INICIO);
    }

    private static final class CanalFalso implements CanalNotificacion {
        private final Canal canal;
        private int fallosRestantes;
        private boolean fallarSiempre;
        private String motivoNoAplica;
        private int intentos;
        private int enviosExitosos;

        CanalFalso(Canal canal) {
            this.canal = canal;
        }

        void fallarSiempre() {
            this.fallarSiempre = true;
        }

        void fallarLasPrimeras(int veces) {
            this.fallosRestantes = veces;
        }

        void responderNoAplica(String motivo) {
            this.motivoNoAplica = motivo;
        }

        @Override
        public Canal canal() {
            return canal;
        }

        @Override
        public ResultadoEnvio enviar(Notificacion notificacion) {
            intentos++;
            if (fallarSiempre || fallosRestantes > 0) {
                fallosRestantes--;
                throw new IllegalStateException("Proveedor caído");
            }
            if (motivoNoAplica != null) {
                return ResultadoEnvio.noAplica(motivoNoAplica);
            }
            enviosExitosos++;
            return ResultadoEnvio.exitoso();
        }
    }

    private final class PendientesEnMemoria implements RepositorioNotificacionesPendientes {
        private final Map<UUID, Notificacion> almacen = new LinkedHashMap<>();
        private final Set<UUID> idsQueFallan = new HashSet<>();

        void agregar(Notificacion notificacion) {
            almacen.put(notificacion.getId(), notificacion);
        }

        void fallarAlActualizar(UUID id) {
            idsQueFallan.add(id);
        }

        Notificacion de(Canal canal) {
            return almacen.values().stream().filter(n -> n.getCanal() == canal).findFirst().orElseThrow();
        }

        List<EstadoNotificacion> estados() {
            return almacen.values().stream().map(Notificacion::getEstado).toList();
        }

        @Override
        public List<Notificacion> reservarPendientes(Instant ahora, int limite, Duration duracionReserva) {
            return almacen.values().stream()
                    .filter(Notificacion::puedeProcesarse)
                    .filter(n -> !n.getProximoIntento().isAfter(ahora))
                    .limit(limite)
                    .toList();
        }

        @Override
        public void actualizar(Notificacion notificacion) {
            if (idsQueFallan.contains(notificacion.getId())) {
                throw new IllegalStateException("Base de datos no disponible");
            }
            almacen.put(notificacion.getId(), notificacion);
        }
    }

    private static final class RelojAjustable extends Clock {
        private Instant actual;

        RelojAjustable(Instant inicio) {
            this.actual = inicio;
        }

        void avanzar(Duration duracion) {
            actual = actual.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return actual;
        }
    }
}
