package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.ModerarComando;
import co.edu.uco.kudos.aplicacion.dto.ReporteVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReportesPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ReportarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VetarContenidoPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.ModeracionSolicitud;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ModeracionControlador {

    private final ReportarContenidoPuerto reportar;
    private final VetarContenidoPuerto vetar;
    private final ConsultarReportesPuerto consultarReportes;

    public ModeracionControlador(ReportarContenidoPuerto reportar, VetarContenidoPuerto vetar,
                                 ConsultarReportesPuerto consultarReportes) {
        this.reportar = reportar;
        this.vetar = vetar;
        this.consultarReportes = consultarReportes;
    }

    @PostMapping("/api/v1/reportes")
    public ResponseEntity<Void> reportar(@AuthenticationPrincipal Jwt jwt, @RequestBody ModeracionSolicitud solicitud) {
        reportar.reportar(comando(jwt, solicitud));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/v1/reportes")
    public List<ReporteVista> pendientes(@AuthenticationPrincipal Jwt jwt) {
        return consultarReportes.pendientes(UsuarioAutenticado.id(jwt));
    }

    @PostMapping("/api/v1/vetos")
    public ResponseEntity<Void> vetar(@AuthenticationPrincipal Jwt jwt, @RequestBody ModeracionSolicitud solicitud) {
        vetar.vetar(comando(jwt, solicitud));
        return ResponseEntity.noContent().build();
    }

    private static ModerarComando comando(Jwt jwt, ModeracionSolicitud solicitud) {
        return new ModerarComando(UsuarioAutenticado.id(jwt), solicitud.tipoContenido(), solicitud.contenidoId(),
                solicitud.motivo());
    }
}
