FROM eclipse-temurin:25-jdk-noble AS construccion
WORKDIR /fuente
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -ntp -q package -DskipTests \
    && cp target/kudos-*.jar target/aplicacion.jar \
    && java -Djarmode=tools -jar target/aplicacion.jar extract --layers --launcher --destination target/extraido

FROM eclipse-temurin:25-jre-noble
RUN groupadd --system kudos && useradd --system --gid kudos --no-create-home --shell /usr/sbin/nologin kudos
WORKDIR /app
COPY --from=construccion --chown=kudos:kudos /fuente/target/extraido/dependencies/ ./
COPY --from=construccion --chown=kudos:kudos /fuente/target/extraido/spring-boot-loader/ ./
COPY --from=construccion --chown=kudos:kudos /fuente/target/extraido/snapshot-dependencies/ ./
COPY --from=construccion --chown=kudos:kudos /fuente/target/extraido/application/ ./
USER kudos
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
