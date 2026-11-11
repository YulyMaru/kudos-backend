package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

final class ConversionesJdbc {

    private ConversionesJdbc() {
    }

    static OffsetDateTime fecha(Instant instante) {
        return instante == null ? null : OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    static Instant instante(ResultSet fila, String columna) throws SQLException {
        OffsetDateTime valor = fila.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    static UUID uuid(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, UUID.class);
    }

    static String texto(UUID valor) {
        return valor == null ? null : valor.toString();
    }
}
