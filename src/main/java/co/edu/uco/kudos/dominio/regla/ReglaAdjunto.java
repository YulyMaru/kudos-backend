package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;

import java.util.Map;

public final class ReglaAdjunto {

    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    public static final int MAXIMO_ADJUNTOS = 5;

    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "application/pdf", "pdf");

    private ReglaAdjunto() {
    }

    public static void validarCantidad(int cantidad) {
        if (cantidad > MAXIMO_ADJUNTOS) {
            throw new ReglaNegocioExcepcion("DEMASIADOS_ADJUNTOS", "adjuntos",
                    "Se permiten como máximo " + MAXIMO_ADJUNTOS + " adjuntos por Kudo");
        }
    }

    public static void validarArchivo(String nombreOriginal, String tipoDetectado, long tamanoBytes) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new CampoObligatorioExcepcion("adjuntos.nombre");
        }
        if (tamanoBytes <= 0) {
            throw new ReglaNegocioExcepcion("ADJUNTO_VACIO", "adjuntos",
                    "El archivo " + nombreOriginal + " está vacío");
        }
        if (tamanoBytes > TAMANO_MAXIMO_BYTES) {
            throw new ReglaNegocioExcepcion("ADJUNTO_DEMASIADO_GRANDE", "adjuntos",
                    "El archivo " + nombreOriginal + " supera el tamaño máximo de 5 MB");
        }
        if (tipoDetectado == null || !TIPOS_PERMITIDOS.containsKey(tipoDetectado)) {
            throw new ReglaNegocioExcepcion("TIPO_ADJUNTO_NO_PERMITIDO", "adjuntos",
                    "El archivo " + nombreOriginal + " no es de un tipo permitido (JPG, PNG o PDF)");
        }
    }

    public static String extension(String tipoContenido) {
        String extension = TIPOS_PERMITIDOS.get(tipoContenido);
        if (extension == null) {
            throw new ReglaNegocioExcepcion("TIPO_ADJUNTO_NO_PERMITIDO", "adjuntos",
                    "Tipo de archivo no permitido");
        }
        return extension;
    }
}
