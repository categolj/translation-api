package am.ik.translation.openai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jilt.Builder;

@Builder
public record ChatCompletionRequest(List<ChatMessage> messages, String model, boolean stream, double temperature,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) @JsonProperty("max_tokens") Integer maxTokens) {
}
