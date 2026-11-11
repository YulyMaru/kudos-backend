package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarCategoriasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategoriasAdministracion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class AdministrarCategoriasCasoUso implements AdministrarCategoriasPuerto {

    public static final Set<Rol> ROLES_ADMINISTRADORES = Set.of(Rol.ADMINISTRADOR, Rol.RECTOR,
            Rol.COORDINADOR_ACADEMICO);
    private static final String RECURSO = "CATEGORIA";

    private final RepositorioUsuarios usuarios;
    private final RepositorioCategoriasAdministracion categorias;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public AdministrarCategoriasCasoUso(RepositorioUsuarios usuarios, RepositorioCategoriasAdministracion categorias,
                                        RegistroAuditoria auditoria, GestorTransacciones transacciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.categorias = Objects.requireNonNull(categorias);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public CategoriaVista crear(UUID actorId, String nombre) {
        Usuario actor = autorizar(actorId);
        Categoria categoria = Categoria.crear(nombre);
        validarNombreLibre(categoria.getNombre(), null);
        return guardar(actor, categoria, AccionAuditoria.CATEGORIA_CREADA, "nombre=" + categoria.getNombre());
    }

    @Override
    public CategoriaVista renombrar(UUID actorId, UUID categoriaId, String nombre) {
        Usuario actor = autorizar(actorId);
        Categoria renombrada = cargar(categoriaId).renombrar(nombre);
        validarNombreLibre(renombrada.getNombre(), renombrada.getId());
        return guardar(actor, renombrada, AccionAuditoria.CATEGORIA_ACTUALIZADA, "nombre=" + renombrada.getNombre());
    }

    @Override
    public CategoriaVista cambiarEstado(UUID actorId, UUID categoriaId, boolean activa) {
        Usuario actor = autorizar(actorId);
        Categoria actualizada = cargar(categoriaId).conEstado(activa);
        return guardar(actor, actualizada, AccionAuditoria.CATEGORIA_ACTUALIZADA, "activa=" + activa);
    }

    private Usuario autorizar(UUID actorId) {
        return Actores.conRol(usuarios, actorId, ROLES_ADMINISTRADORES, "administrar categorías");
    }

    private Categoria cargar(UUID categoriaId) {
        return categorias.buscarPorId(Objects.requireNonNull(categoriaId, "categoriaId"))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("categoria", categoriaId.toString()));
    }

    private void validarNombreLibre(String nombre, UUID excluirId) {
        if (categorias.existeNombre(nombre, excluirId)) {
            throw new ReglaNegocioExcepcion("CATEGORIA_DUPLICADA", "nombre", "Ya existe una categoría con ese nombre");
        }
    }

    private CategoriaVista guardar(Usuario actor, Categoria categoria, AccionAuditoria accion, String detalle) {
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            categorias.guardar(categoria);
            auditoria.registrar(EntradaAuditoria.de(ahora, categoria.getId(), actor, accion, RECURSO,
                    categoria.getId(), null, ResultadoAuditoria.EXITOSO, detalle));
        });
        return new CategoriaVista(categoria.getId(), categoria.getNombre());
    }
}
