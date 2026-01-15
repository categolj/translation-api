package am.ik.translation.config;

import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import am.ik.translation.github.GithubProps;
import am.ik.webhook.spring.WebhookVerifierRequestBodyAdvice;
import java.util.Set;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.backoff.ExponentialBackOff;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;
import org.zalando.logbook.spring.webflux.LogbookExchangeFilterFunction;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	private final GithubProps githubProps;

	public AppConfig(GithubProps githubProps) {
		this.githubProps = githubProps;
	}

	@Bean
	public RestClientCustomizer restClientCustomizer(
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(3_000L);
		backOff.setMaxInterval(60_000L);
		backOff.setMultiplier(2);
		return builder -> builder.requestFactory(new SimpleClientHttpRequestFactory())
			.requestInterceptor(logbookClientHttpRequestInterceptor)
			.requestInterceptor(new RetryableClientHttpRequestInterceptor(backOff, Set.of( //
					408 /* Request Timeout */, //
					425 /* Too Early */, //
					429 /* Too Many Requests */, //
					500 /* Internal Server Error */, //
					502 /* Bad Gateway */, //
					503 /* Service Unavailable */, //
					504 /* Gateway Timeout */, //
					529 /* Overloaded Error */
			), options -> options.sensitiveHeaders(Set.of(HttpHeaders.AUTHORIZATION.toLowerCase(),
					HttpHeaders.PROXY_AUTHENTICATE.toLowerCase(), HttpHeaders.COOKIE.toLowerCase(),
					HttpHeaders.SET_COOKIE.toLowerCase(), "x-amz-security-token"))));
	}

	@Bean
	public WebClientCustomizer webClientCustomizer(Logbook logbook) {
		return builder -> builder.filter(new LogbookExchangeFilterFunction(logbook));
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
