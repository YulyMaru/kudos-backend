package co.edu.uco.kudos.aplicacion.dto;

import co.edu.uco.kudos.dominio.modelo.Rol;

public record UsuarioDemostracionComando(String nombreCompleto, String correo, String celular, Rol rol,
                                         String contrasena) {

    @Override
    public String toString() {
        return "UsuarioDemostracionComando[correo=" + correo + ", rol=" + rol + "]";
    }
}
