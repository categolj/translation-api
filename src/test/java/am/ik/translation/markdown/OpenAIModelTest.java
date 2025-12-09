package am.ik.translation.markdown;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple unit tests for OpenAIModel
 */
class OpenAIModelTest {

	@Test
	void shouldReturnCorrectMaxInputTokens() {
		// Max input tokens should be 70% of the total max tokens
		assertThat(OpenAIModel.GPT_3_5_TURBO.getMaxInputTokens()).isEqualTo((int) (4096 * 0.7));
		assertThat(OpenAIModel.GPT_4.getMaxInputTokens()).isEqualTo((int) (8192 * 0.7));
		assertThat(OpenAIModel.GPT_4O_MINI.getMaxInputTokens()).isEqualTo((int) (16000 * 0.7));
	}

	@Test
	void shouldFindModelByName() {
		assertThat(OpenAIModel.fromModelName("gpt-3.5-turbo")).isEqualTo(OpenAIModel.GPT_3_5_TURBO);
		assertThat(OpenAIModel.fromModelName("gpt-4")).isEqualTo(OpenAIModel.GPT_4);
		assertThat(OpenAIModel.fromModelName("gpt-4o-mini")).isEqualTo(OpenAIModel.GPT_4O_MINI);

		// Should default to GPT_4O_MINI for unknown models
		assertThat(OpenAIModel.fromModelName("unknown-model")).isEqualTo(OpenAIModel.GPT_4O_MINI);
	}

	@Test
	void shouldProvideModelName() {
		assertThat(OpenAIModel.GPT_3_5_TURBO.getModelName()).isEqualTo("gpt-3.5-turbo");
		assertThat(OpenAIModel.GPT_4.getModelName()).isEqualTo("gpt-4");
		assertThat(OpenAIModel.GPT_4O_MINI.getModelName()).isEqualTo("gpt-4o-mini");
	}

}
