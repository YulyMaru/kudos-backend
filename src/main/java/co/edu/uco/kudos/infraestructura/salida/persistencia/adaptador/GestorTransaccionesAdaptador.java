package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class GestorTransaccionesAdaptador implements GestorTransacciones {

    private final TransactionTemplate plantilla;

    public GestorTransaccionesAdaptador(PlatformTransactionManager gestor) {
        this.plantilla = new TransactionTemplate(gestor);
    }

    @Override
    public void enTransaccion(Runnable operacion) {
        plantilla.executeWithoutResult(estado -> operacion.run());
    }
}
