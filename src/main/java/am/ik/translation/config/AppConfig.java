package am.ik.translation.config;

import am.ik.accesslogger.AccessLogger;
import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import am.ik.translation.github.GithubProps;
import am.ik.webhook.spring.WebhookVerifierRequestBodyAdvice;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	private final GithubProps githubProps;

	public AppConfig(GithubProps githubProps) {
		this.githubProps = githubProps;
	}

	@Bean
	public AccessLogger accessLogger() {
		return new AccessLogger(httpExchange -> {
			String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator")
					|| uri.startsWith("/cloudfoundryapplication"));
		});
	}

	@Bean
	public RestClientCustomizer restClientCustomizer() {
		return builder -> builder
			.requestInterceptor(new RetryableClientHttpRequestInterceptor(new FixedBackOff(2_000, 2)));
	}

	@Bean
	public WebhookVerifierRequestBodyAdvice webhookVerifierRequestBodyAdvice() {
		return WebhookVerifierRequestBodyAdvice.githubSha256(this.githubProps.webhookSecret());
	}

}
