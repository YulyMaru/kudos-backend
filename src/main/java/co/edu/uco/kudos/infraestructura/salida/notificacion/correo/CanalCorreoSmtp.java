package co.edu.uco.kudos.infraestructura.salida.notificacion.correo;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class CanalCorreoSmtp implements CanalNotificacion {

    public static final String ENCABEZADO_ID_NOTIFICACION = "X-Kudos-Notificacion-Id";
    static final String ASUNTO = "¡Recibiste un Kudo en UCO Kudos!";

    private final JavaMailSender enviador;
    private final String remitente;
    private final String nombreRemitente;
    private final String urlFrontend;

    public CanalCorreoSmtp(JavaMailSender enviador, String remitente, String nombreRemitente, String urlFrontend) {
        this.enviador = Objects.requireNonNull(enviador);
        this.remitente = Objects.requireNonNull(remitente);
        this.nombreRemitente = Objects.requireNonNull(nombreRemitente);
        this.urlFrontend = Objects.requireNonNull(urlFrontend).replaceAll("/+$", "");
    }

    @Override
    public Canal canal() {
        return Canal.EMAIL;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        Mensaje mensaje = construir(notificacion.getDatos());
        MimeMessage correo = enviador.createMimeMessage();
        try {
            MimeMessageHelper ayudante = new MimeMessageHelper(correo, true, "UTF-8");
            ayudante.setFrom(remitente, nombreRemitente);
            ayudante.setTo(notificacion.getDestinatario());
            ayudante.setSubject(mensaje.asunto());
            ayudante.setText(mensaje.texto(), mensaje.html());
            correo.setHeader(ENCABEZADO_ID_NOTIFICACION, notificacion.getId().toString());
        } catch (MessagingException | UnsupportedEncodingException error) {
            throw new IllegalStateException("No fue posible construir el correo de la notificación", error);
        }
        enviador.send(correo);
        return ResultadoEnvio.exitoso();
    }

    Mensaje construir(Map<String, String> datos) {
        String nombre = datos.getOrDefault("nombreDestinatario", "");
        return switch (datos.getOrDefault("plantilla", "KUDO_RECIBIDO")) {
            case "CODIGO_VERIFICACION" -> {
                String codigo = datos.getOrDefault("codigo", "");
                yield mensaje("Tu código de verificación de UCO Kudos", nombre,
                        "Tu código para verificar el correo es " + codigo + ". Vence en 15 minutos.",
                        "Tu código para verificar el correo es <strong style=\"font-size:20px;letter-spacing:3px;\">"
                                + escapar(codigo) + "</strong>. Vence en 15 minutos.", null, null);
            }
            case "RESTABLECER_CONTRASENA" -> {
                String enlace = urlFrontend + "/restablecer?token="
                        + URLEncoder.encode(datos.getOrDefault("token", ""), StandardCharsets.UTF_8);
                yield mensaje("Restablece tu contraseña de UCO Kudos", nombre,
                        "Recibimos una solicitud para restablecer tu contraseña. El enlace vence en 30 minutos. "
                                + "Si no fuiste tú, ignora este correo.",
                        "Recibimos una solicitud para restablecer tu contraseña. El enlace vence en 30 minutos. "
                                + "Si no fuiste tú, ignora este correo.", enlace, "Restablecer contraseña");
            }
            case "SOLICITUD_RECHAZADA" -> {
                String motivo = datos.getOrDefault("motivo", "");
                yield mensaje("Tu solicitud de registro en UCO Kudos fue rechazada", nombre,
                        "Tu solicitud de registro fue rechazada. Motivo: " + motivo,
                        "Tu solicitud de registro fue rechazada. Motivo: " + escapar(motivo), null, null);
            }
            case "CUENTA_ACTIVADA" -> mensaje("Tu cuenta de UCO Kudos está activa", nombre,
                    "Tu cuenta fue activada. Ya puedes iniciar sesión y reconocer a tu comunidad.",
                    "Tu cuenta fue activada. Ya puedes iniciar sesión y reconocer a tu comunidad.",
                    urlFrontend + "/iniciar-sesion", "Iniciar sesión");
            default -> {
                String autor = datos.getOrDefault("nombreAutor", "Un miembro de la comunidad");
                String categoria = datos.getOrDefault("nombreCategoria", "");
                String enlace = urlFrontend + "/kudos/" + datos.getOrDefault("idKudo", "");
                yield mensaje(ASUNTO, nombre,
                        autor + " te reconoció con un Kudo en la categoría " + categoria + ".",
                        "<strong>" + escapar(autor) + "</strong> te reconoció con un Kudo en la categoría <strong>"
                                + escapar(categoria) + "</strong>.", enlace, "Ver mi Kudo");
            }
        };
    }

    private static Mensaje mensaje(String asunto, String nombre, String cuerpoTexto, String cuerpoHtml, String enlace,
                                   String textoBoton) {
        String texto = "Hola " + nombre + ",\n\n" + cuerpoTexto + "\n\n"
                + (enlace == null ? "" : enlace + "\n\n") + "UCO Kudos\n";
        String boton = enlace == null ? "" : "<p><a href=\"" + escapar(enlace)
                + "\" style=\"background:#0b5ed7;color:#ffffff;padding:10px 16px;border-radius:6px;"
                + "text-decoration:none;\">" + escapar(textoBoton) + "</a></p>";
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <body style="font-family: Arial, sans-serif; color: #1f2937;">
                <p>Hola %s,</p>
                <p>%s</p>
                %s
                <p style="color:#6b7280;font-size:12px;">UCO Kudos</p>
                </body>
                </html>
                """.formatted(escapar(nombre), cuerpoHtml, boton);
        return new Mensaje(asunto, texto, html);
    }

    static String escapar(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    record Mensaje(String asunto, String texto, String html) {
    }
}
