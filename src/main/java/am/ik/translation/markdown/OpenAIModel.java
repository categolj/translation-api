package am.ik.translation.markdown;

/**
 * Enumeration of OpenAI models with their token limits
 */
public enum OpenAIModel {

	GPT_3_5_TURBO("gpt-3.5-turbo", 4096), GPT_4("gpt-4", 8192), GPT_4_TURBO("gpt-4-turbo", 128000),
	GPT_4O_MINI("gpt-4o-mini", 16000); // Default model

	private final String modelName;

	private final int maxTokens;

	OpenAIModel(String modelName, int maxTokens) {
		this.modelName = modelName;
		this.maxTokens = maxTokens;
	}

	/**
	 * Calculate actual usable tokens, reserving space for response
	 * @return maximum tokens that can be used for input
	 */
	public int getMaxInputTokens() {
		return (int) (maxTokens * 0.7); // Reserve 30% for response
	}

	/**
	 * Get the model name
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Get the total maximum tokens for the model
	 * @return maximum tokens
	 */
	public int getMaxTokens() {
		return maxTokens;
	}

	/**
	 * Find OpenAIModel by model name
	 * @param modelName the model name to find
	 * @return the corresponding OpenAIModel or GPT_4O_MINI if not found
	 */
	public static OpenAIModel fromModelName(String modelName) {
		for (OpenAIModel model : values()) {
			if (model.getModelName().equals(modelName)) {
				return model;
			}
		}
		return GPT_4O_MINI; // Default model
	}

}
