package am.ik.translation.translation;

import am.ik.translation.entry.Entry;
import am.ik.translation.entry.EntryBuilder;
import am.ik.translation.entry.EntryProps;
import am.ik.translation.entry.FrontMatterBuilder;
import am.ik.translation.github.Commit;
import am.ik.translation.github.Committer;
import am.ik.translation.github.CreateBranchRequest;
import am.ik.translation.github.CreateBranchResponse;
import am.ik.translation.github.CreateContentRequestBuilders;
import am.ik.translation.github.CreatePullResponse;
import am.ik.translation.github.GithubProps;
import am.ik.translation.util.ResponseParser;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static am.ik.translation.github.CreateContentRequestBuilder.createContentRequest;
import static am.ik.translation.github.CreatePullRequestBuilder.createPullRequest;

@Service
public class TranslationService {

	private final RestClient restClient;

	private final GithubProps githubProps;

	private final EntryProps entryProps;

	private final ChatClient chatClient;

	private final String chatModel;

	private final Logger logger = LoggerFactory.getLogger(TranslationService.class);

	public TranslationService(RestClient.Builder restClientBuilder, GithubProps githubProps, EntryProps entryProps,
			ChatClient.Builder chatClientBuilder,
			@Value("${spring.ai.openai.chat.options.model:N/A}") String chatModel) {
		this.restClient = restClientBuilder.build();
		this.githubProps = githubProps;
		this.entryProps = entryProps;
		this.chatClient = chatClientBuilder.build();
		this.chatModel = chatModel;
	}

	@Async
	public void translateAndSendPullRequest(Long entryId, int issueNumber) {
		this.sendComment(issueNumber);
		Entry translated = this.translate(entryId);
		logger.info("Translated {}", translated.entryId());
		CreatePullResponse createPullResponse = this.sendPullRequest(translated, issueNumber);
		logger.info("Sent a pull request: {}", createPullResponse.html_url());
	}

	public void sendComment(int issueNumber) {
		this.restClient.post()
			.uri("%s/repos/making/ik.am_en/issues/{issueNumber}/comments".formatted(this.githubProps.apiUrl()),
					issueNumber)
			.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
			.header("X-GitHub-Api-Version", "2022-11-28")
			.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("body",
					"We will now start translating using OpenAI API (%s). please wait a moment."
						.formatted(this.chatModel)))
			.retrieve()
			.toBodilessEntity();
	}

	public Entry translate(Long entryId) {
		Entry entry = Objects.requireNonNull(this.restClient.get()
			.uri("%s/entries/{entryId}".formatted(this.entryProps.apiUrl()), entryId)
			.retrieve()
			.body(Entry.class));
		logger.info("Start translation entryId={}, model={}", entryId, chatModel);
		String text = this.chatClient.prompt()
			.system("""
					You are a skilled Japanese-to-English translator, specializing in technical documentation translation.

					Please translate the user's input which ia a Japanese blog entry into English. Both title and content are to be translated.
					The content is written in markdown.
					Please include the <code>and <pre> elements in the markdown content in the result without translating them.
					The part surrounded by ```` in markdown is the source code, so please do not translate the Japanese in that code.
					The format of the input and the output should be following format and do not include any explanations.

					== title ==
					translated title

					== content ==
					translated content (markdown)
					""")
			.user(u -> u.text("""
					== title ==
					{title}

					== content ==
					{content}
					""").param("title", entry.frontMatter().title()).param("content", entry.content()))
			.stream()
			.content()
			.collectList()
			.map(list -> String.join("", list))
			.block();
		ResponseParser.TitleAndContent titleAndContent = ResponseParser.parseText(Objects.requireNonNull(text));
		return EntryBuilder.from(entry)
			.content(
					"""
							> ⚠️ This article was automatically translated by OpenAI API (%s).
							> It may be edited eventually, but please be aware that it may contain incorrect information at this time.

							"""
						.formatted(this.chatModel) + titleAndContent.content())
			.frontMatter(FrontMatterBuilder.from(entry.frontMatter()).title(titleAndContent.title()).build())
			.build();
	}

	public CreatePullResponse sendPullRequest(Entry translated, int issueNumber) {
		CreateBranchResponse branchResponse = this.restClient.get()
			.uri("%s/repos/making/ik.am_en/branches/main".formatted(this.githubProps.apiUrl()))
			.retrieve()
			.body(CreateBranchResponse.class);
		String latestSha = Objects.requireNonNull(branchResponse).commit().sha();
		String branchName = "translation-" + translated.entryId() + "-" + Instant.now().getEpochSecond();
		this.restClient.post()
			.uri("%s/repos/making/ik.am_en/git/refs".formatted(this.githubProps.apiUrl()))
			.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
			.body(new CreateBranchRequest("refs/heads/" + branchName, latestSha))
			.retrieve()
			.toBodilessEntity();
		String fileName = "%s.md".formatted(translated.formatId());
		String commitMessage = """
				Translate %s by OpenAI (%s)

				closes gh-%d
				""".formatted(fileName, this.chatModel, issueNumber).trim();
		CreateContentRequestBuilders.Optionals ccrBuilder = createContentRequest().message(commitMessage)
			.branch(branchName)
			.content(Base64.getEncoder().encodeToString(translated.toMarkdown().getBytes(StandardCharsets.UTF_8)))
			.committer(new Committer("Translation Bot", "makingx+bot@gmail.com"));
		try {
			Commit latestCommit = this.restClient.get()
				.uri("%s/repos/making/ik.am_en/contents/content/{fileName}".formatted(this.githubProps.apiUrl()),
						fileName)
				.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
				.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
				.retrieve()
				.body(Commit.class);
			ccrBuilder.sha(Objects.requireNonNull(latestCommit).sha());
		}
		catch (HttpClientErrorException.NotFound notFound) {
			// ignore
		}
		this.restClient.put()
			.uri("%s/repos/making/ik.am_en/contents/content/{fileName}".formatted(this.githubProps.apiUrl()), fileName)
			.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
			.body(ccrBuilder.build())
			.retrieve()
			.toBodilessEntity();
		return this.restClient.post()
			.uri("%s/repos/making/ik.am_en/pulls".formatted(this.githubProps.apiUrl()))
			.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
			.body(createPullRequest().title(commitMessage).body("""
					translated https://github.com/making/blog.ik.am/blob/master/content/%s.md

					closes gh-%d
					""".formatted(translated.formatId(), issueNumber)).head(branchName).base("main").build())
			.retrieve()
			.body(CreatePullResponse.class);
	}

}
