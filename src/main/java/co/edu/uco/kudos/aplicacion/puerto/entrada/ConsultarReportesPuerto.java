package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.ReporteVista;

import java.util.List;
import java.util.UUID;

public interface ConsultarReportesPuerto {

    List<ReporteVista> pendientes(UUID solicitanteId);
}
