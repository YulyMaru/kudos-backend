package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.AdjuntoEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.KudoEntidad;

import java.util.UUID;

public final class KudoMapeador {

    private KudoMapeador() {
    }

    public static KudoEntidad aEntidad(Kudo kudo) {
        KudoEntidad entidad = new KudoEntidad(kudo.getId(), kudo.getAutorId(), kudo.getDestinatarioId(),
                kudo.getCategoriaId(), kudo.getContenido(), kudo.getFechaPublicacion());
        for (Adjunto adjunto : kudo.getAdjuntos()) {
            entidad.agregarAdjunto(new AdjuntoEntidad(UUID.randomUUID(), adjunto.nombreOriginal(),
                    adjunto.tipoContenido(), adjunto.tamanoBytes(), adjunto.hashSha256(), adjunto.claveObjeto()));
        }
        return entidad;
    }

    public static Kudo aDominio(KudoEntidad entidad) {
        return Kudo.reconstruir(entidad.getId(), entidad.getAutorId(), entidad.getDestinatarioId(),
                entidad.getCategoriaId(), entidad.getContenido(), entidad.getFechaPublicacion(),
                entidad.getAdjuntos().stream().map(KudoMapeador::adjuntoADominio).toList());
    }

    private static Adjunto adjuntoADominio(AdjuntoEntidad entidad) {
        return new Adjunto(entidad.getNombreOriginal(), entidad.getTipoContenido(), entidad.getTamanoBytes(),
                entidad.getHashSha256(), entidad.getClaveObjeto());
    }
}
