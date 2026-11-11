package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.ReporteVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReportesPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasConversacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.regla.ReglaModeracion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarReportesCasoUso implements ConsultarReportesPuerto {

    private static final int LIMITE = 100;

    private final RepositorioUsuarios usuarios;
    private final ConsultasConversacion consultas;

    public ConsultarReportesCasoUso(RepositorioUsuarios usuarios, ConsultasConversacion consultas) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.consultas = Objects.requireNonNull(consultas);
    }

    @Override
    public List<ReporteVista> pendientes(UUID solicitanteId) {
        Actores.conRol(usuarios, solicitanteId, ReglaModeracion.ROLES_MODERADORES, "consultar los reportes");
        return consultas.reportesPendientes(LIMITE);
    }
}
