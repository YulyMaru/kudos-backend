package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEstructura {

    Optional<Facultad> facultad(UUID id);

    Optional<Programa> programa(UUID id);

    boolean existeFacultad(String nombre);

    boolean existePrograma(UUID facultadId, String nombre);

    void guardarFacultad(Facultad facultad);

    void guardarPrograma(Programa programa);

    List<Facultad> facultadesActivas();

    List<Programa> programasActivos(UUID facultadId);
}
