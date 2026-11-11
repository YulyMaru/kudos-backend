package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.PublicarReplicaComando;
import co.edu.uco.kudos.aplicacion.dto.ReplicaPublicadaResultado;

public interface PublicarReplicaPuerto {

    ReplicaPublicadaResultado publicar(PublicarReplicaComando comando);
}
