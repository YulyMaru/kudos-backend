package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.PublicarKudoCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarKudoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;
import co.edu.uco.kudos.aplicacion.puerto.salida.DetectorTipoArchivo;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategorias;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CasosUsoConfiguracion {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    PublicarKudoPuerto publicarKudo(RepositorioUsuarios usuarios, RepositorioCategorias categorias,
                                    RepositorioKudos kudos, RepositorioOutbox outbox, RegistroAuditoria auditoria,
                                    AlmacenamientoArchivos almacenamiento, DetectorTipoArchivo detectorTipo,
                                    GestorTransacciones transacciones, Clock reloj) {
        return new PublicarKudoCasoUso(usuarios, categorias, kudos, outbox, auditoria, almacenamiento,
                detectorTipo, transacciones, reloj);
    }
}
