package co.edu.uco.kudos.infraestructura.entrada.excepcion;

import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaBloqueadaExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaNoActivaExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.UncheckedIOException;

@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(CampoObligatorioExcepcion.class)
    public ProblemDetail campoObligatorio(CampoObligatorioExcepcion error) {
        return problemaDeNegocio(HttpStatus.BAD_REQUEST, "Campo obligatorio", error);
    }

    @ExceptionHandler(ParametroInvalidoExcepcion.class)
    public ProblemDetail parametroInvalido(ParametroInvalidoExcepcion error) {
        return problemaDeNegocio(HttpStatus.BAD_REQUEST, "Parámetro inválido", error);
    }

    @ExceptionHandler(ReglaNegocioExcepcion.class)
    public ProblemDetail reglaNegocio(ReglaNegocioExcepcion error) {
        return problemaDeNegocio(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida", error);
    }

    @ExceptionHandler(RecursoNoEncontradoExcepcion.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoExcepcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
        problema.setTitle("Recurso no encontrado");
        problema.setProperty("codigo", "RECURSO_NO_ENCONTRADO");
        problema.setProperty("campo", error.getRecurso());
        return problema;
    }

    @ExceptionHandler(CredencialesInvalidasExcepcion.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasExcepcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
        problema.setTitle("Credenciales inválidas");
        problema.setProperty("codigo", "CREDENCIALES_INVALIDAS");
        return problema;
    }

    @ExceptionHandler(CuentaBloqueadaExcepcion.class)
    public ProblemDetail cuentaBloqueada(CuentaBloqueadaExcepcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, error.getMessage());
        problema.setTitle("Cuenta bloqueada temporalmente");
        problema.setProperty("codigo", "CUENTA_BLOQUEADA");
        problema.setProperty("bloqueadaHasta", error.getBloqueadaHasta());
        return problema;
    }

    @ExceptionHandler(CuentaNoActivaExcepcion.class)
    public ProblemDetail cuentaNoActiva(CuentaNoActivaExcepcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, error.getMessage());
        problema.setTitle("Cuenta no activa");
        problema.setProperty("codigo", "CUENTA_NO_ACTIVA");
        problema.setProperty("estado", error.getEstado().name());
        problema.setProperty("condicionesPendientes", error.getCondicionesPendientes());
        return problema;
    }

    @ExceptionHandler(AccesoDenegadoExcepcion.class)
    public ProblemDetail accesoDenegado(AccesoDenegadoExcepcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, error.getMessage());
        problema.setTitle("Acceso denegado");
        problema.setProperty("codigo", "ACCESO_DENEGADO");
        return problema;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflictoDeDatos(DataIntegrityViolationException error) {
        LOG.warn("Violación de integridad de datos: {}", error.getMostSpecificCause().getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes");
        problema.setTitle("Conflicto de datos");
        problema.setProperty("codigo", "CONFLICTO_DE_DATOS");
        return problema;
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ProblemDetail archivoIlegible(UncheckedIOException error) {
        LOG.warn("Error de lectura o escritura de archivo", error);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "No fue posible procesar el archivo adjunto");
        problema.setTitle("Archivo no procesable");
        problema.setProperty("codigo", "ARCHIVO_NO_PROCESABLE");
        problema.setProperty("campo", "adjuntos");
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception error) {
        LOG.error("Error inesperado", error);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado, intenta de nuevo más tarde");
        problema.setTitle("Error interno");
        problema.setProperty("codigo", "ERROR_INTERNO");
        return problema;
    }

    private static ProblemDetail problemaDeNegocio(HttpStatus estado, String titulo, ReglaNegocioExcepcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, error.getMessage());
        problema.setTitle(titulo);
        problema.setProperty("codigo", error.getCodigo());
        problema.setProperty("campo", error.getCampo());
        return problema;
    }
}
