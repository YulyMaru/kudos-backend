package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglaRegistroCorreoTest {

    @ParameterizedTest
    @ValueSource(strings = {"ana@uco.edu.co", "a@u.co", "ana.perez@correo.uco.net.co"})
    void aceptaCorreosConDominioQueTienePuntoInterno(String correo) {
        assertThat(ReglaRegistro.esCorreoValido(correo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ana", "ana@", "@uco.edu.co", "ana@ucoeduco", "ana@.uco", "ana@uco.",
            "ana@.", "ana perez@uco.edu.co", "ana@uco edu.co", "ana@b@uco.edu.co"})
    void rechazaCorreosMalFormados(String correo) {
        assertThat(ReglaRegistro.esCorreoValido(correo)).isFalse();
    }

    @Test
    void rechazaCorreoNulo() {
        assertThat(ReglaRegistro.esCorreoValido(null)).isFalse();
    }

    @Test
    void normalizaUnCorreoInstitucionalValido() {
        assertThat(ReglaRegistro.normalizarCorreoInstitucional("  Ana@UCO.edu.co ", List.of("uco.edu.co")))
                .isEqualTo("ana@uco.edu.co");
        assertThat(ReglaRegistro.normalizarCorreoInstitucional("ana@correo.uco.edu.co", List.of(" UCO.EDU.CO ")))
                .isEqualTo("ana@correo.uco.edu.co");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@ucoeduco", "ana@.uco", "ana@uco.", "ana", "ana@b@uco.edu.co"})
    void rechazaAlNormalizarCorreosSinFormatoValido(String correo) {
        assertThatThrownBy(() -> ReglaRegistro.normalizarCorreoInstitucional(correo, List.of("uco.edu.co")))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting(error -> ((ReglaNegocioExcepcion) error).getCodigo())
                .isEqualTo("CORREO_INVALIDO");
    }

    @Test
    void rechazaCorreosDeDominiosNoInstitucionales() {
        assertThatThrownBy(() -> ReglaRegistro.normalizarCorreoInstitucional("ana@gmail.com", List.of("uco.edu.co")))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting(error -> ((ReglaNegocioExcepcion) error).getCodigo())
                .isEqualTo("CORREO_NO_INSTITUCIONAL");
    }
}
