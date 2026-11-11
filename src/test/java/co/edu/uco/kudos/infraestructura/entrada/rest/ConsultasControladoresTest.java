package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleVista;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;
import co.edu.uco.kudos.aplicacion.dto.PaginaFeed;
import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.BuscarUsuariosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarCategoriasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarFeedPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarKudoPuerto;
import co.edu.uco.kudos.configuracion.SeguridadConfiguracion;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {KudoConsultaControlador.class, CategoriaControlador.class, UsuarioControlador.class},
        properties = "kudos.seguridad.jwt-secreto=secreto-de-pruebas-con-al-menos-32-bytes")
@Import(SeguridadConfiguracion.class)
class ConsultasControladoresTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final PersonaVista ANA = new PersonaVista(UUID.randomUUID(), "Ana", Rol.DOCENTE);
    private static final PersonaVista DIEGO = new PersonaVista(UUID.randomUUID(), "Diego", Rol.ESTUDIANTE);
    private static final CategoriaVista LIDERAZGO = new CategoriaVista(UUID.randomUUID(), "Liderazgo");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarFeedPuerto consultarFeed;
    @MockitoBean
    private ConsultarKudoPuerto consultarKudo;
    @MockitoBean
    private ConsultarCategoriasPuerto consultarCategorias;
    @MockitoBean
    private BuscarUsuariosPuerto buscarUsuarios;

    @Test
    void devuelveElFeedConElCursorDeLaSiguientePagina() throws Exception {
        KudoResumenVista kudo = new KudoResumenVista(UUID.randomUUID(), ANA, DIEGO, LIDERAZGO, "Gracias", true,
                Instant.parse("2026-11-11T10:00:00Z"), 1, 2, 4.5, 3);
        when(consultarFeed.consultar("abc", 10)).thenReturn(new PaginaFeed(List.of(kudo), "siguiente"));

        mockMvc.perform(get("/api/v1/kudos").param("cursor", "abc").param("limite", "10").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kudos[0].autor.nombreCompleto").value("Ana"))
                .andExpect(jsonPath("$.kudos[0].categoria.nombre").value("Liderazgo"))
                .andExpect(jsonPath("$.kudos[0].contenidoTruncado").value(true))
                .andExpect(jsonPath("$.kudos[0].cantidadReplicas").value(2))
                .andExpect(jsonPath("$.kudos[0].promedioCalificacion").value(4.5))
                .andExpect(jsonPath("$.siguienteCursor").value("siguiente"));
    }

    @Test
    void unCursorInvalidoRespondeCuatroCientos() throws Exception {
        when(consultarFeed.consultar(any(), any())).thenThrow(new ParametroInvalidoExcepcion("cursor", "inválido"));

        mockMvc.perform(get("/api/v1/kudos").param("cursor", "xyz").with(token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cursor"));
    }

    @Test
    void devuelveElDetalleDeUnKudoConSusAdjuntos() throws Exception {
        UUID id = UUID.randomUUID();
        UUID adjuntoId = UUID.randomUUID();
        when(consultarKudo.consultar(id)).thenReturn(new KudoDetalleVista(id, ANA, DIEGO, LIDERAZGO, "Texto completo",
                Instant.parse("2026-11-11T10:00:00Z"),
                List.of(new KudoDetalleVista.AdjuntoVista(adjuntoId, "evidencia.pdf", "application/pdf", 2048)),
                0, 0, 0));

        mockMvc.perform(get("/api/v1/kudos/{id}", id).with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").value("Texto completo"))
                .andExpect(jsonPath("$.adjuntos[0].id").value(adjuntoId.toString()))
                .andExpect(jsonPath("$.adjuntos[0].claveObjeto").doesNotExist());
    }

    @Test
    void elDetalleDeUnKudoInexistenteRespondeCuatroCientosCuatro() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarKudo.consultar(id)).thenThrow(new RecursoNoEncontradoExcepcion("kudo", id.toString()));

        mockMvc.perform(get("/api/v1/kudos/{id}", id).with(token())).andExpect(status().isNotFound());
    }

    @Test
    void devuelveLasCategoriasActivas() throws Exception {
        when(consultarCategorias.consultarActivas()).thenReturn(List.of(LIDERAZGO));

        mockMvc.perform(get("/api/v1/categorias").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Liderazgo"));
    }

    @Test
    void buscaUsuariosExcluyendoAlSolicitanteDelToken() throws Exception {
        when(buscarUsuarios.buscar("die", null, USUARIO)).thenReturn(List.of(DIEGO));

        mockMvc.perform(get("/api/v1/usuarios").param("busqueda", "die").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCompleto").value("Diego"))
                .andExpect(jsonPath("$[0].correo").doesNotExist());

        verify(buscarUsuarios).buscar(eq("die"), eq(null), eq(USUARIO));
    }

    @Test
    void lasConsultasExigenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/kudos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/categorias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios").param("busqueda", "ana")).andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject(USUARIO.toString()));
    }
}
