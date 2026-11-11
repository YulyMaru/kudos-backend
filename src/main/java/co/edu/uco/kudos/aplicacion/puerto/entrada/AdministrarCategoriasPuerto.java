package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;

import java.util.UUID;

public interface AdministrarCategoriasPuerto {

    CategoriaVista crear(UUID actorId, String nombre);

    CategoriaVista renombrar(UUID actorId, UUID categoriaId, String nombre);

    CategoriaVista cambiarEstado(UUID actorId, UUID categoriaId, boolean activa);
}
