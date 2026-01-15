package am.ik.translation.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages context for translation to ensure consistency across chunks
 */
public class TranslationContext {

	private final Map<String, String> terminology;

	private final List<TranslationHistory> history;

	private static final int MAX_CONTEXT_HISTORY = 3;

	/**
	 * Create a new TranslationContext
	 */
	public TranslationContext() {
		this.terminology = new HashMap<>();
		this.history = new ArrayList<>();
	}

	/**
	 * Add terminology mapping
	 * @param source source term
	 * @param target translated term
	 */
	public void addTerminology(String source, String target) {
		terminology.put(source, target);
	}

	/**
	 * Update context with a new translation
	 * @param original original segment
	 * @param translated translated segment
	 */
	public void updateContext(MarkdownSegment original, MarkdownSegment translated) {
		TranslationHistory entry = TranslationHistory.now(original, translated);
		history.add(entry);

		// Keep only the most recent history entries
		if (history.size() > MAX_CONTEXT_HISTORY) {
			history.remove(0);
		}
	}

	/**
	 * Get the translation context as a prompt
	 * @return context information formatted for inclusion in a prompt
	 */
	public String getContextPrompt() {
		StringBuilder contextPrompt = new StringBuilder();

		// Add terminology if any
		if (!terminology.isEmpty()) {
			contextPrompt.append("# Translation Terminology\n");
			terminology.forEach((source,
					target) -> contextPrompt.append("- ").append(source).append(" → ").append(target).append("\n"));
			contextPrompt.append("\n");
		}

		// Add previous translation context if any
		if (!history.isEmpty()) {
			contextPrompt.append("# Previous Translations\n");
			List<TranslationHistory> recentHistory = history.subList(Math.max(0, history.size() - MAX_CONTEXT_HISTORY),
					history.size());

			for (TranslationHistory entry : recentHistory) {
				// For HEADING type, include the heading text as context
				if (entry.original().type() == SegmentType.HEADING) {
					contextPrompt.append("Section: ")
						.append(entry.original().content().trim())
						.append(" → ")
						.append(entry.translated().content().trim())
						.append("\n");
				}
			}

			contextPrompt.append("\n");
		}

		return contextPrompt.toString();
	}

	/**
	 * Get the most recent translations for context
	 * @param maxChars maximum number of characters to include
	 * @return recent translated content
	 */
	public String getRecentTranslatedContent(int maxChars) {
		if (history.isEmpty()) {
			return "";
		}

		// Build content from the most recent entries, respecting the maxChars limit
		StringBuilder content = new StringBuilder();
		for (int i = history.size() - 1; i >= 0; i--) {
			TranslationHistory entry = history.get(i);
			String translatedContent = entry.translated().content();

			// Skip code blocks in context, they're less relevant for translation
			// consistency
			if (entry.translated().type() == SegmentType.CODE_BLOCK) {
				continue;
			}

			// Prepend each entry (we're going backwards)
			// If adding this would exceed our limit, stop
			if (translatedContent.length() + content.length() > maxChars) {
				break;
			}

			content.insert(0, translatedContent + "\n\n");
		}

		return content.toString().trim();
	}

}
