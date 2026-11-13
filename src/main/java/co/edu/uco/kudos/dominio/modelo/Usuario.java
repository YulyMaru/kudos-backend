package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.regla.PoliticaBloqueo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Usuario {

    private final UUID id;
    private final String nombreCompleto;
    private final String correo;
    private final String celular;
    private final boolean correoVerificado;
    private final boolean celularVerificado;
    private final Rol rol;
    private final EstadoUsuario estado;
    private final String hashContrasena;
    private final int intentosFallidos;
    private final Instant bloqueadoHasta;
    private final UUID facultadId;
    private final UUID programaId;

    public Usuario(UUID id, String nombreCompleto, String correo, String celular,
                   boolean celularVerificado, Rol rol, EstadoUsuario estado) {
        this(id, nombreCompleto, correo, celular, celularVerificado, rol, estado, null, 0, null);
    }

    public Usuario(UUID id, String nombreCompleto, String correo, String celular, boolean celularVerificado,
                   Rol rol, EstadoUsuario estado, String hashContrasena, int intentosFallidos,
                   Instant bloqueadoHasta) {
        this(id, nombreCompleto, correo, celular, true, celularVerificado, rol, estado, hashContrasena,
                intentosFallidos, bloqueadoHasta, null, null);
    }

    public Usuario(UUID id, String nombreCompleto, String correo, String celular, boolean correoVerificado,
                   boolean celularVerificado, Rol rol, EstadoUsuario estado, String hashContrasena,
                   int intentosFallidos, Instant bloqueadoHasta, UUID facultadId, UUID programaId) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombreCompleto = Objects.requireNonNull(nombreCompleto, "nombreCompleto");
        this.correo = Objects.requireNonNull(correo, "correo");
        this.celular = celular;
        this.correoVerificado = correoVerificado;
        this.celularVerificado = celularVerificado;
        this.rol = Objects.requireNonNull(rol, "rol");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.hashContrasena = hashContrasena;
        if (intentosFallidos < 0) {
            throw new IllegalArgumentException("intentosFallidos no puede ser negativo");
        }
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.facultadId = facultadId;
        this.programaId = programaId;
    }

    public static Usuario registrar(String nombreCompleto, String correo, String celular, Rol rol, String hashContrasena,
                                    UUID facultadId, UUID programaId) {
        UUID facultadAlcance = rol == Rol.COORDINADOR_FACULTAD ? facultadId : null;
        UUID programaAlcance = rol == Rol.COORDINADOR_PROGRAMA ? programaId : null;
        return new Usuario(UUID.randomUUID(), nombreCompleto, correo, celular, false, false, rol,
                EstadoUsuario.PENDIENTE, hashContrasena, 0, null, facultadAlcance, programaAlcance);
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public boolean puedeRecibirSms() {
        return celularVerificado && celular != null && !celular.isBlank();
    }

    public boolean esElMismoQue(Usuario otro) {
        return otro != null && id.equals(otro.id);
    }

    public boolean tieneContrasena() {
        return hashContrasena != null && !hashContrasena.isBlank();
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    public boolean contactosVerificados() {
        return correoVerificado && celularVerificado;
    }

    public Usuario registrarIntentoFallido(Instant ahora, PoliticaBloqueo politica) {
        int intentos = intentosFallidos + 1;
        if (politica.debeBloquear(intentos)) {
            return copia(correoVerificado, celularVerificado, estado, hashContrasena, 0,
                    ahora.plus(politica.duracionBloqueo()));
        }
        return copia(correoVerificado, celularVerificado, estado, hashContrasena, intentos, null);
    }

    public Usuario registrarInicioExitoso() {
        return copia(correoVerificado, celularVerificado, estado, hashContrasena, 0, null);
    }

    public boolean requiereLimpiarIntentos() {
        return intentosFallidos > 0 || bloqueadoHasta != null;
    }

    public Usuario conCorreoVerificado() {
        return copia(true, celularVerificado, estado, hashContrasena, intentosFallidos, bloqueadoHasta);
    }

    public Usuario conCelularVerificado() {
        return copia(correoVerificado, true, estado, hashContrasena, intentosFallidos, bloqueadoHasta);
    }

    public Usuario activar() {
        return copia(correoVerificado, celularVerificado, EstadoUsuario.ACTIVO, hashContrasena, 0, null);
    }

    public Usuario inactivar() {
        return copia(correoVerificado, celularVerificado, EstadoUsuario.INACTIVO, hashContrasena, intentosFallidos,
                bloqueadoHasta);
    }

    public Usuario conContrasena(String nuevoHash) {
        return copia(correoVerificado, celularVerificado, estado, nuevoHash, 0, null);
    }

    private Usuario copia(boolean correoOk, boolean celularOk, EstadoUsuario nuevoEstado, String hash, int intentos,
                          Instant bloqueo) {
        return new Usuario(id, nombreCompleto, correo, celular, correoOk, celularOk, rol, nuevoEstado, hash, intentos,
                bloqueo, facultadId, programaId);
    }

    public UUID getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getCelular() {
        return celular;
    }

    public boolean isCorreoVerificado() {
        return correoVerificado;
    }

    public boolean isCelularVerificado() {
        return celularVerificado;
    }

    public Rol getRol() {
        return rol;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public UUID getFacultadId() {
        return facultadId;
    }

    public UUID getProgramaId() {
        return programaId;
    }
}
