package am.ik.translation.markdown;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSizeAnalyzerTest {

	private final OpenAIModel testModel = OpenAIModel.GPT_3_5_TURBO; // Using the smallest
																		// model for
																		// predictable
																		// tests

	private final MarkdownSizeAnalyzer analyzer = new MarkdownSizeAnalyzer(testModel);

	@Test
	void shouldIdentifySmallMarkdown() {
		String smallMarkdown = """
				# 小さい記事

				これは短い記事です。トークン数はとても少ないでしょう。
				""";

		boolean needsSplit = analyzer.isTooBig(smallMarkdown);
		assertThat(needsSplit).isFalse();

		MarkdownSizeAnalyzer.SizeAnalysisResult result = analyzer.analyzeSize(smallMarkdown);
		assertThat(result.needsSplit()).isFalse();
		assertThat(result.estimatedTokens()).isLessThan(result.maxAllowedTokens());
		assertThat(result.usagePercentage()).isLessThan(10.0); // Small text should use
																// only a tiny percentage
	}

	@Test
	void shouldCalculatePercentage() {
		// Create a text that's roughly 10% of the model's capacity
		StringBuilder largeText = new StringBuilder();
		largeText.append("# 長い記事\n\n");

		// Add enough paragraphs to make it substantial
		for (int i = 0; i < 20; i++) {
			largeText.append("これは段落").append(i).append("です。適度に長いテキストを追加して、トークン数を増やします。\n\n");
			largeText.append("Additional English text to increase token count sufficiently.\n\n");
		}

		MarkdownSizeAnalyzer.SizeAnalysisResult result = analyzer.analyzeSize(largeText.toString());

		// The percentage should be a reasonable value between 0 and 100
		assertThat(result.usagePercentage()).isGreaterThan(0.0);
		assertThat(result.usagePercentage()).isLessThan(100.0);
	}

	@Test
	void shouldRespectModelLimits() {
		// Create tests for different models to ensure they have different thresholds
		MarkdownSizeAnalyzer gpt35Analyzer = new MarkdownSizeAnalyzer(OpenAIModel.GPT_3_5_TURBO);
		MarkdownSizeAnalyzer gpt4Analyzer = new MarkdownSizeAnalyzer(OpenAIModel.GPT_4);

		// Same markdown content should be evaluated differently based on model capacity
		StringBuilder moderateText = new StringBuilder();
		moderateText.append("# 中程度の長さの記事\n\n");

		// Add enough paragraphs to make it substantial
		for (int i = 0; i < 40; i++) {
			moderateText.append("これは段落").append(i).append("です。適度に長いテキストを追加して、トークン数を増やします。\n\n");
			moderateText.append("Additional English text to increase token count sufficiently. ");
			moderateText.append("More text to ensure we're approaching token limits for smaller models.\n\n");
		}

		String content = moderateText.toString();

		// The same content may need splitting for smaller models but not larger ones
		MarkdownSizeAnalyzer.SizeAnalysisResult gpt35Result = gpt35Analyzer.analyzeSize(content);
		MarkdownSizeAnalyzer.SizeAnalysisResult gpt4Result = gpt4Analyzer.analyzeSize(content);

		// Absolute token count should be the same
		assertThat(gpt35Result.estimatedTokens()).isEqualTo(gpt4Result.estimatedTokens());

		// But percentage usage should differ based on model capacity
		assertThat(gpt35Result.usagePercentage()).isGreaterThan(gpt4Result.usagePercentage());
	}

}
