package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;
import co.edu.uco.kudos.aplicacion.puerto.salida.EnlacesArchivos;
import co.edu.uco.kudos.infraestructura.salida.almacenamiento.AlmacenamientoArchivosLocal;
import co.edu.uco.kudos.infraestructura.salida.almacenamiento.AlmacenamientoArchivosS3;
import co.edu.uco.kudos.infraestructura.salida.almacenamiento.EnlacesArchivosLocales;
import co.edu.uco.kudos.infraestructura.salida.almacenamiento.EnlacesArchivosS3;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(PropiedadesAlmacenamiento.class)
public class AlmacenamientoConfiguracion {

    private static final String PROPIEDAD_TIPO = "kudos.almacenamiento.tipo";

    @Bean
    @ConditionalOnProperty(name = PROPIEDAD_TIPO, havingValue = "local", matchIfMissing = true)
    AlmacenamientoArchivos almacenamientoArchivosLocal(PropiedadesAlmacenamiento propiedades) {
        return new AlmacenamientoArchivosLocal(Path.of(propiedades.directorio()));
    }

    @Bean
    @ConditionalOnProperty(name = PROPIEDAD_TIPO, havingValue = "local", matchIfMissing = true)
    EnlacesArchivos enlacesArchivosLocales() {
        return new EnlacesArchivosLocales();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = PROPIEDAD_TIPO, havingValue = "s3")
    S3Client clienteS3(PropiedadesAlmacenamiento propiedades) {
        propiedades.validarConfiguracionS3();
        return S3Client.builder()
                .endpointOverride(URI.create(propiedades.endpoint()))
                .region(Region.of(propiedades.region()))
                .credentialsProvider(credenciales(propiedades))
                .serviceConfiguration(configuracionS3())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = PROPIEDAD_TIPO, havingValue = "s3")
    S3Presigner firmadorS3(PropiedadesAlmacenamiento propiedades) {
        propiedades.validarConfiguracionS3();
        return S3Presigner.builder()
                .endpointOverride(URI.create(propiedades.endpoint()))
                .region(Region.of(propiedades.region()))
                .credentialsProvider(credenciales(propiedades))
                .serviceConfiguration(configuracionS3())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = PROPIEDAD_TIPO, havingValue = "s3")
    AlmacenamientoArchivos almacenamientoArchivosS3(S3Client cliente, PropiedadesAlmacenamiento propiedades) {
        return new AlmacenamientoArchivosS3(cliente, propiedades.bucket());
    }

    @Bean
    @ConditionalOnProperty(name = PROPIEDAD_TIPO, havingValue = "s3")
    EnlacesArchivos enlacesArchivosS3(S3Presigner firmador, PropiedadesAlmacenamiento propiedades) {
        return new EnlacesArchivosS3(firmador, propiedades.bucket());
    }

    private static StaticCredentialsProvider credenciales(PropiedadesAlmacenamiento propiedades) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(propiedades.claveAcceso(), propiedades.claveSecreta()));
    }

    private static S3Configuration configuracionS3() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }
}
