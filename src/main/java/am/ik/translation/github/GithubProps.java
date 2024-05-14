package am.ik.translation.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GithubProps(String apiUrl, String accessToken) {
}
