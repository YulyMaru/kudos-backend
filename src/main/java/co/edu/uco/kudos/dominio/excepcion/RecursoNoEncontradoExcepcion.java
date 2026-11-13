package co.edu.uco.kudos.dominio.excepcion;

public class RecursoNoEncontradoExcepcion extends RuntimeException {

    private final String recurso;

    public RecursoNoEncontradoExcepcion(String recurso, String identificador) {
        super("No existe " + recurso + " con identificador " + identificador);
        this.recurso = recurso;
    }

    public String getRecurso() {
        return recurso;
    }
}
