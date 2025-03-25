package am.ik.translation.markdown;

import java.time.LocalDateTime;

/**
 * Represents a history entry of a translation with original and translated segments
 */
public record TranslationHistory(MarkdownSegment original, MarkdownSegment translated, LocalDateTime timestamp) {
	/**
	 * Create a new TranslationHistory with the current timestamp
	 * @param original the original segment
	 * @param translated the translated segment
	 * @return a new TranslationHistory instance
	 */
	public static TranslationHistory now(MarkdownSegment original, MarkdownSegment translated) {
		return new TranslationHistory(original, translated, LocalDateTime.now());
	}
}
