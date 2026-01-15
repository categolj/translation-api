package am.ik.translation.translation.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import am.ik.translation.github.IssueEvent;
import am.ik.translation.translation.TranslationService;
import am.ik.webhook.annotation.WebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranslationController {

	private final TranslationService translationService;

	private final ObjectMapper objectMapper;

	private final Logger log = LoggerFactory.getLogger(TranslationController.class);

	final Pattern titlePattern = Pattern.compile("Translation Request to (\\d+)");

	public TranslationController(TranslationService translationService, ObjectMapper objectMapper) {
		this.translationService = translationService;
		this.objectMapper = objectMapper;
	}

	@PostMapping(path = "webhook", headers = "X-GitHub-Event=issues")
	public ResponseEntity<String> webhook(@WebhookPayload @RequestBody String payload) throws Exception {
		IssueEvent issueEvent = this.objectMapper.readValue(payload, IssueEvent.class);
		log.info("Received {}", issueEvent);
		if (!"opened".equals(issueEvent.action())) {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("Ignored action: " + issueEvent.action());
		}
		if (!"making/ik.am_en".equals(issueEvent.repository().fullName())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("Invalid request: " + issueEvent.repository().fullName());
		}
		IssueEvent.Issue issue = issueEvent.issue();
		Matcher matcher = this.titlePattern.matcher(issue.title());
		if (matcher.matches()) {
			long entryId = Long.parseLong(matcher.group(1));
			this.translationService.translateAndSendPullRequest(entryId, issue.number());
			return ResponseEntity.ok("Translation is requested for " + entryId);
		}
		else {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("Ignored title: " + issue.title());
		}
	}

}
