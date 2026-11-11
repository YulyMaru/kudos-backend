package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.DecidirSolicitudCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.RegistrarUsuarioCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.RestablecerContrasenaCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.VerificarContactoCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.DecidirSolicitudPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RegistrarUsuarioPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RestablecerContrasenaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VerificarContactoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCodigos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(PropiedadesRegistro.class)
public class RegistroConfiguracion {

    @Bean
    RegistrarUsuarioPuerto registrarUsuario(RepositorioCuentas cuentas, RepositorioSolicitudes solicitudes,
                                            RepositorioCodigos codigos, RepositorioEstructura estructura,
                                            RepositorioOutbox outbox, CodificadorContrasenas codificador,
                                            RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                            Clock reloj, PropiedadesRegistro propiedades) {
        return new RegistrarUsuarioCasoUso(cuentas, solicitudes, codigos, estructura, outbox, codificador, auditoria,
                transacciones, reloj, propiedades.dominiosInstitucionales(), propiedades.vigenciaCodigos());
    }

    @Bean
    VerificarContactoPuerto verificarContacto(RepositorioCuentas cuentas, RepositorioCodigos codigos,
                                              RepositorioSolicitudes solicitudes, RepositorioVinculos vinculos,
                                              RepositorioOutbox outbox, RegistroAuditoria auditoria,
                                              GestorTransacciones transacciones, Clock reloj,
                                              PropiedadesRegistro propiedades) {
        return new VerificarContactoCasoUso(cuentas, codigos, solicitudes, vinculos, outbox, auditoria, transacciones,
                reloj, propiedades.vigenciaCodigos());
    }

    @Bean
    DecidirSolicitudPuerto decidirSolicitud(RepositorioSolicitudes solicitudes, RepositorioUsuarios usuarios,
                                            RepositorioCuentas cuentas, RepositorioEstructura estructura,
                                            RepositorioVinculos vinculos, RepositorioOutbox outbox,
                                            RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                            Clock reloj) {
        return new DecidirSolicitudCasoUso(solicitudes, usuarios, cuentas, estructura, vinculos, outbox, auditoria,
                transacciones, reloj);
    }

    @Bean
    RestablecerContrasenaPuerto restablecerContrasena(RepositorioCuentas cuentas, RepositorioUsuarios usuarios,
                                                      RepositorioCodigos codigos, RepositorioTokensRefresco tokens,
                                                      RepositorioOutbox outbox, CodificadorContrasenas codificador,
                                                      RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                                      Clock reloj, PropiedadesRegistro propiedades) {
        return new RestablecerContrasenaCasoUso(cuentas, usuarios, codigos, tokens, outbox, codificador, auditoria,
                transacciones, reloj, propiedades.vigenciaRestablecimiento());
    }
}
