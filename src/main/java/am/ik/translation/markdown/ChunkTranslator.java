package am.ik.translation.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import am.ik.translation.util.ResponseParser;

/**
 * Translates markdown segments with context preservation
 */
public class ChunkTranslator {

	private final ChatClient chatClient;

	private final TranslationContext context;

	private final String chatModel;

	private final Logger logger = LoggerFactory.getLogger(ChunkTranslator.class);

	/**
	 * Create a new ChunkTranslator
	 * @param chatClient Spring AI chat client
	 * @param chatModel the chat model being used
	 */
	public ChunkTranslator(ChatClient chatClient, String chatModel) {
		this.chatClient = chatClient;
		this.context = new TranslationContext();
		this.chatModel = chatModel;
	}

	/**
	 * Translate a single markdown segment
	 * @param segment the segment to translate
	 * @return translated segment
	 */
	public MarkdownSegment translate(MarkdownSegment segment) {
		// Skip translation for code blocks and frontmatter
		if (segment.type() == SegmentType.CODE_BLOCK || segment.type() == SegmentType.FRONTMATTER) {
			return segment;
		}

		String prompt = buildPrompt(segment);

		try {
			String translatedText = this.chatClient.prompt().user(prompt).call().content();

			String processedText = processTranslation(translatedText);
			MarkdownSegment translatedSegment = segment.withContent(processedText);

			// Update context with the translation
			context.updateContext(segment, translatedSegment);

			return translatedSegment;
		}
		catch (Exception e) {
			logger.error("Error translating segment: {}", e.getMessage());
			logger.debug("Segment content: {}", segment.content());
			// Return original segment on error
			return segment;
		}
	}

	/**
	 * Translate a batch of markdown segments
	 * @param segments list of segments to translate
	 * @return list of translated segments
	 */
	public List<MarkdownSegment> translateBatch(List<MarkdownSegment> segments) {
		List<MarkdownSegment> translatedSegments = new ArrayList<>();

		for (MarkdownSegment segment : segments) {
			MarkdownSegment translatedSegment = translate(segment);
			translatedSegments.add(translatedSegment);
		}

		return translatedSegments;
	}

	/**
	 * Build translation prompt for a segment
	 * @param segment the segment to translate
	 * @return prompt for translation
	 */
	private String buildPrompt(MarkdownSegment segment) {
		String segmentTypeDesc = getSegmentTypeDescription(segment.type());
		String contextPrompt = context.getContextPrompt();

		return """
				# Translation Context
				%s
				# Translation Instructions
				Please translate the following Japanese markdown text to English.
				Preserve code blocks and HTML elements in the markdown without translating them.
				Translate the %s section.
				Do not include any explanations, just translate the text directly.

				# Input Text
				%s

				# Output Format
				Return only the translation result in markdown format.
				""".formatted(contextPrompt, segmentTypeDesc, segment.content());
	}

	/**
	 * Get human-readable description of segment type
	 * @param type segment type
	 * @return description
	 */
	private String getSegmentTypeDescription(SegmentType type) {
		return switch (type) {
			case HEADING -> "heading";
			case PARAGRAPH -> "paragraph";
			case LIST -> "list";
			case TABLE -> "table";
			case CODE_BLOCK -> "code block";
			case FRONTMATTER -> "frontmatter";
			default -> "content";
		};
	}

	/**
	 * Process and clean up translation result
	 * @param rawTranslation raw translated text
	 * @return processed text
	 */
	private String processTranslation(String rawTranslation) {
		if (rawTranslation == null || rawTranslation.isEmpty()) {
			return "";
		}

		// Try to clean up any formatting the AI might have added
		String cleaned = rawTranslation.trim();

		// Remove markdown formatting that might have been added around code blocks
		cleaned = cleaned.replaceAll("```markdown\\s*", "");

		// Remove any explanatory text that might have been added
		if (cleaned.contains("# Translation Result") || cleaned.contains("# Output")
				|| cleaned.contains("# Translated Text")) {

			// Try to extract just the translation part
			ResponseParser.TitleAndContent extracted = ResponseParser.parseText(cleaned);
			if (extracted != null && extracted.content() != null && !extracted.content().isBlank()) {
				return extracted.content();
			}
		}

		return cleaned;
	}

	/**
	 * Add terminology to translation context
	 * @param source source term
	 * @param target translated term
	 */
	public void addTerminology(String source, String target) {
		context.addTerminology(source, target);
	}

}
