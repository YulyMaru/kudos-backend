package co.edu.uco.kudos.aplicacion.dto;

import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public record CursorFeed(Instant fechaPublicacion, UUID id) {

    private static final String SEPARADOR = "|";

    public CursorFeed {
        Objects.requireNonNull(fechaPublicacion, "fechaPublicacion");
        Objects.requireNonNull(id, "id");
    }

    public static CursorFeed de(KudoResumenVista kudo) {
        return new CursorFeed(kudo.fechaPublicacion(), kudo.id());
    }

    public String codificar() {
        String texto = fechaPublicacion.getEpochSecond() + SEPARADOR + fechaPublicacion.getNano() + SEPARADOR + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorFeed decodificar(String cursor) {
        try {
            String texto = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] partes = texto.split("\\|");
            if (partes.length != 3) {
                throw new IllegalArgumentException("Formato de cursor inválido");
            }
            Instant fecha = Instant.ofEpochSecond(Long.parseLong(partes[0]), Long.parseLong(partes[1]));
            return new CursorFeed(fecha, UUID.fromString(partes[2]));
        } catch (IllegalArgumentException | DateTimeException error) {
            throw new ParametroInvalidoExcepcion("cursor", "El cursor de paginación no es válido");
        }
    }
}
