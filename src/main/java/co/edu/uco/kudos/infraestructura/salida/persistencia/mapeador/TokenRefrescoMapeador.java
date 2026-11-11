package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.TokenRefrescoEntidad;

public final class TokenRefrescoMapeador {

    private TokenRefrescoMapeador() {
    }

    public static TokenRefrescoEntidad aEntidad(TokenRefresco token) {
        return new TokenRefrescoEntidad(token.id(), token.usuarioId(), token.hashToken(), token.creadoEn(),
                token.expiraEn(), token.revocadoEn(), token.reemplazadoPor());
    }

    public static TokenRefresco aDominio(TokenRefrescoEntidad entidad) {
        return new TokenRefresco(entidad.getId(), entidad.getUsuarioId(), entidad.getHashToken(),
                entidad.getCreadoEn(), entidad.getExpiraEn(), entidad.getRevocadoEn(), entidad.getReemplazadoPor());
    }
}
