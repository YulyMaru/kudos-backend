package co.edu.uco.kudos.aplicacion.dto;

import co.edu.uco.kudos.dominio.modelo.Rol;

import java.util.UUID;

public record PersonaVista(UUID id, String nombreCompleto, Rol rol) {
}
