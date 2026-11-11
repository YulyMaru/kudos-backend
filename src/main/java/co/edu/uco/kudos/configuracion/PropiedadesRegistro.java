package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "kudos.registro")
public record PropiedadesRegistro(List<String> dominiosInstitucionales, Duration vigenciaCodigos,
                                  Duration vigenciaRestablecimiento) {

    public PropiedadesRegistro {
        dominiosInstitucionales = dominiosInstitucionales == null || dominiosInstitucionales.isEmpty()
                ? List.of("uco.edu.co") : List.copyOf(dominiosInstitucionales);
        vigenciaCodigos = vigenciaCodigos == null ? Duration.ofMinutes(15) : vigenciaCodigos;
        vigenciaRestablecimiento = vigenciaRestablecimiento == null ? Duration.ofMinutes(30) : vigenciaRestablecimiento;
    }
}
