package am.ik.translation.openai;

import java.util.List;

public record ChatCompletionResponse(String id, String object, long created, String model,
		List<ChatResponseChoice> choices, boolean stream, double temperature) {
}
