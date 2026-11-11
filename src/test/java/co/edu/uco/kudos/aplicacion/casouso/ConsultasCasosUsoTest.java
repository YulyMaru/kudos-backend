package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleVista;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;
import co.edu.uco.kudos.aplicacion.dto.PaginaFeed;
import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultasCasosUsoTest {

    private static final Instant BASE = Instant.parse("2026-11-11T10:00:00Z");
    private static final PersonaVista ANA = new PersonaVista(UUID.randomUUID(), "Ana", Rol.DOCENTE);
    private static final PersonaVista DIEGO = new PersonaVista(UUID.randomUUID(), "Diego", Rol.ESTUDIANTE);
    private static final CategoriaVista LIDERAZGO = new CategoriaVista(UUID.randomUUID(), "Liderazgo");

    private KudosEnMemoria kudos;

    @BeforeEach
    void preparar() {
        kudos = new KudosEnMemoria();
        for (int i = 0; i < 5; i++) {
            kudos.agregar(resumen(BASE.plusSeconds(i)));
        }
    }

    @Test
    void elFeedSePaginaConCursorSinRepetirNiSaltarKudos() {
        ConsultarFeedCasoUso casoUso = new ConsultarFeedCasoUso(kudos);

        PaginaFeed primera = casoUso.consultar(null, 2);
        PaginaFeed segunda = casoUso.consultar(primera.siguienteCursor(), 2);
        PaginaFeed tercera = casoUso.consultar(segunda.siguienteCursor(), 2);

        List<KudoResumenVista> todos = new ArrayList<>();
        todos.addAll(primera.kudos());
        todos.addAll(segunda.kudos());
        todos.addAll(tercera.kudos());
        assertThat(todos).extracting(KudoResumenVista::fechaPublicacion)
                .containsExactly(BASE.plusSeconds(4), BASE.plusSeconds(3), BASE.plusSeconds(2), BASE.plusSeconds(1),
                        BASE);
        assertThat(primera.siguienteCursor()).isNotNull();
        assertThat(tercera.kudos()).hasSize(1);
        assertThat(tercera.siguienteCursor()).isNull();
    }

    @Test
    void elFeedUsaVeinteKudosPorDefectoYRechazaLimitesFueraDeRango() {
        ConsultarFeedCasoUso casoUso = new ConsultarFeedCasoUso(kudos);

        casoUso.consultar(null, null);

        assertThat(kudos.ultimoLimite).isEqualTo(ConsultarFeedCasoUso.LIMITE_POR_DEFECTO + 1);
        assertThatThrownBy(() -> casoUso.consultar(null, 0)).isInstanceOf(ParametroInvalidoExcepcion.class);
        assertThatThrownBy(() -> casoUso.consultar(null, 51)).isInstanceOf(ParametroInvalidoExcepcion.class);
    }

    @Test
    void unCursorManipuladoSeRechaza() {
        ConsultarFeedCasoUso casoUso = new ConsultarFeedCasoUso(kudos);

        assertThatThrownBy(() -> casoUso.consultar("esto-no-es-un-cursor", 10))
                .isInstanceOf(ParametroInvalidoExcepcion.class)
                .extracting("campo").isEqualTo("cursor");
    }

    @Test
    void elCursorConservaLaFechaConNanosegundosYElIdentificador() {
        CursorFeed cursor = new CursorFeed(Instant.parse("2026-11-11T10:00:00.123456789Z"), UUID.randomUUID());

        assertThat(CursorFeed.decodificar(cursor.codificar())).isEqualTo(cursor);
    }

    @Test
    void elDetalleIncluyeLosAdjuntosSinExponerSuClaveDeAlmacenamiento() {
        UUID id = UUID.randomUUID();
        UUID adjuntoId = UUID.randomUUID();
        kudos.detalle = new KudoDetalleDatos(id, ANA, DIEGO, LIDERAZGO, "Contenido completo", BASE,
                List.of(new AdjuntoDatos(adjuntoId, "evidencia.pdf", "application/pdf", 2048, "kudos/1/a.pdf")),
                3, 4.5, 2);
        ConsultarKudoCasoUso casoUso = new ConsultarKudoCasoUso(kudos);

        KudoDetalleVista detalle = casoUso.consultar(id);

        assertThat(detalle.contenido()).isEqualTo("Contenido completo");
        assertThat(detalle.cantidadReplicas()).isEqualTo(3);
        assertThat(detalle.promedioCalificacion()).isEqualTo(4.5);
        assertThat(detalle.adjuntos()).singleElement().satisfies(adjunto -> {
            assertThat(adjunto.id()).isEqualTo(adjuntoId);
            assertThat(adjunto.nombreOriginal()).isEqualTo("evidencia.pdf");
        });
    }

    @Test
    void elDetalleDeUnKudoInexistenteLanzaNoEncontrado() {
        ConsultarKudoCasoUso casoUso = new ConsultarKudoCasoUso(kudos);

        assertThatThrownBy(() -> casoUso.consultar(UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class);
    }

    @Test
    void laBusquedaDeUsuariosValidaElTextoYExcluyeAlSolicitante() {
        List<Object[]> llamadas = new ArrayList<>();
        BuscarUsuariosCasoUso casoUso = new BuscarUsuariosCasoUso((texto, limite, excluir) -> {
            llamadas.add(new Object[]{texto, limite, excluir});
            return List.of(DIEGO);
        });

        List<PersonaVista> resultado = casoUso.buscar("  die  ", null, ANA.id());

        assertThat(resultado).containsExactly(DIEGO);
        assertThat(llamadas).singleElement()
                .satisfies(llamada -> assertThat(llamada).containsExactly("die", 10, ANA.id()));
        assertThatThrownBy(() -> casoUso.buscar("d", null, ANA.id())).isInstanceOf(ParametroInvalidoExcepcion.class);
        assertThatThrownBy(() -> casoUso.buscar(null, null, ANA.id())).isInstanceOf(ParametroInvalidoExcepcion.class);
        assertThatThrownBy(() -> casoUso.buscar("diego", 21, ANA.id())).isInstanceOf(ParametroInvalidoExcepcion.class);
    }

    @Test
    void lasCategoriasSeDelegaALaConsulta() {
        ConsultarCategoriasCasoUso casoUso = new ConsultarCategoriasCasoUso(() -> List.of(LIDERAZGO));

        assertThat(casoUso.consultarActivas()).containsExactly(LIDERAZGO);
    }

    private static KudoResumenVista resumen(Instant fecha) {
        return new KudoResumenVista(UUID.randomUUID(), ANA, DIEGO, LIDERAZGO, "Gracias", false, fecha, 0, 0, 0, 0);
    }

    private static final class KudosEnMemoria implements ConsultasKudos {
        private final List<KudoResumenVista> datos = new ArrayList<>();
        private KudoDetalleDatos detalle;
        private int ultimoLimite;

        void agregar(KudoResumenVista kudo) {
            datos.add(kudo);
        }

        @Override
        public List<KudoResumenVista> feed(CursorFeed despuesDe, int limite) {
            ultimoLimite = limite;
            Comparator<KudoResumenVista> orden = Comparator.comparing(KudoResumenVista::fechaPublicacion)
                    .thenComparing(KudoResumenVista::id).reversed();
            return datos.stream()
                    .sorted(orden)
                    .filter(k -> despuesDe == null || k.fechaPublicacion().isBefore(despuesDe.fechaPublicacion())
                            || (k.fechaPublicacion().equals(despuesDe.fechaPublicacion())
                            && k.id().compareTo(despuesDe.id()) < 0))
                    .limit(limite)
                    .toList();
        }

        @Override
        public Optional<KudoDetalleDatos> detalle(UUID id) {
            return Optional.ofNullable(detalle).filter(d -> d.id().equals(id));
        }

        @Override
        public Optional<AdjuntoDatos> adjunto(UUID kudoId, UUID adjuntoId) {
            return detalle(kudoId).flatMap(d -> d.adjuntos().stream().filter(a -> a.id().equals(adjuntoId)).findFirst());
        }
    }
}
