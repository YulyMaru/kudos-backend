package co.edu.uco.kudos.dominio.excepcion;

public class ReglaNegocioExcepcion extends RuntimeException {

    private final String codigo;
    private final String campo;

    public ReglaNegocioExcepcion(String codigo, String campo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.campo = campo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCampo() {
        return campo;
    }
}
