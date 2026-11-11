package co.edu.uco.kudos;

import org.springframework.boot.SpringApplication;

public class TestKudosApplication {

	public static void main(String[] args) {
		SpringApplication.from(KudosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
