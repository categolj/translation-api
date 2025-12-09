package am.ik.translation.markdown;

/**
 * Utility class for estimating token counts in markdown text
 */
public class TokenCounter {

	// Estimate approximately 0.7 tokens per character for Japanese text
	private static final double JAPANESE_CHAR_TO_TOKEN_RATIO = 0.7;

	// Code blocks typically have different token ratio per character
	private static final double CODE_CHAR_TO_TOKEN_RATIO = 0.5;

	/**
	 * Estimates token count for markdown text
	 * @param markdown the markdown text to analyze
	 * @return estimated token count
	 */
	public int estimateTokens(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return 0;
		}

		int totalEstimatedTokens = 0;
		boolean inCodeBlock = false;

		// Process line by line
		String[] lines = markdown.split("\\n");
		for (String line : lines) {
			// Detect code block start/end
			if (line.trim().startsWith("```")) {
				inCodeBlock = !inCodeBlock;
			}

			// Apply appropriate ratio based on content type
			double ratio = inCodeBlock ? CODE_CHAR_TO_TOKEN_RATIO : JAPANESE_CHAR_TO_TOKEN_RATIO;
			totalEstimatedTokens += line.length() * ratio;
		}

		// Add safety factor
		return (int) (totalEstimatedTokens * 1.1);
	}

}
