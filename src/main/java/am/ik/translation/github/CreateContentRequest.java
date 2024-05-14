package am.ik.translation.github;

import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;

@Builder(style = BuilderStyle.STAGED)
public record CreateContentRequest(String message, String branch, String content, @Opt String sha,
		@Opt Committer committer) {
}
