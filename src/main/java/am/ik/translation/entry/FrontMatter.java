package am.ik.translation.entry;

import java.util.List;

import org.jilt.Builder;

@Builder(toBuilder = "from")
public record FrontMatter(String title, List<Category> categories, List<Tag> tags) {

}
