package co.edu.uco.kudos.aplicacion.casouso;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

final class TokensSeguros {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;
    private static final int DIGITOS_CODIGO = 6;

    private TokensSeguros() {
    }

    static String generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String generarCodigoNumerico() {
        int limite = (int) Math.pow(10, DIGITOS_CODIGO);
        return String.format("%0" + DIGITOS_CODIGO + "d", ALEATORIO.nextInt(limite));
    }

    static String hashCodigo(UUID usuarioId, String codigo) {
        return sha256(usuarioId + ":" + codigo);
    }

    static String sha256(String valor) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }
}
