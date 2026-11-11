package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.dto.UsuarioDemostracionComando;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "kudos.demostracion")
public record PropiedadesDemostracion(boolean habilitada, List<UsuarioDemostracion> usuarios) {

    public PropiedadesDemostracion {
        usuarios = usuarios == null ? List.of() : List.copyOf(usuarios);
    }

    public List<UsuarioDemostracionComando> comandos() {
        return usuarios.stream()
                .map(usuario -> new UsuarioDemostracionComando(usuario.nombre(), usuario.correo(), usuario.celular(),
                        usuario.rol(), usuario.contrasena()))
                .toList();
    }

    public record UsuarioDemostracion(String nombre, String correo, String celular, Rol rol, String contrasena) {

        public UsuarioDemostracion {
            rol = rol == null ? Rol.ESTUDIANTE : rol;
        }

        @Override
        public String toString() {
            return "UsuarioDemostracion[correo=" + correo + ", rol=" + rol + "]";
        }
    }
}
