package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioCuentas {

    Optional<Usuario> buscarPorCorreo(String correo);

    boolean existeConRol(Rol rol);

    boolean existePorCorreoOCelular(String correo, String celular);

    Optional<Usuario> buscarActivoConRol(Rol rol, UUID facultadId, UUID programaId);

    void guardar(Usuario usuario);
}
