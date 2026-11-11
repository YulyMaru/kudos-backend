package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;

public final class ReglaKudo {

    private ReglaKudo() {
    }

    public static void validarPublicacion(Usuario autor, Usuario destinatario,
                                          Categoria categoria, String contenido) {
        validarCamposObligatorios(autor, destinatario, categoria, contenido);
        validarAutorActivo(autor);
        validarNoAutoReconocimiento(autor, destinatario);
        validarDestinatarioActivo(destinatario);
        validarCategoriaActiva(categoria);
    }

    private static void validarCamposObligatorios(Usuario autor, Usuario destinatario,
                                                  Categoria categoria, String contenido) {
        if (autor == null) {
            throw new CampoObligatorioExcepcion("autor");
        }
        if (destinatario == null) {
            throw new CampoObligatorioExcepcion("destinatario");
        }
        if (categoria == null) {
            throw new CampoObligatorioExcepcion("categoria");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new CampoObligatorioExcepcion("contenido");
        }
    }

    private static void validarAutorActivo(Usuario autor) {
        if (!autor.estaActivo()) {
            throw new ReglaNegocioExcepcion("AUTOR_INACTIVO", "autor",
                    "El autor debe tener una cuenta activa para publicar");
        }
    }

    private static void validarNoAutoReconocimiento(Usuario autor, Usuario destinatario) {
        if (autor.esElMismoQue(destinatario)) {
            throw new ReglaNegocioExcepcion("AUTO_RECONOCIMIENTO", "destinatario",
                    "No es posible publicar un Kudo para uno mismo");
        }
    }

    private static void validarDestinatarioActivo(Usuario destinatario) {
        if (!destinatario.estaActivo()) {
            throw new ReglaNegocioExcepcion("DESTINATARIO_INACTIVO", "destinatario",
                    "El destinatario debe tener una cuenta activa");
        }
    }

    private static void validarCategoriaActiva(Categoria categoria) {
        if (!categoria.isActiva()) {
            throw new ReglaNegocioExcepcion("CATEGORIA_INACTIVA", "categoria",
                    "La categoría seleccionada no está activa");
        }
    }
}
