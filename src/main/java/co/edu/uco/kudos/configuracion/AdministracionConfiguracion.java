package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.AdministrarCategoriasCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.AdministrarEstructuraCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarAuditoriaCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.GenerarEnlaceAdjuntoCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.GestionarVinculosCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.LimpiarDatosCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarCategoriasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarEstructuraPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarAuditoriaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GenerarEnlaceAdjuntoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GestionarVinculosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.LimpiarDatosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.EnlacesArchivos;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.MantenimientoDatos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategoriasAdministracion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(PropiedadesLimpieza.class)
public class AdministracionConfiguracion {

    @Bean
    AdministrarCategoriasPuerto administrarCategorias(RepositorioUsuarios usuarios,
                                                      RepositorioCategoriasAdministracion categorias,
                                                      RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                                      Clock reloj) {
        return new AdministrarCategoriasCasoUso(usuarios, categorias, auditoria, transacciones, reloj);
    }

    @Bean
    ConsultarAuditoriaPuerto consultarAuditoria(ConsultasAuditoria consultas) {
        return new ConsultarAuditoriaCasoUso(consultas);
    }

    @Bean
    AdministrarEstructuraPuerto administrarEstructura(RepositorioUsuarios usuarios, RepositorioEstructura estructura,
                                                      RegistroAuditoria auditoria, GestorTransacciones transacciones,
                                                      Clock reloj) {
        return new AdministrarEstructuraCasoUso(usuarios, estructura, auditoria, transacciones, reloj);
    }

    @Bean
    GestionarVinculosPuerto gestionarVinculos(RepositorioUsuarios usuarios, RepositorioEstructura estructura,
                                              RepositorioVinculos vinculos, RegistroAuditoria auditoria,
                                              GestorTransacciones transacciones, Clock reloj) {
        return new GestionarVinculosCasoUso(usuarios, estructura, vinculos, auditoria, transacciones, reloj);
    }

    @Bean
    LimpiarDatosPuerto limpiarDatos(MantenimientoDatos mantenimiento, Clock reloj, PropiedadesLimpieza propiedades) {
        return new LimpiarDatosCasoUso(mantenimiento, reloj, propiedades.retencionNotificaciones(),
                propiedades.retencionCredenciales());
    }

    @Bean
    GenerarEnlaceAdjuntoPuerto generarEnlaceAdjunto(RepositorioUsuarios usuarios, ConsultasKudos consultas,
                                                    EnlacesArchivos enlaces, RegistroAuditoria auditoria, Clock reloj,
                                                    PropiedadesAlmacenamiento propiedades) {
        return new GenerarEnlaceAdjuntoCasoUso(usuarios, consultas, enlaces, auditoria, reloj,
                propiedades.vigenciaEnlaces());
    }
}
