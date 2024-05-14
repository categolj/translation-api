package am.ik.translation.github;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

@Builder(style = BuilderStyle.STAGED)
public record CreatePullRequest(String title, String body, String head, String base) {
}
