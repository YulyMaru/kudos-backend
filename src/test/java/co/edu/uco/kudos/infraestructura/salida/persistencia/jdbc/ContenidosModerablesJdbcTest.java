package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;

import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContenidosModerablesJdbcTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ContenidosModerablesJdbc contenidos = new ContenidosModerablesJdbc(jdbc);
    private final UUID id = UUID.randomUUID();
    private final UUID kudoId = UUID.randomUUID();
    private final UUID autorId = UUID.randomUUID();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void responderCon(ResultSet fila) {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocacion -> List.of(((RowMapper) invocacion.getArgument(2)).mapRow(fila, 0)));
    }

    private ResultSet fila(boolean vetado) throws Exception {
        ResultSet fila = mock(ResultSet.class);
        when(fila.getObject("id", UUID.class)).thenReturn(id);
        when(fila.getObject("kudo_id", UUID.class)).thenReturn(kudoId);
        when(fila.getObject("autor_id", UUID.class)).thenReturn(autorId);
        when(fila.getBoolean("vetado")).thenReturn(vetado);
        return fila;
    }

    @Test
    void buscaUnKudoYMapeaSuEstado() throws Exception {
        responderCon(fila(false));

        Optional<EstadoContenido> estado = contenidos.buscar(TipoContenido.KUDO, id);

        assertThat(estado).contains(new EstadoContenido(TipoContenido.KUDO, id, kudoId, autorId, false));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parametros = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(sql.capture(), parametros.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).contains("FROM kudo").doesNotContain("replica");
        assertThat(parametros.getValue().getValue("id")).isEqualTo(id);
    }

    @Test
    void buscaUnaReplicaConsiderandoElVetoDelKudo() throws Exception {
        responderCon(fila(true));

        Optional<EstadoContenido> estado = contenidos.buscar(TipoContenido.REPLICA, id);

        assertThat(estado).contains(new EstadoContenido(TipoContenido.REPLICA, id, kudoId, autorId, true));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));
        assertThat(sql.getValue()).contains("FROM replica r").contains("k.vetado");
    }

    @Test
    @SuppressWarnings("unchecked")
    void devuelveVacioSiElContenidoNoExiste() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertThat(contenidos.buscar(TipoContenido.KUDO, id)).isEmpty();
    }

    @Test
    void marcaVetadoUnKudo() {
        contenidos.marcarVetado(TipoContenido.KUDO, id);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> parametros = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sql.capture(), parametros.capture());
        assertThat(sql.getValue()).isEqualTo("UPDATE kudo SET vetado = TRUE WHERE id = :id");
        assertThat(parametros.getValue().getValue("id")).isEqualTo(id);
    }

    @Test
    void marcaVetadaUnaReplica() {
        contenidos.marcarVetado(TipoContenido.REPLICA, id);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(MapSqlParameterSource.class));
        assertThat(sql.getValue()).isEqualTo("UPDATE replica SET vetado = TRUE WHERE id = :id");
    }
}
