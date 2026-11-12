package co.edu.uco.kudos.infraestructura.salida.persistencia.consulta;

import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;
import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultasKudosAdaptadorTest {

    private static final Instant FECHA = Instant.parse("2026-11-11T10:00:00Z");

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ConsultasKudosAdaptador consultas = new ConsultasKudosAdaptador(jdbc);
    private final UUID kudoId = UUID.randomUUID();
    private final UUID autorId = UUID.randomUUID();
    private final UUID destinatarioId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();
    private final UUID adjuntoId = UUID.randomUUID();
    private ResultSet fila;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void prepararFila() throws Exception {
        fila = mock(ResultSet.class);
        when(fila.getObject("id", UUID.class)).thenReturn(kudoId);
        when(fila.getObject("fecha_publicacion", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.ofInstant(FECHA, ZoneOffset.UTC));
        when(fila.getString("extracto")).thenReturn("Gracias");
        when(fila.getBoolean("truncado")).thenReturn(true);
        when(fila.getString("contenido")).thenReturn("Gracias por todo");
        when(fila.getObject("autor_id", UUID.class)).thenReturn(autorId);
        when(fila.getString("autor_nombre")).thenReturn("Ana");
        when(fila.getString("autor_rol")).thenReturn("DOCENTE");
        when(fila.getObject("destinatario_id", UUID.class)).thenReturn(destinatarioId);
        when(fila.getString("destinatario_nombre")).thenReturn("Diego");
        when(fila.getString("destinatario_rol")).thenReturn("ESTUDIANTE");
        when(fila.getObject("categoria_id", UUID.class)).thenReturn(categoriaId);
        when(fila.getString("categoria_nombre")).thenReturn("Liderazgo");
        when(fila.getInt("cantidad_adjuntos")).thenReturn(1);
        when(fila.getLong("cantidad_replicas")).thenReturn(2L);
        when(fila.getDouble("promedio")).thenReturn(4.5);
        when(fila.getLong("cantidad_calificaciones")).thenReturn(3L);
        when(fila.getString("nombre_original")).thenReturn("foto.png");
        when(fila.getString("tipo_contenido")).thenReturn("image/png");
        when(fila.getLong("tamano_bytes")).thenReturn(10L);
        when(fila.getString("clave_objeto")).thenReturn("kudos/clave.png");
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocacion -> List.of(((RowMapper) invocacion.getArgument(2)).mapRow(fila, 0)));
    }

    private KudoResumenVista resumenEsperado() {
        return new KudoResumenVista(kudoId, new PersonaVista(autorId, "Ana", Rol.DOCENTE),
                new PersonaVista(destinatarioId, "Diego", Rol.ESTUDIANTE), new CategoriaVista(categoriaId, "Liderazgo"),
                "Gracias", true, FECHA, 1, 2L, 4.5, 3L);
    }

    @Test
    void consultaLaPrimeraPaginaDelFeedSinCursor() {
        List<KudoResumenVista> feed = consultas.feed(null, 20);

        assertThat(feed).containsExactly(resumenEsperado());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parametros = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(sql.capture(), parametros.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).doesNotContain(":fecha").contains("LIMIT :limite");
        assertThat(parametros.getValue().getValue("limite")).isEqualTo(20);
        assertThat(parametros.getValue().getValue("longitudExtracto"))
                .isEqualTo(ConsultasKudosAdaptador.LONGITUD_EXTRACTO);
        assertThat(parametros.getValue().hasValue("fecha")).isFalse();
    }

    @Test
    void consultaLaSiguientePaginaDelFeedDespuesDelCursor() {
        UUID idCursor = UUID.randomUUID();

        List<KudoResumenVista> feed = consultas.feed(new CursorFeed(FECHA, idCursor), 5);

        assertThat(feed).containsExactly(resumenEsperado());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parametros = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(sql.capture(), parametros.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).contains("(k.fecha_publicacion, k.id) < (:fecha, :id)");
        assertThat(parametros.getValue().getValue("limite")).isEqualTo(5);
        assertThat(parametros.getValue().getValue("fecha")).isEqualTo(OffsetDateTime.ofInstant(FECHA, ZoneOffset.UTC));
        assertThat(parametros.getValue().getValue("id")).isEqualTo(idCursor);
        assertThat(parametros.getValue().getValue("longitudExtracto"))
                .isEqualTo(ConsultasKudosAdaptador.LONGITUD_EXTRACTO);
    }

    @Test
    void consultaElDetalleConSusAdjuntos() throws Exception {
        when(fila.getObject("id", UUID.class)).thenReturn(kudoId, adjuntoId);

        Optional<KudoDetalleDatos> detalle = consultas.detalle(kudoId);

        assertThat(detalle).contains(new KudoDetalleDatos(kudoId, new PersonaVista(autorId, "Ana", Rol.DOCENTE),
                new PersonaVista(destinatarioId, "Diego", Rol.ESTUDIANTE), new CategoriaVista(categoriaId, "Liderazgo"),
                "Gracias por todo", FECHA,
                List.of(new AdjuntoDatos(adjuntoId, "foto.png", "image/png", 10L, "kudos/clave.png")),
                2L, 4.5, 3L));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parametros = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc, times(2)).query(sql.capture(), parametros.capture(), any(RowMapper.class));
        assertThat(sql.getAllValues().get(0)).contains("AND k.id = :id");
        assertThat(sql.getAllValues().get(1)).contains("FROM adjunto").contains("WHERE kudo_id = :id");
        assertThat(parametros.getAllValues().get(0).getValue("id")).isEqualTo(kudoId);
        assertThat(parametros.getAllValues().get(0).getValue("longitudExtracto"))
                .isEqualTo(ConsultasKudosAdaptador.LONGITUD_EXTRACTO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void devuelveVacioSiElDetalleNoExiste() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(consultas.detalle(kudoId)).isEmpty();
        verify(jdbc, times(1)).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void consultaUnAdjuntoDeUnKudoNoVetado() throws Exception {
        when(fila.getObject("id", UUID.class)).thenReturn(adjuntoId);

        Optional<AdjuntoDatos> adjunto = consultas.adjunto(kudoId, adjuntoId);

        assertThat(adjunto).contains(new AdjuntoDatos(adjuntoId, "foto.png", "image/png", 10L, "kudos/clave.png"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parametros = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(sql.capture(), parametros.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).contains("k.vetado = FALSE");
        assertThat(parametros.getValue().getValue("kudoId")).isEqualTo(kudoId);
        assertThat(parametros.getValue().getValue("adjuntoId")).isEqualTo(adjuntoId);
    }
}
