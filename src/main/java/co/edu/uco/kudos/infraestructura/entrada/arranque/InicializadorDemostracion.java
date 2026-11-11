package co.edu.uco.kudos.infraestructura.entrada.arranque;

import co.edu.uco.kudos.aplicacion.dto.UsuarioDemostracionComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CrearUsuariosDemostracionPuerto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

public class InicializadorDemostracion implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorDemostracion.class);

    private final CrearUsuariosDemostracionPuerto crearUsuarios;
    private final List<UsuarioDemostracionComando> usuarios;

    public InicializadorDemostracion(CrearUsuariosDemostracionPuerto crearUsuarios,
                                     List<UsuarioDemostracionComando> usuarios) {
        this.crearUsuarios = crearUsuarios;
        this.usuarios = List.copyOf(usuarios);
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (usuarios.isEmpty()) {
            LOG.warn("La demostración está habilitada pero no se configuró ningún usuario");
            return;
        }
        int creados = crearUsuarios.crearFaltantes(usuarios);
        LOG.info("Usuarios de demostración configurados: {}, creados en este arranque: {}", usuarios.size(), creados);
    }
}
