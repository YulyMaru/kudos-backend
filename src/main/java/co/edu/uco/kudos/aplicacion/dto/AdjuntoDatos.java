package co.edu.uco.kudos.aplicacion.dto;


import java.util.UUID;

public record AdjuntoDatos(UUID id, String nombreOriginal, String tipoContenido, long tamanoBytes, String claveObjeto) {
}
