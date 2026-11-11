package co.edu.uco.kudos.infraestructura.salida.auditoria;

import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

public class RegistroAuditoriaConMetricas implements RegistroAuditoria {

    public static final String METRICA_EVENTOS = "kudos.auditoria.eventos";
    public static final String METRICA_KUDOS_PUBLICADOS = "kudos.publicados";
    public static final String METRICA_NOTIFICACIONES = "kudos.notificaciones";
    public static final String METRICA_SESIONES = "kudos.sesiones";

    private final RegistroAuditoria delegado;
    private final MeterRegistry registro;

    public RegistroAuditoriaConMetricas(RegistroAuditoria delegado, MeterRegistry registro) {
        this.delegado = Objects.requireNonNull(delegado);
        this.registro = Objects.requireNonNull(registro);
    }

    @Override
    public void registrar(EntradaAuditoria entrada) {
        delegado.registrar(entrada);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contar(entrada);
                }
            });
        } else {
            contar(entrada);
        }
    }

    private void contar(EntradaAuditoria entrada) {
        String resultado = entrada.resultado().name();
        registro.counter(METRICA_EVENTOS, "accion", entrada.accion().name(), "resultado", resultado).increment();
        switch (entrada.accion()) {
            case KUDO_PUBLICADO -> registro.counter(METRICA_KUDOS_PUBLICADOS).increment();
            case NOTIFICACION_ENVIADA, NOTIFICACION_FALLIDA, NOTIFICACION_NO_APLICA -> registro.counter(
                    METRICA_NOTIFICACIONES, "canal", String.valueOf(entrada.canal()), "resultado", resultado)
                    .increment();
            case INICIO_SESION, INICIO_SESION_FALLIDO -> registro.counter(METRICA_SESIONES, "resultado", resultado)
                    .increment();
            default -> {
            }
        }
    }
}
