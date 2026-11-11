package co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio;

import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.TokenRefrescoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenRefrescoRepositorioJpa extends JpaRepository<TokenRefrescoEntidad, UUID> {

    Optional<TokenRefrescoEntidad> findByHashToken(String hashToken);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TokenRefrescoEntidad t
            SET t.revocadoEn = :fecha, t.reemplazadoPor = :reemplazadoPor
            WHERE t.id = :id AND t.revocadoEn IS NULL
            """)
    int revocarSiVigente(@Param("id") UUID id, @Param("fecha") Instant fecha,
                         @Param("reemplazadoPor") UUID reemplazadoPor);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TokenRefrescoEntidad t
            SET t.revocadoEn = :fecha
            WHERE t.usuarioId = :usuarioId AND t.revocadoEn IS NULL
            """)
    int revocarTodosDeUsuario(@Param("usuarioId") UUID usuarioId, @Param("fecha") Instant fecha);
}
