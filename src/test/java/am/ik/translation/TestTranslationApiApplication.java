package am.ik.translation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestTranslationApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TranslationApiApplication::main).with(TestTranslationApiApplication.class).run(args);
	}

}
