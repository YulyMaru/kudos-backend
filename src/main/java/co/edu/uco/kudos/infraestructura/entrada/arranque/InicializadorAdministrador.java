package co.edu.uco.kudos.infraestructura.entrada.arranque;

import co.edu.uco.kudos.aplicacion.puerto.entrada.CrearAdministradorInicialPuerto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorAdministrador.class);

    private final CrearAdministradorInicialPuerto crearAdministrador;
    private final String nombre;
    private final String correo;
    private final String contrasena;

    public InicializadorAdministrador(CrearAdministradorInicialPuerto crearAdministrador,
                                      @Value("${kudos.administrador-inicial.nombre:Administrador UCO Kudos}") String nombre,
                                      @Value("${kudos.administrador-inicial.correo:}") String correo,
                                      @Value("${kudos.administrador-inicial.contrasena:}") String contrasena) {
        this.crearAdministrador = crearAdministrador;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (correo.isBlank() || contrasena.isBlank()) {
            LOG.info("Administrador inicial no configurado (ADMIN_CORREO / ADMIN_CONTRASENA); se omite su creación");
            return;
        }
        if (crearAdministrador.crearSiNoExiste(nombre, correo, contrasena)) {
            LOG.info("Se creó el administrador inicial de la plataforma");
        }
    }
}
