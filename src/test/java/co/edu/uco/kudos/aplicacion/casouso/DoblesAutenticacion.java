package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;
import co.edu.uco.kudos.aplicacion.dto.TokenAcceso;
import co.edu.uco.kudos.aplicacion.puerto.salida.CodificadorContrasenas;
import co.edu.uco.kudos.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCodigos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoAprobacion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class DoblesAutenticacion {

    private DoblesAutenticacion() {
    }

    static final class CodificadorFalso implements CodificadorContrasenas {
        int comparaciones;

        @Override
        public String codificar(String contrasena) {
            return "hash:" + contrasena;
        }

        @Override
        public boolean coincide(String contrasena, String hash) {
            comparaciones++;
            return hash != null && hash.equals("hash:" + contrasena);
        }
    }

    static final class EmisorFalso implements EmisorTokens {
        @Override
        public TokenAcceso emitir(Usuario usuario, Instant ahora) {
            return new TokenAcceso("jwt-" + usuario.getId(), ahora.plus(Duration.ofMinutes(15)));
        }
    }

    static final class CuentasEnMemoria implements RepositorioCuentas, RepositorioUsuarios {
        final Map<UUID, Usuario> usuarios = new LinkedHashMap<>();

        @Override
        public Optional<Usuario> buscarPorCorreo(String correo) {
            return usuarios.values().stream().filter(u -> u.getCorreo().equalsIgnoreCase(correo)).findFirst();
        }

        @Override
        public boolean existeConRol(Rol rol) {
            return usuarios.values().stream().anyMatch(u -> u.getRol() == rol);
        }

        @Override
        public boolean existePorCorreoOCelular(String correo, String celular) {
            return usuarios.values().stream().anyMatch(u -> u.getCorreo().equalsIgnoreCase(correo)
                    || Objects.equals(u.getCelular(), celular));
        }

        @Override
        public Optional<Usuario> buscarActivoConRol(Rol rol, UUID facultadId, UUID programaId) {
            return usuarios.values().stream()
                    .filter(u -> u.getRol() == rol && u.getEstado() == EstadoUsuario.ACTIVO)
                    .filter(u -> rol != Rol.COORDINADOR_FACULTAD || Objects.equals(u.getFacultadId(), facultadId))
                    .filter(u -> rol != Rol.COORDINADOR_PROGRAMA || Objects.equals(u.getProgramaId(), programaId))
                    .findFirst();
        }

        @Override
        public void guardar(Usuario usuario) {
            usuarios.put(usuario.getId(), usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(UUID id) {
            return Optional.ofNullable(usuarios.get(id));
        }
    }

    static final class TokensEnMemoria implements RepositorioTokensRefresco {
        final Map<UUID, TokenRefresco> tokens = new LinkedHashMap<>();
        boolean simularRotacionConcurrente;

        @Override
        public void guardar(TokenRefresco token) {
            tokens.put(token.id(), token);
        }

        @Override
        public Optional<TokenRefresco> buscarPorHash(String hashToken) {
            return tokens.values().stream().filter(t -> t.hashToken().equals(hashToken)).findFirst();
        }

        @Override
        public boolean revocarSiVigente(UUID id, Instant fecha, UUID reemplazadoPor) {
            TokenRefresco token = tokens.get(id);
            if (simularRotacionConcurrente || token == null || token.estaRevocado()) {
                return false;
            }
            tokens.put(id, revocado(token, fecha, reemplazadoPor));
            return true;
        }

        @Override
        public void revocarTodosDeUsuario(UUID usuarioId, Instant fecha) {
            tokens.replaceAll((id, token) -> token.usuarioId().equals(usuarioId) && !token.estaRevocado()
                    ? revocado(token, fecha, null) : token);
        }

        private static TokenRefresco revocado(TokenRefresco token, Instant fecha, UUID reemplazadoPor) {
            return new TokenRefresco(token.id(), token.usuarioId(), token.hashToken(), token.creadoEn(),
                    token.expiraEn(), fecha, reemplazadoPor);
        }
    }

    static final class SolicitudesEnMemoria implements RepositorioSolicitudes {
        final Map<UUID, SolicitudRegistro> solicitudes = new LinkedHashMap<>();

        @Override
        public void guardar(SolicitudRegistro solicitud) {
            solicitudes.put(solicitud.id(), solicitud);
        }

        @Override
        public void actualizar(SolicitudRegistro solicitud) {
            solicitudes.put(solicitud.id(), solicitud);
        }

        @Override
        public Optional<SolicitudRegistro> buscarPorId(UUID id) {
            return Optional.ofNullable(solicitudes.get(id));
        }

        @Override
        public Optional<SolicitudRegistro> buscarPorUsuario(UUID usuarioId) {
            return solicitudes.values().stream().filter(s -> s.usuarioId().equals(usuarioId)).findFirst();
        }

        @Override
        public List<SolicitudPendienteVista> pendientes(List<Rol> rolesSolicitados, UUID programaId, UUID facultadId,
                                                        int limite) {
            return solicitudes.values().stream()
                    .filter(s -> s.estado() == EstadoAprobacion.PENDIENTE && rolesSolicitados.contains(s.rolSolicitado()))
                    .filter(s -> programaId == null || programaId.equals(s.programaId()))
                    .map(s -> new SolicitudPendienteVista(s.id(), s.usuarioId(), "", "", s.rolSolicitado(), null, null,
                            false, false, s.creadaEn()))
                    .limit(limite)
                    .toList();
        }
    }

    static final class CodigosEnMemoria implements RepositorioCodigos {
        final Map<UUID, CodigoVerificacion> codigos = new LinkedHashMap<>();

        @Override
        public void guardar(CodigoVerificacion codigo) {
            codigos.put(codigo.id(), codigo);
        }

        @Override
        public void actualizar(CodigoVerificacion codigo) {
            codigos.put(codigo.id(), codigo);
        }

        @Override
        public Optional<CodigoVerificacion> ultimo(UUID usuarioId, PropositoCodigo proposito) {
            return codigos.values().stream()
                    .filter(c -> c.usuarioId().equals(usuarioId) && c.proposito() == proposito)
                    .max(Comparator.comparing(CodigoVerificacion::creadoEn));
        }

        @Override
        public Optional<CodigoVerificacion> buscarPorHash(PropositoCodigo proposito, String hashCodigo) {
            return codigos.values().stream()
                    .filter(c -> c.proposito() == proposito && c.hashCodigo().equals(hashCodigo))
                    .findFirst();
        }

        @Override
        public int contarEmitidosDesde(UUID usuarioId, PropositoCodigo proposito, Instant desde) {
            return (int) codigos.values().stream()
                    .filter(c -> c.usuarioId().equals(usuarioId) && c.proposito() == proposito)
                    .filter(c -> !c.creadoEn().isBefore(desde))
                    .count();
        }

        @Override
        public void invalidarVigentes(UUID usuarioId, PropositoCodigo proposito, Instant fecha) {
            codigos.replaceAll((id, c) -> c.usuarioId().equals(usuarioId) && c.proposito() == proposito
                    && c.usadoEn() == null ? c.usado(fecha) : c);
        }
    }

    static final class OutboxEnMemoria implements RepositorioOutbox {
        final List<Notificacion> notificaciones = new ArrayList<>();

        @Override
        public void guardarTodas(List<Notificacion> nuevas) {
            notificaciones.addAll(nuevas);
        }
    }

    static final class VinculosEnMemoria implements RepositorioVinculos {
        final Set<String> vinculos = new HashSet<>();

        @Override
        public void vincular(UUID usuarioId, UUID programaId) {
            vinculos.add(usuarioId + "/" + programaId);
        }

        @Override
        public boolean desvincular(UUID usuarioId, UUID programaId) {
            return vinculos.remove(usuarioId + "/" + programaId);
        }

        @Override
        public List<Programa> programasDe(UUID usuarioId) {
            return List.of();
        }

        @Override
        public List<PersonaVista> miembros(UUID programaId) {
            return List.of();
        }
    }

    static final class EstructuraEnMemoria implements RepositorioEstructura {
        final Map<UUID, Facultad> facultades = new LinkedHashMap<>();
        final Map<UUID, Programa> programas = new LinkedHashMap<>();

        @Override
        public Optional<Facultad> facultad(UUID id) {
            return Optional.ofNullable(id == null ? null : facultades.get(id));
        }

        @Override
        public Optional<Programa> programa(UUID id) {
            return Optional.ofNullable(id == null ? null : programas.get(id));
        }

        @Override
        public boolean existeFacultad(String nombre) {
            return facultades.values().stream().anyMatch(f -> f.nombre().equalsIgnoreCase(nombre));
        }

        @Override
        public boolean existePrograma(UUID facultadId, String nombre) {
            return programas.values().stream()
                    .anyMatch(p -> p.facultadId().equals(facultadId) && p.nombre().equalsIgnoreCase(nombre));
        }

        @Override
        public void guardarFacultad(Facultad facultad) {
            facultades.put(facultad.id(), facultad);
        }

        @Override
        public void guardarPrograma(Programa programa) {
            programas.put(programa.id(), programa);
        }

        @Override
        public List<Facultad> facultadesActivas() {
            return List.copyOf(facultades.values());
        }

        @Override
        public List<Programa> programasActivos(UUID facultadId) {
            return programas.values().stream().filter(p -> p.facultadId().equals(facultadId)).toList();
        }
    }

    static final class RelojAjustable extends Clock {
        private Instant actual;

        RelojAjustable(Instant inicio) {
            this.actual = inicio;
        }

        void avanzar(Duration duracion) {
            actual = actual.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return actual;
        }
    }
}
