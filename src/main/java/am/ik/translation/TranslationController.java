package am.ik.translation;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import am.ik.translation.entry.EntryBuilder;
import am.ik.translation.entry.EntryProps;
import am.ik.translation.entry.FrontMatterBuilder;
import am.ik.translation.github.GithubProps;
import am.ik.translation.openai.OpenAiProps;
import am.ik.translation.openai.OpenAiProps.Options;
import am.ik.translation.util.ResponseParser;
import am.ik.translation.util.ResponseParser.TitleAndContent;
import am.ik.translation.entry.Entry;
import am.ik.translation.github.Commit;
import am.ik.translation.github.Committer;
import am.ik.translation.github.CreateBranchRequest;
import am.ik.translation.github.CreateBranchResponse;
import am.ik.translation.github.CreateContentRequestBuilders;
import am.ik.translation.github.CreatePullResponse;
import am.ik.translation.openai.ChatCompletionResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static am.ik.translation.github.CreateContentRequestBuilder.createContentRequest;
import static am.ik.translation.github.CreatePullRequestBuilder.createPullRequest;
import static am.ik.translation.openai.ChatCompletionRequestBuilder.chatCompletionRequest;
import static am.ik.translation.openai.ChatMessageBuilder.chatMessage;

@RestController
public class TranslationController {

	private final RestClient restClient;

	private final OpenAiProps openAiProps;

	private final GithubProps githubProps;

	private final EntryProps entryProps;

	public TranslationController(RestClient.Builder restClientBuilder, OpenAiProps openAiProps, GithubProps githubProps,
			EntryProps entryProps) {
		this.restClient = restClientBuilder.build();
		this.openAiProps = openAiProps;
		this.githubProps = githubProps;
		this.entryProps = entryProps;
	}

	@GetMapping(path = "/entries/{entryId}")
	public Object translate(@PathVariable Long entryId) {
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
		Options chatOptions = this.openAiProps.chat().options();
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
		TitleAndContent titleAndContent = ResponseParser
			.parseText(Objects.requireNonNull(response).choices().get(0).message().content());
		Entry translated = EntryBuilder.from(entry)
			.content(
					"""
							> ⚠️ This article was automatically translated by OpenAI (%s).
							> It may be edited eventually, but please be aware that it may contain incorrect information at this time.

							"""
						.formatted(chatOptions.model()) + titleAndContent.content())
			.frontMatter(FrontMatterBuilder.from(entry.frontMatter()).title(titleAndContent.title()).build())
			.build();

		CreateBranchResponse branchResponse = this.restClient.get()
			.uri("%s/repos/making/ik.am_en/branches/main".formatted(this.githubProps.apiUrl()))
			.retrieve()
			.body(CreateBranchResponse.class);
		String latestSha = Objects.requireNonNull(branchResponse).commit().sha();
		String branchName = "translation-" + entry.entryId() + "-" + Instant.now().getEpochSecond();
		this.restClient.post()
			.uri("%s/repos/making/ik.am_en/git/refs".formatted(this.githubProps.apiUrl()))
			.header(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.header(HttpHeaders.AUTHORIZATION, "token %s".formatted(this.githubProps.accessToken()))
			.body(new CreateBranchRequest("refs/heads/" + branchName, latestSha))
			.retrieve()
			.toBodilessEntity();
		String fileName = "%s.md".formatted(entry.formatId());
		String commitMessage = "Translate %s by OpenAI (%s)".formatted(fileName, chatOptions.model());
		CreateContentRequestBuilders.Optionals ccrBuilder = createContentRequest().message(commitMessage)
			.branch(branchName)
			.content(Base64.getEncoder().encodeToString(translated.toMarkdown().getBytes(StandardCharsets.UTF_8)))
			.committer(new Committer("Translation Bot", "makingx+bot@gmail.com"));
		try {
			Commit latestCommit = this.restClient.get()
				.uri("%s/repos/making/ik.am_en/contents/content/{fileName}".formatted(this.githubProps.apiUrl()), fileName)
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
			.body(createPullRequest().title(commitMessage)
				.body("translated https://github.com/making/blog.ik.am/blob/master/content/%s.md"
					.formatted(entry.formatId()))
				.head(branchName)
				.base("main")
				.build())
			.retrieve()
			.body(CreatePullResponse.class);
	}

}
