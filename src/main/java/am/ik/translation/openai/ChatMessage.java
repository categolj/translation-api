package am.ik.translation.openai;

import org.jilt.Builder;

@Builder
public record ChatMessage(String content, String role) {
}
