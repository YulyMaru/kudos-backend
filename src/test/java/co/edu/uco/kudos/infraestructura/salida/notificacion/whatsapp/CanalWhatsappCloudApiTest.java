package co.edu.uco.kudos.infraestructura.salida.notificacion.whatsapp;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CanalWhatsappCloudApiTest {

    private static final String URL_API = "https://graph.prueba/v23.0";

    private RestClient.Builder constructor;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        constructor = RestClient.builder().baseUrl(URL_API);
        servidor = MockRestServiceServer.bindTo(constructor).build();
    }

    @Test
    void enviaLaPlantillaConElAutorYLaCategoria() {
        servidor.expect(requestTo(URL_API + "/123456/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-prueba"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("573002222222"))
                .andExpect(jsonPath("$.type").value("template"))
                .andExpect(jsonPath("$.template.name").value("kudo_recibido"))
                .andExpect(jsonPath("$.template.language.code").value("es"))
                .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("Ana"))
                .andExpect(jsonPath("$.template.components[0].parameters[1].text").value("Liderazgo"))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.1\"}]}", MediaType.APPLICATION_JSON));

        ResultadoEnvio resultado = canal("token-prueba").enviar(notificacion("+57 300 222 2222"));

        assertThat(resultado.entregado()).isTrue();
        servidor.verify();
    }

    @Test
    void propagaLosErroresDelProveedorParaQueSeReintente() {
        servidor.expect(requestTo(URL_API + "/123456/messages")).andRespond(withServerError());

        assertThatThrownBy(() -> canal("token-prueba").enviar(notificacion("+573002222222")))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void fallaSiNoEstaConfigurado() {
        assertThatThrownBy(() -> canal("").enviar(notificacion("+573002222222")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void noAplicaSiElNumeroNoTieneDigitos() {
        ResultadoEnvio resultado = canal("token-prueba").enviar(notificacion("sin-numero"));

        assertThat(resultado.entregado()).isFalse();
        assertThat(resultado.motivo()).contains("número");
    }

    private CanalWhatsappCloudApi canal(String token) {
        CanalWhatsappCloudApi canal = new CanalWhatsappCloudApi(constructor.build(), token, "123456",
                "kudo_recibido", "es");
        assertThat(canal.canal()).isEqualTo(Canal.WHATSAPP);
        return canal;
    }

    private static Notificacion notificacion(String celular) {
        return Notificacion.pendiente(UUID.randomUUID(), UUID.randomUUID(), Canal.WHATSAPP, celular,
                Map.of("nombreAutor", "Ana", "nombreCategoria", "Liderazgo"), Instant.parse("2026-11-11T10:00:00Z"));
    }
}
