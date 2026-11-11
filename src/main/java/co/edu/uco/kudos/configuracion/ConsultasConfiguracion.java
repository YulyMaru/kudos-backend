package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.BuscarUsuariosCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarCategoriasCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarFeedCasoUso;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarKudoCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.BuscarUsuariosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarCategoriasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarFeedPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarKudoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasCategorias;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasUsuarios;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class ConsultasConfiguracion {

    @Bean
    ConsultarFeedPuerto consultarFeed(ConsultasKudos consultas) {
        return new ConsultarFeedCasoUso(consultas);
    }

    @Bean
    ConsultarKudoPuerto consultarKudo(ConsultasKudos consultas) {
        return new ConsultarKudoCasoUso(consultas);
    }

    @Bean
    ConsultarCategoriasPuerto consultarCategorias(ConsultasCategorias consultas) {
        return new ConsultarCategoriasCasoUso(consultas);
    }

    @Bean
    BuscarUsuariosPuerto buscarUsuarios(ConsultasUsuarios consultas) {
        return new BuscarUsuariosCasoUso(consultas);
    }
}
