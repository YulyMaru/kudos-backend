package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlmacenamientoS3Test {

    @Test
    void subeElArchivoAlBucketConSuTipoYTamano() {
        S3Client cliente = mock(S3Client.class);
        AlmacenamientoArchivosS3 almacenamiento = new AlmacenamientoArchivosS3(cliente, "kudos-adjuntos");

        almacenamiento.guardar("kudos/1/foto.png", "image/png", new byte[]{1, 2, 3});

        ArgumentCaptor<PutObjectRequest> solicitud = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cliente).putObject(solicitud.capture(), any(RequestBody.class));
        assertThat(solicitud.getValue().bucket()).isEqualTo("kudos-adjuntos");
        assertThat(solicitud.getValue().key()).isEqualTo("kudos/1/foto.png");
        assertThat(solicitud.getValue().contentType()).isEqualTo("image/png");
        assertThat(solicitud.getValue().contentLength()).isEqualTo(3L);
    }

    @Test
    void eliminaElObjetoDelBucket() {
        S3Client cliente = mock(S3Client.class);

        new AlmacenamientoArchivosS3(cliente, "kudos-adjuntos").eliminar("kudos/1/foto.png");

        ArgumentCaptor<DeleteObjectRequest> solicitud = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(cliente).deleteObject(solicitud.capture());
        assertThat(solicitud.getValue().bucket()).isEqualTo("kudos-adjuntos");
        assertThat(solicitud.getValue().key()).isEqualTo("kudos/1/foto.png");
    }

    @Test
    void generaUnEnlaceFirmadoQueExpira() {
        try (S3Presigner firmador = S3Presigner.builder()
                .endpointOverride(URI.create("https://proyecto.supabase.co/storage/v1/s3"))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("acceso", "secreto")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {

            Optional<String> enlace = new EnlacesArchivosS3(firmador, "kudos-adjuntos")
                    .generarEnlaceTemporal("kudos/1/foto.png", Duration.ofMinutes(10));

            assertThat(enlace).hasValueSatisfying(url -> assertThat(url)
                    .startsWith("https://proyecto.supabase.co/storage/v1/s3/kudos-adjuntos/kudos/1/foto.png")
                    .contains("X-Amz-Expires=600")
                    .contains("X-Amz-Signature="));
        }
    }

    @Test
    void elAlmacenamientoLocalNoOfreceEnlaces() {
        assertThat(new EnlacesArchivosLocales().generarEnlaceTemporal("x", Duration.ofMinutes(1))).isEmpty();
    }
}
