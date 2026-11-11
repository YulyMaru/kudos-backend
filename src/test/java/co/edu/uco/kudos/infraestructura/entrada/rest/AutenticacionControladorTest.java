package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.IniciarSesionComando;
import co.edu.uco.kudos.aplicacion.dto.SesionResultado;
import co.edu.uco.kudos.aplicacion.puerto.entrada.IniciarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RefrescarSesionPuerto;
import co.edu.uco.kudos.configuracion.SeguridadConfiguracion;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaBloqueadaExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaNoActivaExcepcion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AutenticacionControlador.class,
        properties = "kudos.seguridad.jwt-secreto=secreto-de-pruebas-con-al-menos-32-bytes")
@Import(SeguridadConfiguracion.class)
class AutenticacionControladorTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final Instant EXPIRA = Instant.parse("2026-11-11T10:15:00Z");
    private static final SesionResultado SESION = new SesionResultado("jwt-acceso", EXPIRA, "token-refresco",
            Instant.parse("2026-11-18T10:00:00Z"), USUARIO, "Ana Docente", Rol.DOCENTE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IniciarSesionPuerto iniciarSesion;

    @MockitoBean
    private RefrescarSesionPuerto refrescarSesion;

    @Test
    void iniciaSesionSinNecesitarTokenYNoPermiteCachearLaRespuesta() throws Exception {
        when(iniciarSesion.iniciarSesion(any())).thenReturn(SESION);

        mockMvc.perform(post("/api/v1/auth/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"ana@uco.net.co\", \"contrasena\": \"Clave-Segura-2026\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenAcceso").value("jwt-acceso"))
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value("2026-11-11T10:15:00Z"))
                .andExpect(jsonPath("$.tokenRefresco").value("token-refresco"))
                .andExpect(jsonPath("$.usuarioId").value(USUARIO.toString()))
                .andExpect(jsonPath("$.rol").value("DOCENTE"));

        verify(iniciarSesion).iniciarSesion(eq(new IniciarSesionComando("ana@uco.net.co", "Clave-Segura-2026")));
    }

    @Test
    void respondeCuatroCientosUnoConMensajeGenericoSiLasCredencialesSonInvalidas() throws Exception {
        when(iniciarSesion.iniciarSesion(any())).thenThrow(new CredencialesInvalidasExcepcion());

        mockMvc.perform(post("/api/v1/auth/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"ana@uco.net.co\", \"contrasena\": \"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos"));
    }

    @Test
    void respondeCuatroVeintitresSiLaCuentaEstaBloqueada() throws Exception {
        when(iniciarSesion.iniciarSesion(any())).thenThrow(new CuentaBloqueadaExcepcion(EXPIRA));

        mockMvc.perform(post("/api/v1/auth/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"ana@uco.net.co\", \"contrasena\": \"mala\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"))
                .andExpect(jsonPath("$.bloqueadaHasta").value("2026-11-11T10:15:00Z"));
    }

    @Test
    void respondeCuatroCientosTresSiLaCuentaNoEstaActiva() throws Exception {
        when(iniciarSesion.iniciarSesion(any())).thenThrow(new CuentaNoActivaExcepcion(EstadoUsuario.PENDIENTE));

        mockMvc.perform(post("/api/v1/auth/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"ana@uco.net.co\", \"contrasena\": \"Clave-Segura-2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_ACTIVA"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void refrescaLaSesionSinNecesitarTokenDeAcceso() throws Exception {
        when(refrescarSesion.refrescar("token-refresco")).thenReturn(SESION);

        mockMvc.perform(post("/api/v1/auth/refrescar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenRefresco\": \"token-refresco\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").value("jwt-acceso"));
    }

    @Test
    void respondeCuatroCientosUnoSiElTokenDeRefrescoNoSirve() throws Exception {
        when(refrescarSesion.refrescar(any())).thenThrow(new CredencialesInvalidasExcepcion());

        mockMvc.perform(post("/api/v1/auth/refrescar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenRefresco\": \"robado\"}"))
                .andExpect(status().isUnauthorized());
    }
}
