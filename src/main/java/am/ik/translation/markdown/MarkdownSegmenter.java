package am.ik.translation.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Segments markdown content into logical chunks for translation
 */
public class MarkdownSegmenter {

	private final TokenCounter tokenCounter;

	private final int maxTokensPerSegment;

	private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");

	private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*$");

	private static final Pattern CODE_BLOCK_START_PATTERN = Pattern.compile("^```.*$");

	private static final Pattern CODE_BLOCK_END_PATTERN = Pattern.compile("^```\\s*$");

	private static final Pattern LIST_PATTERN = Pattern.compile("^[\\s]*[\\*\\-\\+]\\s+.*$");

	private static final Pattern TABLE_PATTERN = Pattern.compile("^\\s*\\|.*\\|\\s*$");

	/**
	 * Create a new MarkdownSegmenter with the default max tokens per segment
	 */
	public MarkdownSegmenter() {
		this(4000); // Default max tokens per segment
	}

	/**
	 * Create a new MarkdownSegmenter with a specific max tokens per segment
	 * @param maxTokensPerSegment maximum tokens per segment
	 */
	public MarkdownSegmenter(int maxTokensPerSegment) {
		this.tokenCounter = new TokenCounter();
		this.maxTokensPerSegment = maxTokensPerSegment;
	}

	/**
	 * Segment markdown into logical chunks for translation
	 * @param markdown the markdown content to segment
	 * @return list of markdown segments
	 */
	public List<MarkdownSegment> segment(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return List.of();
		}

		List<MarkdownSegment> segments = new ArrayList<>();
		String[] lines = markdown.split("\\n");

		StringBuilder currentSegment = new StringBuilder();
		SegmentType currentType = null;
		boolean inCodeBlock = false;
		boolean inFrontMatter = false;
		int segmentOrder = 0;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String trimmedLine = line.trim();

			// Handle frontmatter
			if (FRONTMATTER_PATTERN.matcher(trimmedLine).matches()) {
				if (!inFrontMatter && i == 0) {
					// Start of frontmatter
					inFrontMatter = true;
					currentType = SegmentType.FRONTMATTER;
					currentSegment.append(line).append("\n");
				}
				else if (inFrontMatter) {
					// End of frontmatter
					inFrontMatter = false;
					currentSegment.append(line).append("\n");
					segments
						.add(new MarkdownSegment(currentSegment.toString(), SegmentType.FRONTMATTER, segmentOrder++));
					currentSegment = new StringBuilder();
					currentType = null;
				}
				else {
					// Normal horizontal rule or similar
					handleNormalLine(line, segments, currentSegment, currentType, segmentOrder);
				}
				continue;
			}

			if (inFrontMatter) {
				// Continue collecting frontmatter
				currentSegment.append(line).append("\n");
				continue;
			}

			// Handle code blocks
			if (CODE_BLOCK_START_PATTERN.matcher(trimmedLine).matches() && !inCodeBlock) {
				// Start of code block
				if (currentType != null && currentSegment.length() > 0) {
					// Finish previous segment
					segments.add(new MarkdownSegment(currentSegment.toString(), currentType, segmentOrder++));
					currentSegment = new StringBuilder();
				}
				inCodeBlock = true;
				currentType = SegmentType.CODE_BLOCK;
				currentSegment.append(line).append("\n");
				continue;
			}

			if (CODE_BLOCK_END_PATTERN.matcher(trimmedLine).matches() && inCodeBlock) {
				// End of code block
				currentSegment.append(line).append("\n");
				segments.add(new MarkdownSegment(currentSegment.toString(), SegmentType.CODE_BLOCK, segmentOrder++));
				currentSegment = new StringBuilder();
				currentType = null;
				inCodeBlock = false;
				continue;
			}

			if (inCodeBlock) {
				// Continue collecting code block
				currentSegment.append(line).append("\n");
				continue;
			}

			// Handle headings - always start a new segment
			Matcher headingMatcher = HEADING_PATTERN.matcher(trimmedLine);
			if (headingMatcher.matches()) {
				if (currentType != null && currentSegment.length() > 0) {
					// Finish previous segment
					segments.add(new MarkdownSegment(currentSegment.toString(), currentType, segmentOrder++));
					currentSegment = new StringBuilder();
				}
				currentType = SegmentType.HEADING;
				currentSegment.append(line).append("\n");
				continue;
			}

