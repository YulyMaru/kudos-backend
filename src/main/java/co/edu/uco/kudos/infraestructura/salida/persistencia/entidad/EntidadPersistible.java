package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@MappedSuperclass
public abstract class EntidadPersistible implements Persistable<UUID> {

    @Transient
    private boolean nueva = true;

    @Override
    public boolean isNew() {
        return nueva;
    }

    @PostLoad
    @PostPersist
    void marcarPersistida() {
        nueva = false;
    }
}
