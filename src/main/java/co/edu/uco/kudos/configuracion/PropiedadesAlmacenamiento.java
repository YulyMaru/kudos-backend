package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kudos.almacenamiento")
public record PropiedadesAlmacenamiento(String tipo, String directorio, String endpoint, String region,
                                        String bucket, String claveAcceso, String claveSecreta,
                                        Duration vigenciaEnlaces) {

    public PropiedadesAlmacenamiento {
        tipo = tipo == null || tipo.isBlank() ? "local" : tipo;
        directorio = directorio == null || directorio.isBlank() ? "almacenamiento-local" : directorio;
        region = region == null || region.isBlank() ? "us-east-1" : region;
        bucket = bucket == null || bucket.isBlank() ? "kudos-adjuntos" : bucket;
        vigenciaEnlaces = vigenciaEnlaces == null ? Duration.ofMinutes(10) : vigenciaEnlaces;
    }

    public void validarConfiguracionS3() {
        if (endpoint == null || endpoint.isBlank() || claveAcceso == null || claveAcceso.isBlank()
                || claveSecreta == null || claveSecreta.isBlank()) {
            throw new IllegalStateException(
                    "Con ALMACENAMIENTO_TIPO=s3 son obligatorias S3_ENDPOINT, S3_CLAVE_ACCESO y S3_CLAVE_SECRETA");
        }
    }

    @Override
    public String toString() {
        return "PropiedadesAlmacenamiento[tipo=" + tipo + ", endpoint=" + endpoint + ", bucket=" + bucket
                + ", claveSecreta=****]";
    }
}
