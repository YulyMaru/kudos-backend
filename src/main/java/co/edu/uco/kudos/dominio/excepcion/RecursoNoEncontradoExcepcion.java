package co.edu.uco.kudos.dominio.excepcion;

public class RecursoNoEncontradoExcepcion extends RuntimeException {

    private final String recurso;
    private final String identificador;

    public RecursoNoEncontradoExcepcion(String recurso, String identificador) {
        super("No existe " + recurso + " con identificador " + identificador);
        this.recurso = recurso;
        this.identificador = identificador;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getIdentificador() {
        return identificador;
    }
}
