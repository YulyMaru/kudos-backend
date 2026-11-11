package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.dto.EnlaceAdjunto;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;
import co.edu.uco.kudos.aplicacion.dto.ReplicaPublicadaResultado;
import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;
import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarCategoriasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarEstructuraPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CalificarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CerrarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarAuditoriaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReplicasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReportesPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.DecidirSolicitudPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GenerarEnlaceAdjuntoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GestionarVinculosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarReplicaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RegistrarUsuarioPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ReportarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RestablecerContrasenaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VerificarContactoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VetarContenidoPuerto;
import co.edu.uco.kudos.configuracion.SeguridadConfiguracion;
import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RegistroControlador.class, CuentaControlador.class, SolicitudControlador.class,
        EstructuraControlador.class, VinculoControlador.class, ConversacionControlador.class,
        CalificacionControlador.class, ModeracionControlador.class, CategoriaAdministracionControlador.class,
        AuditoriaControlador.class, AdjuntoControlador.class},
        properties = "kudos.seguridad.jwt-secreto=secreto-de-pruebas-con-al-menos-32-bytes")
@Import(SeguridadConfiguracion.class)
class FuncionalidadesControladoresTest {

    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarUsuarioPuerto registrarUsuario;
    @MockitoBean
    private VerificarContactoPuerto verificarContacto;
    @MockitoBean
    private CerrarSesionPuerto cerrarSesion;
    @MockitoBean
    private RestablecerContrasenaPuerto restablecerContrasena;
    @MockitoBean
    private DecidirSolicitudPuerto decidirSolicitud;
    @MockitoBean
    private AdministrarEstructuraPuerto estructura;
    @MockitoBean
    private GestionarVinculosPuerto vinculos;
    @MockitoBean
    private PublicarReplicaPuerto publicarReplica;
    @MockitoBean
    private ConsultarReplicasPuerto consultarReplicas;
    @MockitoBean
    private CalificarContenidoPuerto calificar;
    @MockitoBean
    private ReportarContenidoPuerto reportar;
    @MockitoBean
    private VetarContenidoPuerto vetar;
    @MockitoBean
    private ConsultarReportesPuerto consultarReportes;
    @MockitoBean
    private AdministrarCategoriasPuerto categorias;
    @MockitoBean
    private ConsultarAuditoriaPuerto auditoria;
    @MockitoBean
    private GenerarEnlaceAdjuntoPuerto enlaces;

    @Test
    void elRegistroLaVerificacionYElRestablecimientoSonPublicos() throws Exception {
        mockMvc.perform(post("/api/v1/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreCompleto":"Ana","correo":"ana@uco.edu.co","celular":"+573001112233",
                         "rol":"ESTUDIANTE","programaId":"%s","contrasena":"Clave-Segura-2026","aceptaPolitica":true}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/registro/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@uco.edu.co\",\"canal\":\"CELULAR\",\"codigo\":\"123456\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/restablecer/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@uco.edu.co\"}"))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/auth/cerrar-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenRefresco\":\"abc\"}"))
                .andExpect(status().isNoContent());

        verify(verificarContacto).verificar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR, "123456");
    }

    @Test
    void elCatalogoEsPublicoPeroCrearFacultadesExigeRol() throws Exception {
        Facultad ingenieria = Facultad.crear("Ingeniería");
        when(estructura.facultades()).thenReturn(List.of(ingenieria));
        when(estructura.crearFacultad(any(), eq("Ingeniería"))).thenReturn(ingenieria);

        mockMvc.perform(get("/api/v1/catalogo/facultades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ingeniería"));
        mockMvc.perform(post("/api/v1/facultades").with(como("ESTUDIANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ingeniería\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/facultades").with(como("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ingeniería\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void soloElRectorYElCoordinadorAcademicoVetanYVenReportes() throws Exception {
        String cuerpo = "{\"tipoContenido\":\"KUDO\",\"contenidoId\":\"%s\",\"motivo\":\"x\"}".formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/vetos").with(como("DOCENTE")).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/vetos").with(como("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/vetos").with(como("RECTOR")).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/reportes").with(como("ESTUDIANTE"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes").with(como("COORDINADOR_ACADEMICO"))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reportes").with(como("ESTUDIANTE")).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)).andExpect(status().isCreated());
    }

    @Test
    void laAuditoriaEsExclusivaDelAdministradorYAceptaFiltros() throws Exception {
        when(auditoria.consultar(any(), any(), any())).thenReturn(new PaginaAuditoria(List.of(), 0, 50, 0));

        mockMvc.perform(get("/api/v1/auditoria").with(como("RECTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/auditoria").with(como("ADMINISTRADOR"))
                        .param("accion", "KUDO_PUBLICADO").param("desde", "2026-11-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        verify(auditoria).consultar(org.mockito.ArgumentMatchers.argThat(filtro ->
                filtro.accion() == AccionAuditoria.KUDO_PUBLICADO
                        && Instant.parse("2026-11-01T00:00:00Z").equals(filtro.desde())), any(), any());
    }

    @Test
    void administrarCategoriasExigeUnRolDirectivo() throws Exception {
        when(categorias.crear(any(), eq("Resiliencia"))).thenReturn(new CategoriaVista(UUID.randomUUID(), "Resiliencia"));

        mockMvc.perform(post("/api/v1/categorias").with(como("ESTUDIANTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Resiliencia\"}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/categorias").with(como("COORDINADOR_ACADEMICO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Resiliencia\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Resiliencia"));
    }

    @Test
    void replicarYCalificarEstanDisponiblesParaUsuariosAutenticados() throws Exception {
        UUID kudoId = UUID.randomUUID();
        when(publicarReplica.publicar(any())).thenReturn(new ReplicaPublicadaResultado(UUID.randomUUID(), kudoId, null,
                Instant.parse("2026-11-11T10:00:00Z")));
        when(calificar.calificar(any())).thenReturn(new ResumenCalificacion(TipoContenido.KUDO, kudoId, 4.5, 2, 5));

        mockMvc.perform(post("/api/v1/kudos/{id}/replicas", kudoId).with(como("ESTUDIANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"¡Felicitaciones!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kudoId").value(kudoId.toString()));
        mockMvc.perform(put("/api/v1/calificaciones").with(como("ESTUDIANTE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoContenido\":\"KUDO\",\"contenidoId\":\"%s\",\"valor\":5}".formatted(kudoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(4.5))
                .andExpect(jsonPath("$.miCalificacion").value(5));
        mockMvc.perform(post("/api/v1/kudos/{id}/replicas", kudoId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contenido\":\"x\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void unaDecisionFueraDeAlcanceRespondeCuatroCientosTres() throws Exception {
        UUID solicitud = UUID.randomUUID();
        doThrow(new AccesoDenegadoExcepcion("No le compete decidir esta solicitud de registro"))
                .when(decidirSolicitud).aprobar(USUARIO, solicitud);

        mockMvc.perform(post("/api/v1/solicitudes/{id}/aprobar", solicitud).with(como("COORDINADOR_PROGRAMA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void elEnlaceDeUnAdjuntoNoSeGuardaEnCache() throws Exception {
        when(enlaces.generar(any(), any(), eq(USUARIO))).thenReturn(new EnlaceAdjunto("https://firmado",
                Instant.parse("2026-11-11T10:10:00Z")));

        mockMvc.perform(get("/api/v1/kudos/{k}/adjuntos/{a}/enlace", UUID.randomUUID(), UUID.randomUUID())
                        .with(como("DOCENTE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.url").value("https://firmado"));
    }

    private static RequestPostProcessor como(String rol) {
        return jwt().jwt(token -> token.subject(USUARIO.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
