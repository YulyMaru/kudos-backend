package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.KudoPublicadoResultado;
import co.edu.uco.kudos.aplicacion.dto.PublicarKudoComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarKudoPuerto;
import co.edu.uco.kudos.configuracion.SeguridadConfiguracion;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.infraestructura.salida.seguridad.EmisorTokensJwt;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KudoControlador.class,
        properties = "kudos.seguridad.jwt-secreto=secreto-de-pruebas-con-al-menos-32-bytes")
@Import(SeguridadConfiguracion.class)
class KudoControladorTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID DESTINATARIO = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();
    private static final UUID KUDO = UUID.randomUUID();
    private static final Instant FECHA = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicarKudoPuerto publicarKudo;

    @Autowired
    private JwtEncoder codificadorJwt;

    @Test
    void aceptaUnTokenRealEmitidoPorLaPlataforma() throws Exception {
        when(publicarKudo.publicar(any())).thenReturn(new KudoPublicadoResultado(KUDO, FECHA));
        Usuario autor = new Usuario(AUTOR, "Ana", "ana@uco.net.co", null, false, Rol.DOCENTE, EstadoUsuario.ACTIVO);
        String token = new EmisorTokensJwt(codificadorJwt, "uco-kudos", Duration.ofMinutes(15))
                .emitir(autor, Instant.now()).valor();

        mockMvc.perform(post("/api/v1/kudos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() throws Exception {
        SecretKey otraClave = new SecretKeySpec(
                "otro-secreto-distinto-de-al-menos-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Usuario autor = new Usuario(AUTOR, "Ana", "ana@uco.net.co", null, false, Rol.DOCENTE, EstadoUsuario.ACTIVO);
        String token = new EmisorTokensJwt(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(otraClave)),
                "uco-kudos", Duration.ofMinutes(15)).emitir(autor, Instant.now()).valor();

        mockMvc.perform(post("/api/v1/kudos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isUnauthorized());

        verify(publicarKudo, never()).publicar(any());
    }

    @Test
    void publicaKudoEnJsonConElAutorTomadoDelToken() throws Exception {
        when(publicarKudo.publicar(any())).thenReturn(new KudoPublicadoResultado(KUDO, FECHA));

        mockMvc.perform(post("/api/v1/kudos")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/kudos/" + KUDO))
                .andExpect(jsonPath("$.id").value(KUDO.toString()))
                .andExpect(jsonPath("$.fechaPublicacion").value("2026-11-11T10:00:00Z"));

        ArgumentCaptor<PublicarKudoComando> comando = ArgumentCaptor.forClass(PublicarKudoComando.class);
        verify(publicarKudo).publicar(comando.capture());
        assertThat(comando.getValue().autorId()).isEqualTo(AUTOR);
        assertThat(comando.getValue().destinatarioId()).isEqualTo(DESTINATARIO);
        assertThat(comando.getValue().categoriaId()).isEqualTo(CATEGORIA);
        assertThat(comando.getValue().contenido()).isEqualTo("Gracias por tu apoyo");
        assertThat(comando.getValue().adjuntos()).isEmpty();
    }

    @Test
    void publicaKudoMultipartConAdjuntos() throws Exception {
        when(publicarKudo.publicar(any())).thenReturn(new KudoPublicadoResultado(KUDO, FECHA));

        mockMvc.perform(multipart("/api/v1/kudos")
                        .file(new MockMultipartFile("adjuntos", "evidencia.png", "image/png", PNG))
                        .param("destinatarioId", DESTINATARIO.toString())
                        .param("categoriaId", CATEGORIA.toString())
                        .param("contenido", "Gracias por tu apoyo")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString()))))
                .andExpect(status().isCreated());

        ArgumentCaptor<PublicarKudoComando> comando = ArgumentCaptor.forClass(PublicarKudoComando.class);
        verify(publicarKudo).publicar(comando.capture());
        assertThat(comando.getValue().adjuntos()).singleElement().satisfies(adjunto -> {
            assertThat(adjunto.nombreOriginal()).isEqualTo("evidencia.png");
            assertThat(adjunto.contenido()).isEqualTo(PNG);
        });
    }

    @Test
    void rechazaPeticionSinToken() throws Exception {
        mockMvc.perform(post("/api/v1/kudos").contentType(MediaType.APPLICATION_JSON).content(cuerpoJson()))
                .andExpect(status().isUnauthorized());

        verify(publicarKudo, never()).publicar(any());
    }

    @Test
    void rechazaTokenConIdentificadorInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/kudos")
                        .with(jwt().jwt(token -> token.subject("no-es-un-uuid")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void respondeCuatroCientosCuandoFaltaUnCampo() throws Exception {
        when(publicarKudo.publicar(any())).thenThrow(new CampoObligatorioExcepcion("contenido"));

        mockMvc.perform(post("/api/v1/kudos")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPO_OBLIGATORIO"))
                .andExpect(jsonPath("$.campo").value("contenido"));
    }

    @Test
    void respondeCuatroVeintidosCuandoSeIncumpleUnaRegla() throws Exception {
        when(publicarKudo.publicar(any())).thenThrow(new ReglaNegocioExcepcion("AUTO_RECONOCIMIENTO",
                "destinatario", "No es posible publicar un Kudo para uno mismo"));

        mockMvc.perform(post("/api/v1/kudos")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("AUTO_RECONOCIMIENTO"))
                .andExpect(jsonPath("$.campo").value("destinatario"))
                .andExpect(jsonPath("$.detail").value("No es posible publicar un Kudo para uno mismo"));
    }

    @Test
    void respondeCuatroCientosCuatroCuandoNoExisteElDestinatario() throws Exception {
        when(publicarKudo.publicar(any()))
                .thenThrow(new RecursoNoEncontradoExcepcion("destinatario", DESTINATARIO.toString()));

        mockMvc.perform(post("/api/v1/kudos")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.campo").value("destinatario"));
    }

    @Test
    void respondeCuatroCientosCuandoElIdentificadorNoEsUnUuid() throws Exception {
        mockMvc.perform(multipart("/api/v1/kudos")
                        .param("destinatarioId", "abc")
                        .param("categoriaId", CATEGORIA.toString())
                        .param("contenido", "Hola")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString()))))
                .andExpect(status().isBadRequest());

        verify(publicarKudo, never()).publicar(any());
    }

    @Test
    void ocultaLosDetallesDeErroresInesperados() throws Exception {
        when(publicarKudo.publicar(any())).thenThrow(new IllegalStateException("contraseña de base de datos"));

        mockMvc.perform(post("/api/v1/kudos")
                        .with(jwt().jwt(token -> token.subject(AUTOR.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado, intenta de nuevo más tarde"));
    }

    private static String cuerpoJson() {
        return """
                {"destinatarioId": "%s", "categoriaId": "%s", "contenido": "Gracias por tu apoyo"}
                """.formatted(DESTINATARIO, CATEGORIA);
    }
}
