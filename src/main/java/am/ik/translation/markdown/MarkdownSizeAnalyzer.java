package am.ik.translation.markdown;

/**
 * Analyzes markdown size and determines if chunking is necessary
 */
public class MarkdownSizeAnalyzer {

	private final TokenCounter tokenCounter;

	private final OpenAIModel model;

	// Threshold for splitting (max input tokens minus margin)
	private final int SPLIT_THRESHOLD;

	/**
	 * Create a new MarkdownSizeAnalyzer
	 * @param model the OpenAI model to use
	 */
	public MarkdownSizeAnalyzer(OpenAIModel model) {
		this.tokenCounter = new TokenCounter();
		this.model = model;
		this.SPLIT_THRESHOLD = model.getMaxInputTokens() - 500; // 500 token margin
	}

	/**
	 * Returns true if the markdown needs to be split into chunks
	 * @param markdown the markdown content to analyze
	 * @return true if chunking is needed
	 */
	public boolean isTooBig(String markdown) {
		int estimatedTokens = tokenCounter.estimateTokens(markdown);
		return estimatedTokens > SPLIT_THRESHOLD;
	}

	/**
	 * Returns size analysis result for the given markdown
	 * @param markdown the markdown content to analyze
	 * @return result of the size analysis
	 */
	public SizeAnalysisResult analyzeSize(String markdown) {
		int estimatedTokens = tokenCounter.estimateTokens(markdown);
		boolean needsSplit = estimatedTokens > SPLIT_THRESHOLD;
		double usagePercentage = (double) estimatedTokens / model.getMaxInputTokens() * 100;

		return new SizeAnalysisResult(estimatedTokens, model.getMaxInputTokens(), needsSplit, usagePercentage);
	}

	/**
	 * Record containing size analysis results
	 */
	public record SizeAnalysisResult(int estimatedTokens, int maxAllowedTokens, boolean needsSplit,
			double usagePercentage) {
	}

}
