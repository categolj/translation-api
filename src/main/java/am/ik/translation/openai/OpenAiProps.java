package am.ik.translation.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "spring.ai.openai")
public record OpenAiProps(String baseUrl, String apiKey, Chat chat) {

	public record Chat(@NestedConfigurationProperty Options options) {

	}

	public record Options(String model, double temperature, boolean stream) {

	}
}
