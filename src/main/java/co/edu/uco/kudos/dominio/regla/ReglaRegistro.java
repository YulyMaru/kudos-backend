package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.Rol;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ReglaRegistro {

    private static final Pattern CELULAR_E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]++@([^@\\s]++)$");

    private ReglaRegistro() {
    }

    public static String normalizarCorreoInstitucional(String correo, List<String> dominiosPermitidos) {
        if (correo == null || correo.isBlank()) {
            throw new CampoObligatorioExcepcion("correo");
        }
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        var coincidencia = CORREO.matcher(normalizado);
        if (!coincidencia.matches() || !tienePuntoInterno(coincidencia.group(1))) {
            throw new ReglaNegocioExcepcion("CORREO_INVALIDO", "correo", "El correo no tiene un formato válido");
        }
        String dominio = coincidencia.group(1);
        boolean institucional = dominiosPermitidos.stream()
                .map(permitido -> permitido.trim().toLowerCase(Locale.ROOT))
                .anyMatch(permitido -> dominio.equals(permitido) || dominio.endsWith("." + permitido));
        if (!institucional) {
            throw new ReglaNegocioExcepcion("CORREO_NO_INSTITUCIONAL", "correo",
                    "Se requiere un correo institucional (" + String.join(", ", dominiosPermitidos) + ")");
        }
        return normalizado;
    }

    public static boolean esCorreoValido(String correo) {
        if (correo == null) {
            return false;
        }
        var coincidencia = CORREO.matcher(correo);
        return coincidencia.matches() && tienePuntoInterno(coincidencia.group(1));
    }

    private static boolean tienePuntoInterno(String dominio) {
        int punto = dominio.indexOf('.', 1);
        return punto > 0 && punto < dominio.length() - 1;
    }

    public static String normalizarCelular(String celular) {
        if (celular == null || celular.isBlank()) {
            throw new CampoObligatorioExcepcion("celular");
        }
        String normalizado = celular.replaceAll("[\\s-]", "");
        if (!CELULAR_E164.matcher(normalizado).matches()) {
            throw new ReglaNegocioExcepcion("CELULAR_INVALIDO", "celular",
                    "El celular debe estar en formato internacional, por ejemplo +573001234567");
        }
        return normalizado;
    }

    public static void validarAlcance(Rol rol, UUID facultadId, UUID programaId) {
        if (rol == null) {
            throw new CampoObligatorioExcepcion("rol");
        }
        boolean requierePrograma = rol == Rol.ESTUDIANTE || rol == Rol.DOCENTE || rol == Rol.COORDINADOR_PROGRAMA;
        if (requierePrograma && programaId == null) {
            throw new CampoObligatorioExcepcion("programaId");
        }
        if (rol == Rol.COORDINADOR_FACULTAD && facultadId == null) {
            throw new CampoObligatorioExcepcion("facultadId");
        }
    }
}
