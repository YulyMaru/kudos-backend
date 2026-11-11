package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.infraestructura.salida.auditoria.RegistroAuditoriaAdaptador;
import co.edu.uco.kudos.infraestructura.salida.auditoria.RegistroAuditoriaConMetricas;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ObservabilidadConfiguracion {

    @Bean
    @Primary
    RegistroAuditoria registroAuditoriaConMetricas(RegistroAuditoriaAdaptador registroBase, MeterRegistry registro) {
        return new RegistroAuditoriaConMetricas(registroBase, registro);
    }
}
