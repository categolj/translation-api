package am.ik.translation.entry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entry")
public record EntryProps(String apiUrl) {
}
