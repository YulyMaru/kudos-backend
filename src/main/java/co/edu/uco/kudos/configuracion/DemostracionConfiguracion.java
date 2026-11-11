package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.CrearUsuariosDemostracionCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CrearUsuariosDemostracionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.infraestructura.entrada.arranque.InicializadorDemostracion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(PropiedadesDemostracion.class)
@ConditionalOnProperty(name = "kudos.demostracion.habilitada", havingValue = "true")
public class DemostracionConfiguracion {

    @Bean
    CrearUsuariosDemostracionPuerto crearUsuariosDemostracion(RepositorioCuentas cuentas,
                                                              CodificadorContrasenas codificador,
                                                              RegistroAuditoria auditoria,
                                                              GestorTransacciones transacciones, Clock reloj) {
        return new CrearUsuariosDemostracionCasoUso(cuentas, codificador, auditoria, transacciones, reloj);
    }

    @Bean
    InicializadorDemostracion inicializadorDemostracion(CrearUsuariosDemostracionPuerto crearUsuarios,
                                                        PropiedadesDemostracion propiedades) {
        return new InicializadorDemostracion(crearUsuarios, propiedades.comandos());
    }
}
