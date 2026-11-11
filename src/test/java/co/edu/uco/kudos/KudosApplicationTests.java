package co.edu.uco.kudos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "kudos.seguridad.jwt-secreto=secreto-de-pruebas-con-al-menos-32-bytes")
class KudosApplicationTests {

	@Test
	void contextLoads() {
	}

}
