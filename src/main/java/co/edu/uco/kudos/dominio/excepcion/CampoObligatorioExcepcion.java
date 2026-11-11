package co.edu.uco.kudos.dominio.excepcion;

public class CampoObligatorioExcepcion extends ReglaNegocioExcepcion {

    public CampoObligatorioExcepcion(String campo) {
        super("CAMPO_OBLIGATORIO", campo, "El campo " + campo + " es obligatorio");
    }
}
