package co.edu.uco.kudos.infraestructura.salida.seguridad;

import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CodificadorContrasenasBCrypt implements CodificadorContrasenas {

    private final BCryptPasswordEncoder codificador;

    public CodificadorContrasenasBCrypt(int costo) {
        this.codificador = new BCryptPasswordEncoder(costo);
    }

    @Override
    public String codificar(String contrasena) {
        return codificador.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        if (contrasena == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return codificador.matches(contrasena, hash);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}
