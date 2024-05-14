package am.ik.translation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TranslationApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TranslationApiApplication.class, args);
	}

}
