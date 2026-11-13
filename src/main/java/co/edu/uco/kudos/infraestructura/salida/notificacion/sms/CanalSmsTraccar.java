package co.edu.uco.kudos.infraestructura.salida.notificacion.sms;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Objects;

public class CanalSmsTraccar implements CanalNotificacion {

    private final RestClient cliente;
    private final String url;
    private final String token;
    private final String urlFrontend;

    public CanalSmsTraccar(RestClient cliente, String url, String token, String urlFrontend) {
        this.cliente = Objects.requireNonNull(cliente);
        this.url = Objects.requireNonNull(url);
        this.token = token;
        this.urlFrontend = urlFrontend == null ? "" : sinBarrasFinales(urlFrontend);
    }

    private static String sinBarrasFinales(String valor) {
        int fin = valor.length();
        while (fin > 0 && valor.charAt(fin - 1) == '/') {
            fin--;
        }
        return valor.substring(0, fin);
    }

    @Override
    public Canal canal() {
        return Canal.SMS;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("La pasarela SMS Traccar no está configurada (token)");
        }
        String numero = notificacion.getDestinatario() == null ? "" : notificacion.getDestinatario().replaceAll("\\D", "");
        if (numero.isEmpty()) {
            return ResultadoEnvio.noAplica("El destinatario no tiene un número de celular válido");
        }
        cliente.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("to", "+" + numero, "message", mensaje(notificacion.getDatos())))
                .retrieve()
                .toBodilessEntity();
        return ResultadoEnvio.exitoso();
    }

    String mensaje(Map<String, String> datos) {
        String codigo = datos.get("codigo");
        if (tieneValor(codigo)) {
            return "UCO Kudos: tu código de verificación es " + codigo + ". Vence en 15 minutos.";
        }
        String texto = "UCO Kudos: " + datos.getOrDefault("nombreAutor", "Un miembro de la comunidad")
                + " te dio un reconocimiento en la categoría " + datos.getOrDefault("nombreCategoria", "") + ".";
        String idKudo = datos.get("idKudo");
        return tieneValor(urlFrontend) && tieneValor(idKudo)
                ? texto + " Míralo en " + urlFrontend + "/kudos/" + idKudo
                : texto;
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
