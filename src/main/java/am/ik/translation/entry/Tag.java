package am.ik.translation.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

public record Tag(String name, @Nullable @JsonInclude(JsonInclude.Include.NON_EMPTY) String version) {
	public Tag(String name) {
		this(name, null);
	}
}
