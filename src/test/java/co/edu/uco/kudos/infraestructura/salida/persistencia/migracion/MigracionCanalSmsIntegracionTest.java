package co.edu.uco.kudos.infraestructura.salida.persistencia.migracion;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class MigracionCanalSmsIntegracionTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    private DataSource fuenteDatos;
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararBaseDeDatos() {
        fuenteDatos = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
        jdbc = new JdbcTemplate(fuenteDatos);
        flyway(null).clean();
    }

    @Test
    void convierteLosCanalesWhatsappExistentesASms() {
        flyway("5").migrate();
        UUID outboxWhatsapp = insertarOutbox("WHATSAPP");
        UUID outboxCorreo = insertarOutbox("EMAIL");
        UUID auditoriaWhatsapp = insertarAuditoria("WHATSAPP");
        UUID auditoriaPush = insertarAuditoria("PUSH");

        flyway(null).migrate();

        assertThat(canalOutbox(outboxWhatsapp)).isEqualTo("SMS");
        assertThat(canalOutbox(outboxCorreo)).isEqualTo("EMAIL");
        assertThat(canalAuditoria(auditoriaWhatsapp)).isEqualTo("SMS");
        assertThat(canalAuditoria(auditoriaPush)).isEqualTo("PUSH");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_notificacion WHERE canal = 'WHATSAPP'",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_auditoria WHERE canal = 'WHATSAPP'",
                Integer.class)).isZero();
    }

    @Test
    void laRestriccionDelOutboxAceptaSmsYRechazaWhatsapp() {
        flyway(null).migrate();

        assertThat(canalOutbox(insertarOutbox("SMS"))).isEqualTo("SMS");
        assertThatThrownBy(() -> insertarOutbox("WHATSAPP"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_outbox_canal");
    }

    private Flyway flyway(String version) {
        var configuracion = Flyway.configure()
                .dataSource(fuenteDatos)
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (version != null) {
            configuracion.target(version);
        }
        return configuracion.load();
    }

    private UUID insertarOutbox(String canal) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO outbox_notificacion (id, id_evento, id_correlacion, canal, payload, estado, creado_en)
                VALUES (?, ?, ?, ?, '{}'::jsonb, 'PENDIENTE', now())
                """, id, UUID.randomUUID(), UUID.randomUUID(), canal);
        return id;
    }

    private UUID insertarAuditoria(String canal) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO registro_auditoria (id, fecha_hora, accion, canal, resultado)
                VALUES (?, now(), 'NOTIFICACION_ENVIADA', ?, 'EXITOSO')
                """, id, canal);
        return id;
    }

    private String canalOutbox(UUID id) {
        return jdbc.queryForObject("SELECT canal FROM outbox_notificacion WHERE id = ?", String.class, id);
    }

    private String canalAuditoria(UUID id) {
        return jdbc.queryForObject("SELECT canal FROM registro_auditoria WHERE id = ?", String.class, id);
    }
}
