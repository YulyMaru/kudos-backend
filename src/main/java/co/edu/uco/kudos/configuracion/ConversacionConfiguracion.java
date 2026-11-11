package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.CalificarContenidoCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarReplicasCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarReportesCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.PublicarReplicaCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ReportarContenidoCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.VetarContenidoCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CalificarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReplicasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReportesPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarReplicaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ReportarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VetarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasConversacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCalificaciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioModeracion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioReplicas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConversacionConfiguracion {

    @Bean
    PublicarReplicaPuerto publicarReplica(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                          RepositorioReplicas replicas, RegistroAuditoria auditoria,
                                          GestorTransacciones transacciones, Clock reloj) {
        return new PublicarReplicaCasoUso(usuarios, contenidos, replicas, auditoria, transacciones, reloj);
    }

    @Bean
    ConsultarReplicasPuerto consultarReplicas(ConsultasConversacion consultas, ContenidosModerables contenidos) {
        return new ConsultarReplicasCasoUso(consultas, contenidos);
    }

    @Bean
    CalificarContenidoPuerto calificarContenido(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                                RepositorioCalificaciones calificaciones, Clock reloj) {
        return new CalificarContenidoCasoUso(usuarios, contenidos, calificaciones, reloj);
    }

    @Bean
    ReportarContenidoPuerto reportarContenido(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                              RepositorioModeracion moderacion, RegistroAuditoria auditoria,
                                              GestorTransacciones transacciones, Clock reloj) {
        return new ReportarContenidoCasoUso(usuarios, contenidos, moderacion, auditoria, transacciones, reloj);
    }

    @Bean
    VetarContenidoPuerto vetarContenido(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                        RepositorioModeracion moderacion, RegistroAuditoria auditoria,
                                        GestorTransacciones transacciones, Clock reloj) {
        return new VetarContenidoCasoUso(usuarios, contenidos, moderacion, auditoria, transacciones, reloj);
    }

    @Bean
    ConsultarReportesPuerto consultarReportes(RepositorioUsuarios usuarios, ConsultasConversacion consultas) {
        return new ConsultarReportesCasoUso(usuarios, consultas);
    }
}
