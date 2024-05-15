package am.ik.translation;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import am.ik.translation.openai.ChatCompletionResponse;
import am.ik.translation.openai.OpenAiProps;
import am.ik.translation.util.ResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static am.ik.translation.github.CreateContentRequestBuilder.createContentRequest;
import static am.ik.translation.github.CreatePullRequestBuilder.createPullRequest;
import static am.ik.translation.openai.ChatCompletionRequestBuilder.chatCompletionRequest;
import static am.ik.translation.openai.ChatMessageBuilder.chatMessage;

@Service
public class TranslationService {

	private final RestClient restClient;

	private final OpenAiProps openAiProps;

	private final GithubProps githubProps;

	private final EntryProps entryProps;

	private final Logger logger = LoggerFactory.getLogger(TranslationService.class);

	public TranslationService(RestClient.Builder restClientBuilder, OpenAiProps openAiProps, GithubProps githubProps,
			EntryProps entryProps) {
		this.restClient = restClientBuilder.build();
		this.openAiProps = openAiProps;
		this.githubProps = githubProps;
		this.entryProps = entryProps;
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
		OpenAiProps.Options chatOptions = this.openAiProps.chat().options();
		this.restClient.post()
			.uri("%s/repos/making/ik.am_en/issues/{issueNumber}/comments".formatted(this.githubProps.apiUrl()),
					issueNumber)
			.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
			.header("X-GitHub-Api-Version", "2022-11-28")
			.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("body",
					"We will now start translating using OpenAI (%s). please wait a moment."
						.formatted(chatOptions.model())))
			.retrieve()
			.toBodilessEntity();
	}

	public Entry translate(Long entryId) {
		Entry entry = this.restClient.get()
			.uri("%s/entries/{entryId}".formatted(this.entryProps.apiUrl()), entryId)
			.retrieve()
			.body(Entry.class);
		String prompt = """
				Please translate the following Japanese blog entry into English. Both title and content are to be translated.
				The content is written in markdown.
				Please include the <code>and <pre> elements in the markdown content in the result without translating them.
				The part surrounded by ```` in markdown is the source code, so please do not translate the Japanese in that code.
				The format of the input and the output should be following format and do not include any explanations.

				== title ==
				translated title

				== content ==
				translated content (markdown)

				The input entry is the following:

				== title ==
				%s

				== content ==
				%s
				"""
			.formatted(Objects.requireNonNull(entry).frontMatter().title(), entry.content());
		OpenAiProps.Options chatOptions = this.openAiProps.chat().options();
		ChatCompletionResponse response = this.restClient.post()
			.uri("%s/v1/chat/completions".formatted(this.openAiProps.baseUrl()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(this.openAiProps.apiKey()))
			.body(chatCompletionRequest().messages(List.of(chatMessage().content(prompt).role("user").build()))
				.temperature(chatOptions.temperature())
				.model(chatOptions.model())
				.stream(chatOptions.stream())
				.build())
			.retrieve()
			.body(ChatCompletionResponse.class);
		ResponseParser.TitleAndContent titleAndContent = ResponseParser
			.parseText(Objects.requireNonNull(response).choices().get(0).message().content());
		return EntryBuilder.from(entry)
			.content(
					"""
							> ⚠️ This article was automatically translated by OpenAI (%s).
							> It may be edited eventually, but please be aware that it may contain incorrect information at this time.

							"""
						.formatted(chatOptions.model()) + titleAndContent.content())
			.frontMatter(FrontMatterBuilder.from(entry.frontMatter()).title(titleAndContent.title()).build())
			.build();
	}

	public CreatePullResponse sendPullRequest(Entry translated, int issueNumber) {
		OpenAiProps.Options chatOptions = this.openAiProps.chat().options();
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
				""".formatted(fileName, chatOptions.model(), issueNumber).trim();
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
