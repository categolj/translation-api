package am.ik.translation.markdown;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TokenCounterTest {

	private final TokenCounter tokenCounter = new TokenCounter();

	@Test
	void shouldEstimateTokensForSimpleText() {
		String text = "これは日本語のテキストです。英語のトークン数より多くなるはずです。";
		int tokens = tokenCounter.estimateTokens(text);
		assertThat(tokens).isGreaterThan(0);
		assertThat(tokens).isLessThan(text.length()); // Japanese tokens are estimated at
														// ~0.7 per char
	}

	@Test
	void shouldEstimateTokensForMarkdown() {
		String markdown = """
				# これはマークダウンのヘッダーです

				これは通常の段落です。日本語で書かれています。

				## サブヘッダー

				- リスト項目1
				- リスト項目2

				```java
				// This is a code block
				public class Test {
				    public static void main(String[] args) {
				        System.out.println("Hello World");
				    }
				}
				```

				通常のテキストに戻りました。
				""";

		int tokens = tokenCounter.estimateTokens(markdown);
		assertThat(tokens).isGreaterThan(0);
	}

	@Test
	void shouldHandleCodeBlocksDifferently() {
		String textOnly = "これは日本語のテキストです。50文字くらいあります。これは日本語のテキストです。";
		String codeBlock = """
				```java
				// This is a code block with similar length
				public class Test {
				    public static void main(String[] args) {
				        System.out.println("Hello World");
				    }
				}
				```
				""";

		int textTokens = tokenCounter.estimateTokens(textOnly);
		int codeTokens = tokenCounter.estimateTokens(codeBlock);

		// Code blocks should have different token estimates compared to normal text
		assertThat(textTokens).isNotEqualTo(codeTokens);
	}

	@Test
	void shouldHandleEmptyOrNullInput() {
		assertThat(tokenCounter.estimateTokens("")).isEqualTo(0);
		assertThat(tokenCounter.estimateTokens(null)).isEqualTo(0);
	}

}
