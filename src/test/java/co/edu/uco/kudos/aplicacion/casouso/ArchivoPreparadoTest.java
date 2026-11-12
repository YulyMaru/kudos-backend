package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.dominio.modelo.Adjunto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class ArchivoPreparadoTest {

    private static Constructor<?> constructor;

    private final Adjunto adjunto = new Adjunto("foto.png", "image/png", 3, "abc", "kudos/1/2.png");
    private final Adjunto otroAdjunto = new Adjunto("otra.png", "image/png", 3, "def", "kudos/1/3.png");

    @BeforeAll
    static void obtenerConstructor() throws Exception {
        Class<?> tipo = Class.forName(PublicarKudoCasoUso.class.getName() + "$ArchivoPreparado");
        constructor = tipo.getDeclaredConstructor(Adjunto.class, byte[].class);
        constructor.setAccessible(true);
    }

    private static Object archivo(Adjunto adjunto, byte[] contenido) throws Exception {
        return constructor.newInstance(adjunto, contenido);
    }

    @Test
    void esIgualCuandoElAdjuntoYElContenidoCoinciden() throws Exception {
        Object archivo = archivo(adjunto, new byte[]{1, 2, 3});
        Object mismoContenido = archivo(adjunto, new byte[]{1, 2, 3});

        assertThat(archivo).isEqualTo(mismoContenido);
        assertThat(archivo).hasSameHashCodeAs(mismoContenido);
    }

    @Test
    void esDistintoCuandoCambiaElContenidoElAdjuntoOElTipo() throws Exception {
        Object archivo = archivo(adjunto, new byte[]{1, 2, 3});

        assertThat(archivo).isNotEqualTo(archivo(adjunto, new byte[]{1, 2, 4}));
        assertThat(archivo).isNotEqualTo(archivo(otroAdjunto, new byte[]{1, 2, 3}));
        assertThat(archivo).isNotEqualTo(null);
        assertThat(archivo).isNotEqualTo("ArchivoPreparado");
    }

    @Test
    void calculaElHashConElAdjuntoYElContenidoAunqueSeanNulos() throws Exception {
        assertThat(archivo(null, null).hashCode()).isZero();
        assertThat(archivo(null, null)).isEqualTo(archivo(null, null));
        assertThat(archivo(adjunto, new byte[]{1}).hashCode())
                .isNotEqualTo(archivo(adjunto, new byte[]{2}).hashCode());
    }

    @Test
    void muestraElAdjuntoYLosBytesDelContenido() throws Exception {
        assertThat(archivo(adjunto, new byte[]{1, 2, 3}))
                .hasToString("ArchivoPreparado[adjunto=" + adjunto + ", contenido=[1, 2, 3]]");
    }
}
