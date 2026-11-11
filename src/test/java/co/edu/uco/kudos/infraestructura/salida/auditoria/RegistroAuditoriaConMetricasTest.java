package co.edu.uco.kudos.infraestructura.salida.auditoria;

import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroAuditoriaConMetricasTest {

    private final SimpleMeterRegistry metricas = new SimpleMeterRegistry();
    private final List<EntradaAuditoria> guardadas = new ArrayList<>();
    private final RegistroAuditoriaConMetricas registro = new RegistroAuditoriaConMetricas(guardadas::add, metricas);

    @Test
    void cuentaKudosPublicadosYNotificacionesPorCanal() {
        registro.registrar(entrada(AccionAuditoria.KUDO_PUBLICADO, null, ResultadoAuditoria.EXITOSO));
        registro.registrar(entrada(AccionAuditoria.NOTIFICACION_ENVIADA, Canal.EMAIL, ResultadoAuditoria.EXITOSO));
        registro.registrar(entrada(AccionAuditoria.NOTIFICACION_FALLIDA, Canal.WHATSAPP, ResultadoAuditoria.FALLIDO));
        registro.registrar(entrada(AccionAuditoria.NOTIFICACION_FALLIDA, Canal.WHATSAPP, ResultadoAuditoria.FALLIDO));

        assertThat(guardadas).hasSize(4);
        assertThat(metricas.counter(RegistroAuditoriaConMetricas.METRICA_KUDOS_PUBLICADOS).count()).isEqualTo(1);
        assertThat(metricas.counter(RegistroAuditoriaConMetricas.METRICA_NOTIFICACIONES,
                "canal", "EMAIL", "resultado", "EXITOSO").count()).isEqualTo(1);
        assertThat(metricas.counter(RegistroAuditoriaConMetricas.METRICA_NOTIFICACIONES,
                "canal", "WHATSAPP", "resultado", "FALLIDO").count()).isEqualTo(2);
    }

    @Test
    void dentroDeUnaTransaccionSoloCuentaDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            registro.registrar(entrada(AccionAuditoria.KUDO_PUBLICADO, null, ResultadoAuditoria.EXITOSO));

            assertThat(metricas.counter(RegistroAuditoriaConMetricas.METRICA_KUDOS_PUBLICADOS).count()).isZero();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            assertThat(metricas.counter(RegistroAuditoriaConMetricas.METRICA_KUDOS_PUBLICADOS).count()).isEqualTo(1);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void siLaTransaccionSeRevierteNoCuenta() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            registro.registrar(entrada(AccionAuditoria.KUDO_PUBLICADO, null, ResultadoAuditoria.EXITOSO));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sincronizacion -> sincronizacion.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            assertThat(metricas.counter(RegistroAuditoriaConMetricas.METRICA_KUDOS_PUBLICADOS).count()).isZero();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static EntradaAuditoria entrada(AccionAuditoria accion, Canal canal, ResultadoAuditoria resultado) {
        UUID id = UUID.randomUUID();
        return new EntradaAuditoria(UUID.randomUUID(), Instant.now(), id, null, null, accion, "KUDO", id, canal,
                resultado, null);
    }
}
