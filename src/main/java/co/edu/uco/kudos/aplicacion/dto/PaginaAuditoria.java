package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;

import java.util.List;

public record PaginaAuditoria(List<EntradaAuditoria> registros, int pagina, int tamano, long total) {

    public PaginaAuditoria {
        registros = List.copyOf(registros);
    }
}
