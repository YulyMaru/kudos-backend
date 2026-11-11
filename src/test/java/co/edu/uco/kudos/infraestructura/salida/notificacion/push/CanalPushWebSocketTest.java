package co.edu.uco.kudos.infraestructura.salida.notificacion.push;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanalPushWebSocketTest {

    private final SimpMessagingTemplate mensajeria = mock(SimpMessagingTemplate.class);
    private final SimpUserRegistry usuarios = mock(SimpUserRegistry.class);
    private final CanalPushWebSocket canal = new CanalPushWebSocket(mensajeria, usuarios);
    private final String destinatario = UUID.randomUUID().toString();

    @Test
    @SuppressWarnings("unchecked")
    void enviaSoloAlDestinatarioConectado() {
        when(usuarios.getUser(destinatario)).thenReturn(mock(SimpUser.class));

        ResultadoEnvio resultado = canal.enviar(notificacion());

        ArgumentCaptor<Object> mensaje = ArgumentCaptor.forClass(Object.class);
        verify(mensajeria).convertAndSendToUser(eq(destinatario), eq(CanalPushWebSocket.DESTINO_USUARIO),
                mensaje.capture());
        assertThat(resultado.entregado()).isTrue();
        assertThat(canal.canal()).isEqualTo(Canal.PUSH);
        assertThat((Map<String, String>) mensaje.getValue())
                .containsEntry("tipo", "KUDO_RECIBIDO")
                .containsEntry("nombreAutor", "Ana")
                .containsEntry("nombreCategoria", "Liderazgo");
    }

    @Test
    void noAplicaSiElDestinatarioNoEstaConectado() {
        when(usuarios.getUser(destinatario)).thenReturn(null);

        ResultadoEnvio resultado = canal.enviar(notificacion());

        assertThat(resultado.entregado()).isFalse();
        assertThat(resultado.motivo()).contains("no está conectado");
        verify(mensajeria, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
    }

    private Notificacion notificacion() {
        return Notificacion.pendiente(UUID.randomUUID(), UUID.randomUUID(), Canal.PUSH, destinatario,
                Map.of("nombreAutor", "Ana", "nombreCategoria", "Liderazgo", "idKudo", UUID.randomUUID().toString()),
                Instant.parse("2026-11-11T10:00:00Z"));
    }
}
