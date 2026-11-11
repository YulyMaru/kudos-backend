package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarCategoriasPuerto;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaControlador {

    private final ConsultarCategoriasPuerto consultarCategorias;

    public CategoriaControlador(ConsultarCategoriasPuerto consultarCategorias) {
        this.consultarCategorias = consultarCategorias;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaVista>> activas() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(consultarCategorias.consultarActivas());
    }
}
