package am.ik.translation;

import am.ik.translation.util.ResponseParser;
import am.ik.translation.util.ResponseParser.TitleAndContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseParserTest {

	@Test
	void parseText() {
		TitleAndContent titleAndContent = ResponseParser.parseText("""
				== title ==
				Hello World

				== content ==
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""");
		assertThat(titleAndContent).isEqualTo(new TitleAndContent("Hello World", """
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""".trim()));
	}

	@Test
	void parseTextWithThink() {
		TitleAndContent titleAndContent = ResponseParser.parseText("""
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
		assertThat(titleAndContent).isEqualTo(new TitleAndContent("Hello World", """
				ABCDEFGH
				IJKLMNOP
				QRSTUVWX
				YZ
				""".trim()));
	}

}