package am.ik.translation.config;

import java.util.Set;

import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import am.ik.translation.github.GithubProps;
import am.ik.webhook.spring.WebhookVerifierRequestBodyAdvice;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	private final GithubProps githubProps;

	public AppConfig(GithubProps githubProps) {
		this.githubProps = githubProps;
	}

	@Bean
	public RestClientCustomizer restClientCustomizer(
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		return builder -> builder.requestFactory(new SimpleClientHttpRequestFactory())
			.requestInterceptor(logbookClientHttpRequestInterceptor)
			.requestInterceptor(new RetryableClientHttpRequestInterceptor(new FixedBackOff(2_000, 2),
					options -> options.sensitiveHeaders(Set.of(HttpHeaders.AUTHORIZATION.toLowerCase(),
							HttpHeaders.PROXY_AUTHENTICATE.toLowerCase(), HttpHeaders.COOKIE.toLowerCase(),
							HttpHeaders.SET_COOKIE.toLowerCase(), "x-amz-security-token"))));
	}

	@Bean
	public WebhookVerifierRequestBodyAdvice webhookVerifierRequestBodyAdvice() {
		return WebhookVerifierRequestBodyAdvice.githubSha256(this.githubProps.webhookSecret());
	}

	// https://github.com/spring-projects/spring-boot/issues/34622#issuecomment-2243481536
	@Bean
	public ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
		return new ContextPropagatingTaskDecorator();
	}

}
