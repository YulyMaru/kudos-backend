package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.aplicacion.dto.TokenAcceso;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Instant;

public interface EmisorTokens {

    TokenAcceso emitir(Usuario usuario, Instant ahora);
}
