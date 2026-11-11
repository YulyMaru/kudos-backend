package co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio;

import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.NotificacionOutboxEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificacionOutboxRepositorioJpa extends JpaRepository<NotificacionOutboxEntidad, UUID> {

    List<NotificacionOutboxEntidad> findByIdCorrelacion(UUID idCorrelacion);

    long countByEstado(EstadoNotificacion estado);

    @Query(value = """
            SELECT *
            FROM outbox_notificacion
            WHERE estado IN ('PENDIENTE', 'REINTENTANDO')
              AND proximo_intento <= :ahora
            ORDER BY proximo_intento
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificacionOutboxEntidad> bloquearPendientes(@Param("ahora") Instant ahora, @Param("limite") int limite);
}
