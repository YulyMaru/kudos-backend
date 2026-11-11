package co.edu.uco.kudos.infraestructura.entrada.rest.dto;


import co.edu.uco.kudos.dominio.modelo.Rol;

import java.util.UUID;

public record RegistroSolicitud(String nombreCompleto, String correo, String celular, Rol rol, UUID facultadId,
                                UUID programaId, String contrasena, Boolean aceptaPolitica) {

    @Override
    public String toString() {
        return "RegistroSolicitud[correo=" + correo + ", rol=" + rol + "]";
    }
}
