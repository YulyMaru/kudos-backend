package co.edu.uco.kudos.infraestructura.salida.notificacion.whatsapp;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CanalWhatsappCloudApi implements CanalNotificacion {

    private static final String PLANTILLA_CODIGO_POR_DEFECTO = "codigo_verificacion";

    private final RestClient cliente;
    private final String token;
    private final String telefonoId;
    private final String plantilla;
    private final String plantillaCodigo;
    private final String idioma;

    public CanalWhatsappCloudApi(RestClient cliente, String token, String telefonoId, String plantilla,
                                 String idioma) {
        this(cliente, token, telefonoId, plantilla, PLANTILLA_CODIGO_POR_DEFECTO, idioma);
    }

    public CanalWhatsappCloudApi(RestClient cliente, String token, String telefonoId, String plantilla,
                                 String plantillaCodigo, String idioma) {
        this.cliente = Objects.requireNonNull(cliente);
        this.token = token;
        this.telefonoId = telefonoId;
        this.plantilla = Objects.requireNonNull(plantilla);
        this.plantillaCodigo = Objects.requireNonNull(plantillaCodigo);
        this.idioma = Objects.requireNonNull(idioma);
    }

    @Override
    public Canal canal() {
        return Canal.WHATSAPP;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        if (token == null || token.isBlank() || telefonoId == null || telefonoId.isBlank()) {
            throw new IllegalStateException("WhatsApp Cloud API no está configurado (token o identificador de teléfono)");
        }
        String numero = notificacion.getDestinatario() == null ? "" : notificacion.getDestinatario().replaceAll("\\D", "");
        if (numero.isEmpty()) {
            return ResultadoEnvio.noAplica("El destinatario no tiene un número de WhatsApp válido");
        }
        cliente.post()
                .uri("/{telefonoId}/messages", telefonoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo(numero, notificacion.getDatos()))
                .retrieve()
                .toBodilessEntity();
        return ResultadoEnvio.exitoso();
    }

    private Map<String, Object> cuerpo(String numero, Map<String, String> datos) {
        boolean esCodigo = "CODIGO_VERIFICACION".equals(datos.get("plantilla"));
        List<Map<String, String>> parametros = esCodigo
                ? List.of(Map.of("type", "text", "text", datos.getOrDefault("codigo", "")))
                : List.of(
                Map.of("type", "text", "text", datos.getOrDefault("nombreAutor", "Un miembro de la comunidad")),
                Map.of("type", "text", "text", datos.getOrDefault("nombreCategoria", "")));
        return Map.of(
                "messaging_product", "whatsapp",
                "to", numero,
                "type", "template",
                "template", Map.of(
                        "name", esCodigo ? plantillaCodigo : plantilla,
                        "language", Map.of("code", idioma),
                        "components", List.of(Map.of("type", "body", "parameters", parametros))));
    }
}
