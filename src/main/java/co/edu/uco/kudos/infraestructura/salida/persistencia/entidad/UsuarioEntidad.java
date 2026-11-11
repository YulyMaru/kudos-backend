package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioEntidad extends EntidadPersistible {

    @Id
    private UUID id;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "correo", nullable = false, length = 254)
    private String correo;

    @Column(name = "celular", length = 20)
    private String celular;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    @Column(name = "celular_verificado", nullable = false)
    private boolean celularVerificado;

    @Column(name = "facultad_id")
    private UUID facultadId;

    @Column(name = "programa_id")
    private UUID programaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoUsuario estado;

    @Column(name = "hash_contrasena", length = 100)
    private String hashContrasena;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected UsuarioEntidad() {
    }

    public UsuarioEntidad(UUID id, String nombreCompleto, String correo, String celular,
                          boolean celularVerificado, Rol rol, EstadoUsuario estado) {
        this(id, nombreCompleto, correo, celular, true, celularVerificado, rol, estado, null, 0, null, null, null);
    }

    public UsuarioEntidad(UUID id, String nombreCompleto, String correo, String celular, boolean correoVerificado,
                          boolean celularVerificado, Rol rol, EstadoUsuario estado, String hashContrasena,
                          int intentosFallidos, Instant bloqueadoHasta, UUID facultadId, UUID programaId) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.celular = celular;
        this.correoVerificado = correoVerificado;
        this.facultadId = facultadId;
        this.programaId = programaId;
        this.celularVerificado = celularVerificado;
        this.rol = rol;
        this.estado = estado;
        this.hashContrasena = hashContrasena;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public void actualizarDesde(UsuarioEntidad otra) {
        this.nombreCompleto = otra.nombreCompleto;
        this.correo = otra.correo;
        this.celular = otra.celular;
        this.correoVerificado = otra.correoVerificado;
        this.celularVerificado = otra.celularVerificado;
        this.facultadId = otra.facultadId;
        this.programaId = otra.programaId;
        this.rol = otra.rol;
        this.estado = otra.estado;
        this.hashContrasena = otra.hashContrasena;
        this.intentosFallidos = otra.intentosFallidos;
        this.bloqueadoHasta = otra.bloqueadoHasta;
    }

    @Override
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

    public UUID getFacultadId() {
        return facultadId;
    }

    public UUID getProgramaId() {
        return programaId;
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
}
