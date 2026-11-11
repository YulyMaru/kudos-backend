package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.UsuarioDemostracionComando;

import java.util.List;

public interface CrearUsuariosDemostracionPuerto {

    int crearFaltantes(List<UsuarioDemostracionComando> usuarios);
}
