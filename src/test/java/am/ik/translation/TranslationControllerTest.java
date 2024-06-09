package am.ik.translation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import am.ik.webhook.WebhookHttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "github.webhook-secret=opensesami", "logging.logback.ecs-encoder.enabled=false" })
class TranslationControllerTest {

	@MockBean
	TranslationService translationService;

	@Autowired
	RestClient.Builder restClientBuilder;

	@LocalServerPort
	int port;

	RestClient restClient;

	@BeforeEach
	void init() {
		if (this.restClient == null) {
			this.restClient = this.restClientBuilder.baseUrl("http://localhost:%d".formatted(port))
				.defaultHeader("X-GitHub-Event", "issues")
				.defaultStatusHandler(new DefaultResponseErrorHandler() {
					@Override
					public void handleError(ClientHttpResponse response) {

					}
				})
				.build();
		}
	}

	@Test
	void webhookOK() throws Exception {
		try (InputStream stream = new ClassPathResource("test-payload.json").getInputStream()) {
			String requestBody = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
			ResponseEntity<String> response = this.restClient.post()
				.uri("/webhook")
				.body(requestBody)
				.header(WebhookHttpHeaders.X_HUB_SIGNATURE_256,
						"sha256=c16be7733c9701d7a4645d608f91db9237acc95e5bac5820321cef6c64bfe417")
				.contentType(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("Translation is requested for 787");
			verify(this.translationService).translateAndSendPullRequest(787L, 1);
		}
	}

	@Test
	void webhookIgnoredAction() throws Exception {
		String requestBody = """
				{"action": "closed"}
				""";
		ResponseEntity<String> response = this.restClient.post()
			.uri("/webhook")
			.body(requestBody)
			.header(WebhookHttpHeaders.X_HUB_SIGNATURE_256,
					"sha256=6a2d1d2f897843f4b3a1aa341ea9c430807cf1b06b4ba5ce5cc2ce50b4393c7d")
			.contentType(MediaType.APPLICATION_JSON)
			.retrieve()
			.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(response.getBody()).isEqualTo("Ignored action: closed");
		verify(this.translationService, never()).translateAndSendPullRequest(anyLong(), anyInt());
	}

	@Test
	void webhookInvalidRepo() throws Exception {
		String requestBody = """
				{"action": "opened", "repository": {"full_name": "test"}}
				""";
		ResponseEntity<String> response = this.restClient.post()
			.uri("/webhook")
			.body(requestBody)
			.header(WebhookHttpHeaders.X_HUB_SIGNATURE_256,
					"sha256=fd861361d3426bf882627fcf5bc64049a069c78c5d6018d5301e3619dc0117a0")
			.contentType(MediaType.APPLICATION_JSON)
			.retrieve()
			.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Invalid request: test");
		verify(this.translationService, never()).translateAndSendPullRequest(anyLong(), anyInt());
	}

	@Test
	void webhookIgnoredTitle() throws Exception {
		String requestBody = """
				{"action": "opened", "repository": {"full_name": "making/ik.am_en"}, "issue":  {"title": "foo"}}
				""";
		ResponseEntity<String> response = this.restClient.post()
			.uri("/webhook")
			.body(requestBody)
			.header(WebhookHttpHeaders.X_HUB_SIGNATURE_256,
					"sha256=d73dec812fc464385afa3d4768bbec9996d159428c7de7def27d775667a218f0")
			.contentType(MediaType.APPLICATION_JSON)
			.retrieve()
			.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(response.getBody()).isEqualTo("Ignored title: foo");
		verify(this.translationService, never()).translateAndSendPullRequest(anyLong(), anyInt());
	}

}