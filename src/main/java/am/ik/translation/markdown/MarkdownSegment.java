package am.ik.translation.markdown;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a segment of markdown content with its type and metadata.
 */
public record MarkdownSegment(String content, SegmentType type, int order, Map<String, String> metadata) {
	/**
	 * Constructor with default empty metadata.
	 */
	public MarkdownSegment(String content, SegmentType type, int order) {
		this(content, type, order, new HashMap<>());
	}

	/**
	 * Add metadata to the segment.
	 * @param key metadata key
	 * @param value metadata value
	 * @return a new MarkdownSegment with the updated metadata
	 */
	public MarkdownSegment addMetadata(String key, String value) {
		Map<String, String> newMetadata = new HashMap<>(this.metadata);
		newMetadata.put(key, value);
		return new MarkdownSegment(this.content, this.type, this.order, newMetadata);
	}

	/**
	 * Create a new segment with updated content.
	 * @param newContent the new content
	 * @return a new MarkdownSegment with updated content
	 */
	public MarkdownSegment withContent(String newContent) {
		return new MarkdownSegment(newContent, this.type, this.order, this.metadata);
	}
}
