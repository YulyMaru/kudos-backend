package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import co.edu.uco.kudos.aplicacion.puerto.salida.EnlacesArchivos;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class EnlacesArchivosS3 implements EnlacesArchivos {

    private final S3Presigner firmador;
    private final String bucket;

    public EnlacesArchivosS3(S3Presigner firmador, String bucket) {
        this.firmador = Objects.requireNonNull(firmador);
        this.bucket = Objects.requireNonNull(bucket);
    }

    @Override
    public Optional<String> generarEnlaceTemporal(String clave, Duration vigencia) {
        GetObjectPresignRequest solicitud = GetObjectPresignRequest.builder()
                .signatureDuration(vigencia)
                .getObjectRequest(objeto -> objeto.bucket(bucket).key(clave))
                .build();
        return Optional.of(firmador.presignGetObject(solicitud).url().toString());
    }
}