			// Handle other elements
			if (isNewSegmentNeeded(line, currentType)) {
				if (currentType != null && currentSegment.length() > 0) {
					segments.add(new MarkdownSegment(currentSegment.toString(), currentType, segmentOrder++));
					currentSegment = new StringBuilder();
				}
				currentType = detectSegmentType(line);
				currentSegment.append(line).append("\n");
			}
			else {
				// Continue with current segment
				if (currentType == null) {
					currentType = detectSegmentType(line);
				}
				currentSegment.append(line).append("\n");

				// Check if the current segment is getting too large
				int currentTokenCount = tokenCounter.estimateTokens(currentSegment.toString());
				if (currentTokenCount > maxTokensPerSegment && currentType != SegmentType.CODE_BLOCK) {
					segments.add(new MarkdownSegment(currentSegment.toString(), currentType, segmentOrder++));
					currentSegment = new StringBuilder();
					currentType = null;
				}
			}
		}

		// Add the last segment if not empty
		if (currentSegment.length() > 0) {
			segments.add(new MarkdownSegment(currentSegment.toString(),
					currentType != null ? currentType : SegmentType.PARAGRAPH, segmentOrder));
		}

		return segments;
	}

	/**
	 * Handle a normal line that's not part of special segments
	 */
	private void handleNormalLine(String line, List<MarkdownSegment> segments, StringBuilder currentSegment,
			SegmentType currentType, int segmentOrder) {
		if (isNewSegmentNeeded(line, currentType)) {
			if (currentType != null && currentSegment.length() > 0) {
				segments.add(new MarkdownSegment(currentSegment.toString(), currentType, segmentOrder));
				currentSegment.setLength(0);
				currentType = detectSegmentType(line);
			}
		}
		currentSegment.append(line).append("\n");
	}

	/**
	 * Detect the type of segment based on the line content
	 * @param line line of text to analyze
	 * @return detected segment type
	 */
	private SegmentType detectSegmentType(String line) {
		String trimmedLine = line.trim();

		if (HEADING_PATTERN.matcher(trimmedLine).matches()) {
			return SegmentType.HEADING;
		}

		if (LIST_PATTERN.matcher(trimmedLine).matches()) {
			return SegmentType.LIST;
		}

		if (TABLE_PATTERN.matcher(trimmedLine).matches()) {
			return SegmentType.TABLE;
		}

		return SegmentType.PARAGRAPH;
	}

	/**
	 * Determine if a new segment should be created based on the current line
	 * @param line current line
	 * @param currentType current segment type
	 * @return true if a new segment should be created
	 */
	private boolean isNewSegmentNeeded(String line, SegmentType currentType) {
		if (currentType == null) {
			return true;
		}

		String trimmedLine = line.trim();
		SegmentType lineType = detectSegmentType(line);

		// Always create a new segment for headings
		if (lineType == SegmentType.HEADING) {
			return true;
		}

		// Create a new segment if the type changes
		return lineType != currentType;
	}

	/**
	 * Merge consecutive segments of the same type if they are small enough
	 * @param segments list of segments to optimize
	 * @return optimized list of segments
	 */
	public List<MarkdownSegment> optimizeSegments(List<MarkdownSegment> segments) {
		if (segments == null || segments.size() <= 1) {
			return segments;
		}

		List<MarkdownSegment> optimized = new ArrayList<>();
		MarkdownSegment current = segments.get(0);
		StringBuilder content = new StringBuilder(current.content());

		for (int i = 1; i < segments.size(); i++) {
			MarkdownSegment next = segments.get(i);

			// Check if segments can be merged
			boolean sameType = current.type() == next.type();
			boolean notCodeBlock = current.type() != SegmentType.CODE_BLOCK;
			boolean notHeading = current.type() != SegmentType.HEADING;
			boolean notFrontMatter = current.type() != SegmentType.FRONTMATTER;

			if (sameType && notCodeBlock && notHeading && notFrontMatter) {
				// Check if merging would not exceed token limit
				String potentialMerge = content.toString() + next.content();
				if (tokenCounter.estimateTokens(potentialMerge) <= maxTokensPerSegment) {
					// Merge segments
					content.append(next.content());
					continue;
				}
			}

			// Cannot merge, add current segment to result
			optimized.add(new MarkdownSegment(content.toString(), current.type(), current.order()));
			current = next;
			content = new StringBuilder(current.content());
		}

		// Add the last segment
		optimized.add(new MarkdownSegment(content.toString(), current.type(), current.order()));

		return optimized;
	}

}
