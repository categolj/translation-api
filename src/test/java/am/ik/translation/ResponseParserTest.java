package am.ik.translation;

import am.ik.translation.util.ResponseParser;
import am.ik.translation.util.ResponseParser.TranslatedContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseParserTest {

	@Test
	void parseText() {
		TranslatedContent translatedContent = ResponseParser.parseText("""
				== title ==
				Hello World

				== content ==
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""");
		assertThat(translatedContent).isEqualTo(new TranslatedContent("Hello World", null, """
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""".trim()));
	}

	@Test
	void parseTextWithSummary() {
		TranslatedContent translatedContent = ResponseParser.parseText("""
				== title ==
				Hello World

				== summary ==
				This is a summary of the article.

				== content ==
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""");
		assertThat(translatedContent)
			.isEqualTo(new TranslatedContent("Hello World", "This is a summary of the article.", """
					ABCDEFGH
					IJKLMNOP
					QRSTUVWX
					YZ
					""".trim()));
	}

	@Test
	void parseTextWithThink() {
		TranslatedContent translatedContent = ResponseParser.parseText("""
				<think>
				This is a thinking part.
				This part should be removed.
				</think>
				== title ==
				Hello World

				== content ==
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""");
		assertThat(translatedContent).isEqualTo(new TranslatedContent("Hello World", null, """
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""".trim()));
	}

	@Test
	void parseTextWithSummaryAndThink() {
		TranslatedContent translatedContent = ResponseParser.parseText("""
				<think>
				This is a thinking part.
				This part should be removed.
				</think>
				== title ==
				Hello World

				== summary ==
				This is a summary of the article.

				== content ==
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""");
		assertThat(translatedContent)
			.isEqualTo(new TranslatedContent("Hello World", "This is a summary of the article.", """
					ABCDEFGH
					IJKLMNOP
					QRSTUVWX
					YZ
					""".trim()));
	}

}