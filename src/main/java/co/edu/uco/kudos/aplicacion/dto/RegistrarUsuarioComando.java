package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.Rol;

import java.util.UUID;

public record RegistrarUsuarioComando(String nombreCompleto, String correo, String celular, Rol rol, UUID facultadId,
                                      UUID programaId, String contrasena, boolean aceptaPolitica) {

    @Override
    public String toString() {
        return "RegistrarUsuarioComando[correo=" + correo + ", rol=" + rol + "]";
    }
}
