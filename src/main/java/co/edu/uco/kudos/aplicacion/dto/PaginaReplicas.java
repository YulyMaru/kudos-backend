package co.edu.uco.kudos.aplicacion.dto;


import java.util.List;

public record PaginaReplicas(List<ReplicaVista> replicas, String siguienteCursor) {

    public PaginaReplicas {
        replicas = List.copyOf(replicas);
    }
}
