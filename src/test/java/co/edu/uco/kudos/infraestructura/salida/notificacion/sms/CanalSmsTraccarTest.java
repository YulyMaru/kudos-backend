package co.edu.uco.kudos.infraestructura.salida.notificacion.sms;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CanalSmsTraccarTest {

    private static final String URL = "https://sms.prueba/sms/";

    private RestClient.Builder constructor;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
    }

    @Test
    void enviaElSmsDelKudoConElTokenSinBearer() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "token-prueba"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.to").value("+573002222222"))
                .andExpect(jsonPath("$.message").value("UCO Kudos: Ana te dio un reconocimiento en la categoría "
                        + "Liderazgo. Míralo en https://kudos.prueba/kudos/abc-123"))
                .andRespond(withSuccess());

        ResultadoEnvio resultado = canal("token-prueba", "https://kudos.prueba/")
                .enviar(notificacion("+57 300 222 2222",
                        Map.of("nombreAutor", "Ana", "nombreCategoria", "Liderazgo", "idKudo", "abc-123")));

        assertThat(resultado.entregado()).isTrue();
        servidor.verify();
    }

    @Test
    void enviaElCodigoDeVerificacion() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.to").value("+573002222222"))
                .andExpect(jsonPath("$.message")
                        .value("UCO Kudos: tu código de verificación es 482913. Vence en 15 minutos."))
                .andRespond(withSuccess());

        ResultadoEnvio resultado = canal("token-prueba", "https://kudos.prueba")
                .enviar(notificacion("+573002222222", Map.of("plantilla", "CODIGO_VERIFICACION", "codigo", "482913")));

        assertThat(resultado.entregado()).isTrue();
        servidor.verify();
    }

    @Test
    void omiteElEnlaceSiFaltaLaUrlOElIdentificador() {
        String esperado = "UCO Kudos: Ana te dio un reconocimiento en la categoría Liderazgo.";
        Map<String, String> sinId = Map.of("nombreAutor", "Ana", "nombreCategoria", "Liderazgo");
        Map<String, String> conId = Map.of("nombreAutor", "Ana", "nombreCategoria", "Liderazgo", "idKudo", "abc");

        assertThat(canal("token-prueba", "https://kudos.prueba").mensaje(sinId)).isEqualTo(esperado);
        assertThat(canal("token-prueba", "").mensaje(conId)).isEqualTo(esperado);
    }

    @Test
    void propagaLosErroresHttpParaQueSeReintente() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> canal("token-prueba", "https://kudos.prueba")
                .enviar(notificacion("+573002222222", Map.of("codigo", "1"))))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void fallaSiNoEstaConfigurado() {
        assertThatThrownBy(() -> canal("", "https://kudos.prueba")
                .enviar(notificacion("+573002222222", Map.of("codigo", "1"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está configurada");
    }

    @Test
    void noAplicaSiNoHayCelular() {
        ResultadoEnvio resultado = canal("token-prueba", "https://kudos.prueba")
                .enviar(notificacion("sin-numero", Map.of("codigo", "1")));

        assertThat(resultado.entregado()).isFalse();
        assertThat(resultado.motivo()).contains("número");
    }

    private CanalSmsTraccar canal(String token, String urlFrontend) {
        CanalSmsTraccar canal = new CanalSmsTraccar(constructor.build(), URL, token, urlFrontend);
        assertThat(canal.canal()).isEqualTo(Canal.SMS);
        return canal;
    }

    private static Notificacion notificacion(String celular, Map<String, String> datos) {
        return Notificacion.pendiente(UUID.randomUUID(), UUID.randomUUID(), Canal.SMS, celular, datos,
                Instant.parse("2026-11-11T10:00:00Z"));
    }
}
