package am.ik.translation.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jilt.Builder;

@Builder(toBuilder = "from")
public record Entry(Long entryId, FrontMatter frontMatter, String content, Author created, Author updated) {

	public String toMarkdown() {
		return """
				---
				title: %s
				tags: %s
				categories: %s%s%s%s
				---

				%s
				""".formatted(frontMatter.title(),
				frontMatter.tags().stream().map(t -> "\"%s\"".formatted(t.name())).toList(),
				frontMatter.categories().stream().map(c -> "\"%s\"".formatted(c.name())).toList(),
				frontMatter.summary() == null ? "" : "%nsummary: %s".formatted(frontMatter.summary()),
				created.date() == null ? "" : "%ndate: %s".formatted(created.date()),
				updated.date() == null ? "" : "%nupdated: %s".formatted(updated.date()), content);
	}

	@JsonIgnore
	public String formatId() {
		return "%05d".formatted(entryId);
	}
}
