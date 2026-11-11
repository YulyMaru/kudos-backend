package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaContrasenaTest {

    @Test
    void aceptaUnaContrasenaSuficientementeLarga() {
        assertThatCode(() -> PoliticaContrasena.validar("Kudos-UCO-2026!")).doesNotThrowAnyException();
    }

    @Test
    void rechazaContrasenasCortasVaciasOSoloEspacios() {
        assertThatThrownBy(() -> PoliticaContrasena.validar("corta"))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CONTRASENA_DEBIL");
        assertThatThrownBy(() -> PoliticaContrasena.validar("               "))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> PoliticaContrasena.validar(null)).isInstanceOf(CampoObligatorioExcepcion.class);
    }

    @Test
    void rechazaContrasenasQueSuperanElLimiteDeBcrypt() {
        assertThatThrownBy(() -> PoliticaContrasena.validar("a".repeat(73)))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CONTRASENA_DEMASIADO_LARGA");
    }
}
