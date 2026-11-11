package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.CerrarSesionCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.CrearAdministradorInicialCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CerrarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.aplicacion.casouso.IniciarSesionCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.RefrescarSesionCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CrearAdministradorInicialPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.IniciarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RefrescarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.regla.PoliticaBloqueo;
import co.edu.uco.kudos.infraestructura.salida.seguridad.CodificadorContrasenasBCrypt;
import co.edu.uco.kudos.infraestructura.salida.seguridad.EmisorTokensJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;

@Configuration
public class AutenticacionConfiguracion {

    @Bean
    CodificadorContrasenas codificadorContrasenas(PropiedadesSeguridad propiedades) {
        return new CodificadorContrasenasBCrypt(propiedades.costoBcrypt());
    }

    @Bean
    EmisorTokens emisorTokens(JwtEncoder codificadorJwt, PropiedadesSeguridad propiedades) {
        return new EmisorTokensJwt(codificadorJwt, propiedades.emisorJwt(), propiedades.duracionTokenAcceso());
    }

    @Bean
    PoliticaBloqueo politicaBloqueo(PropiedadesSeguridad propiedades) {
        return new PoliticaBloqueo(propiedades.maximoIntentosFallidos(), propiedades.duracionBloqueo());
    }

    @Bean
    IniciarSesionPuerto iniciarSesion(RepositorioCuentas cuentas, RepositorioSolicitudes solicitudes,
                                      RepositorioTokensRefresco tokens, CodificadorContrasenas codificador,
                                      EmisorTokens emisor, RegistroAuditoria auditoria,
                                      GestorTransacciones transacciones, Clock reloj, PoliticaBloqueo politica,
                                      PropiedadesSeguridad propiedades) {
        return new IniciarSesionCasoUso(cuentas, solicitudes, tokens, codificador, emisor, auditoria, transacciones,
                reloj, politica, propiedades.duracionTokenRefresco());
    }

    @Bean
    CerrarSesionPuerto cerrarSesion(RepositorioTokensRefresco tokens, RegistroAuditoria auditoria,
                                    GestorTransacciones transacciones, Clock reloj) {
        return new CerrarSesionCasoUso(tokens, auditoria, transacciones, reloj);
    }

    @Bean
    RefrescarSesionPuerto refrescarSesion(RepositorioTokensRefresco tokens, RepositorioUsuarios usuarios,
                                          EmisorTokens emisor, RegistroAuditoria auditoria,
                                          GestorTransacciones transacciones, Clock reloj,
                                          PropiedadesSeguridad propiedades) {
        return new RefrescarSesionCasoUso(tokens, usuarios, emisor, auditoria, transacciones, reloj,
                propiedades.duracionTokenRefresco());
    }

    @Bean
    CrearAdministradorInicialPuerto crearAdministradorInicial(RepositorioCuentas cuentas,
                                                              CodificadorContrasenas codificador,
                                                              RegistroAuditoria auditoria,
                                                              GestorTransacciones transacciones, Clock reloj) {
        return new CrearAdministradorInicialCasoUso(cuentas, codificador, auditoria, transacciones, reloj);
    }
}
