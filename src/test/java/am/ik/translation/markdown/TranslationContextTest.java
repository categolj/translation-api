package am.ik.translation.markdown;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TranslationContextTest {

	@Test
	void shouldManageTerminology() {
		TranslationContext context = new TranslationContext();

		// Add terminology
		context.addTerminology("スプリング", "Spring");
		context.addTerminology("ビーン", "Bean");

		// Context should contain the terminology
		String prompt = context.getContextPrompt();
		assertThat(prompt).contains("# Translation Terminology");
		assertThat(prompt).contains("スプリング → Spring");
		assertThat(prompt).contains("ビーン → Bean");
	}

	@Test
	void shouldBuildContextFromHistory() {
		TranslationContext context = new TranslationContext();

		// Create original and translated segments
		MarkdownSegment originalHeader = new MarkdownSegment("# スプリングフレームワークの紹介", SegmentType.HEADING, 0);
		MarkdownSegment translatedHeader = new MarkdownSegment("# Introduction to Spring Framework",
				SegmentType.HEADING, 0);

		// Add to context
		context.updateContext(originalHeader, translatedHeader);

		// Get context prompt
		String prompt = context.getContextPrompt();

		// Should contain previous translation
		assertThat(prompt).contains("# Previous Translations");
		// With the "Section:" prefix that's added in the implementation
		assertThat(prompt).contains("Section: # スプリングフレームワークの紹介 → # Introduction to Spring Framework");
	}

	@Test
	void shouldLimitHistorySize() {
		TranslationContext context = new TranslationContext();

		// Add multiple translations to exceed the limit
		for (int i = 0; i < 10; i++) {
			MarkdownSegment original = new MarkdownSegment("# ヘッダー" + i, SegmentType.HEADING, i);
			MarkdownSegment translated = new MarkdownSegment("# Header" + i, SegmentType.HEADING, i);
			context.updateContext(original, translated);
		}

		// Get recent content (should not contain all 10 entries)
		String recentContent = context.getRecentTranslatedContent(1000);

		// Should not contain the first header (which would be pruned)
		assertThat(recentContent).doesNotContain("Header0");

		// But should contain more recent headers
		assertThat(recentContent).contains("Header9");
	}

	@Test
	void shouldRespectMaxCharsLimit() {
		TranslationContext context = new TranslationContext();

		// Add a fairly long translation
		MarkdownSegment original = new MarkdownSegment("これは長い段落です。日本語のテキストが含まれています。", SegmentType.PARAGRAPH, 0);

		MarkdownSegment translated = new MarkdownSegment(
				"This is a long paragraph. It contains text translated from Japanese.", SegmentType.PARAGRAPH, 0);

		context.updateContext(original, translated);

		// Get content with very small char limit
		String limitedContent = context.getRecentTranslatedContent(10);

		// Should respect the limit
		assertThat(limitedContent.length()).isLessThanOrEqualTo(10);

		// Get with larger limit
		String fullContent = context.getRecentTranslatedContent(1000);
		assertThat(fullContent).isEqualTo("This is a long paragraph. It contains text translated from Japanese.");
	}

	@Test
	void shouldSkipCodeBlocksInRecentContent() {
		TranslationContext context = new TranslationContext();

		// Add a code block
		MarkdownSegment codeOriginal = new MarkdownSegment("```java\nSystem.out.println(\"こんにちは世界\");\n```",
				SegmentType.CODE_BLOCK, 0);

		MarkdownSegment codeTranslated = new MarkdownSegment("```java\nSystem.out.println(\"Hello World\");\n```",
				SegmentType.CODE_BLOCK, 0);

		// Add a paragraph
		MarkdownSegment paraOriginal = new MarkdownSegment("これは普通のテキストです。", SegmentType.PARAGRAPH, 1);

		MarkdownSegment paraTranslated = new MarkdownSegment("This is normal text.", SegmentType.PARAGRAPH, 1);

		// Update context with both
		context.updateContext(codeOriginal, codeTranslated);
		context.updateContext(paraOriginal, paraTranslated);

		// Get recent content
		String recentContent = context.getRecentTranslatedContent(1000);

		// Should contain the paragraph but not the code block
		assertThat(recentContent).contains("This is normal text");
		assertThat(recentContent).doesNotContain("System.out.println");
	}

}
