package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Objects;

public class AlmacenamientoArchivosS3 implements AlmacenamientoArchivos {

    private final S3Client cliente;
    private final String bucket;

    public AlmacenamientoArchivosS3(S3Client cliente, String bucket) {
        this.cliente = Objects.requireNonNull(cliente);
        this.bucket = Objects.requireNonNull(bucket);
    }

    @Override
    public void guardar(String clave, String tipoContenido, byte[] contenido) {
        PutObjectRequest solicitud = PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentType(tipoContenido)
                .contentLength((long) contenido.length)
                .build();
        cliente.putObject(solicitud, RequestBody.fromBytes(contenido));
    }

    @Override
    public void eliminar(String clave) {
        cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
    }
}
