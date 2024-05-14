package am.ik.translation.entry;

import java.time.OffsetDateTime;

import jakarta.annotation.Nullable;
import org.jilt.Builder;

@Builder
public record Author(String name, @Nullable OffsetDateTime date) {
}
