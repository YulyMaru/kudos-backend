package co.edu.uco.kudos.infraestructura.salida.auditoria;

import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.AuditoriaMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.RegistroAuditoriaRepositorioJpa;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RegistroAuditoriaAdaptador implements RegistroAuditoria {

    private final RegistroAuditoriaRepositorioJpa repositorio;

    public RegistroAuditoriaAdaptador(RegistroAuditoriaRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void registrar(EntradaAuditoria entrada) {
        repositorio.save(AuditoriaMapeador.aEntidad(entrada).conContexto(ipActual(), MDC.get("traceId")));
    }

    private static String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRemoteAddr();
        }
        return null;
    }
}
