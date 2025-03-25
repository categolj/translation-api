package am.ik.translation.markdown;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSegmenterTest {

	private final MarkdownSegmenter segmenter = new MarkdownSegmenter();

	@Test
	void shouldSegmentBasicMarkdown() {
		String markdown = """
				# タイトル

				これは段落です。Javaについて説明します。

				## サブセクション

				ここに詳細な説明が入ります。
				複数行に渡る段落です。
				""";

		List<MarkdownSegment> segments = segmenter.segment(markdown);

		// Should have 4 segments: 1 for title, 1 for paragraph, 1 for subsection, 1 for
		// paragraph
		assertThat(segments).hasSize(4);
		assertThat(segments.get(0).type()).isEqualTo(SegmentType.HEADING);
		assertThat(segments.get(1).type()).isEqualTo(SegmentType.PARAGRAPH);
		assertThat(segments.get(2).type()).isEqualTo(SegmentType.HEADING);
		assertThat(segments.get(3).type()).isEqualTo(SegmentType.PARAGRAPH);
	}

	@Test
	void shouldHandleCodeBlocks() {
		String markdown = """
				# コードの例

				以下はJavaのコード例です：

				```java
				public class Example {
				    public static void main(String[] args) {
				        System.out.println("Hello World");
				    }
				}
				```

				上記のコードは"Hello World"と出力します。
				""";

		List<MarkdownSegment> segments = segmenter.segment(markdown);

		// Should have 4 segments: title, intro paragraph, code block, conclusion
		// paragraph
		assertThat(segments).hasSize(4);
		assertThat(segments.get(0).type()).isEqualTo(SegmentType.HEADING);
		assertThat(segments.get(1).type()).isEqualTo(SegmentType.PARAGRAPH);
		assertThat(segments.get(2).type()).isEqualTo(SegmentType.CODE_BLOCK);
		assertThat(segments.get(3).type()).isEqualTo(SegmentType.PARAGRAPH);

		// The code block should contain exact code content
		String codeBlockContent = segments.get(2).content();
		assertThat(codeBlockContent).contains("```java");
		assertThat(codeBlockContent).contains("public class Example");
		assertThat(codeBlockContent).contains("```");
	}

	@Test
	void shouldHandleFrontMatter() {
		String markdown = """
				---
				title: サンプル記事
				date: 2023-01-01
				---

				# はじめに

				この記事はサンプルです。
				""";

		List<MarkdownSegment> segments = segmenter.segment(markdown);

		// Should have 4 segments: frontmatter, empty paragraph, title, paragraph
		assertThat(segments).hasSize(4);
		assertThat(segments.get(0).type()).isEqualTo(SegmentType.FRONTMATTER);
		// The second segment is an empty paragraph between frontmatter and heading
		assertThat(segments.get(2).type()).isEqualTo(SegmentType.HEADING);
		assertThat(segments.get(3).type()).isEqualTo(SegmentType.PARAGRAPH);

		// Front matter content verification
		String frontMatterContent = segments.get(0).content();
		assertThat(frontMatterContent).contains("---");
		assertThat(frontMatterContent).contains("title: サンプル記事");
	}

	@Test
	void shouldHandleListsAndTables() {
		String markdown = """
				# リストとテーブル

				## リスト

				- 項目1
				- 項目2
				- 項目3

				## テーブル

				| 列1 | 列2 | 列3 |
				|-----|-----|-----|
				| A   | B   | C   |
				| D   | E   | F   |
				""";

		List<MarkdownSegment> segments = segmenter.segment(markdown);

		// Verify we have appropriate segments
		assertThat(segments).hasSizeGreaterThanOrEqualTo(4);

		// Find the list segment
		boolean hasListSegment = segments.stream().anyMatch(s -> s.type() == SegmentType.LIST);
		assertThat(hasListSegment).isTrue();

		// Find the table segment
		boolean hasTableSegment = segments.stream().anyMatch(s -> s.type() == SegmentType.TABLE);
		assertThat(hasTableSegment).isTrue();
	}

	@Test
	void shouldOptimizeSegments() {
		// Create a list of segments that can be optimized
		List<MarkdownSegment> segments = List.of(new MarkdownSegment("# Header", SegmentType.HEADING, 0),
				new MarkdownSegment("Paragraph 1.", SegmentType.PARAGRAPH, 1),
				new MarkdownSegment("Paragraph 2.", SegmentType.PARAGRAPH, 2),
				new MarkdownSegment("```code```", SegmentType.CODE_BLOCK, 3),
				new MarkdownSegment("Paragraph 3.", SegmentType.PARAGRAPH, 4),
				new MarkdownSegment("Paragraph 4.", SegmentType.PARAGRAPH, 5));

		List<MarkdownSegment> optimized = segmenter.optimizeSegments(segments);

		// Should combine the paragraphs where possible
		assertThat(optimized.size()).isLessThan(segments.size());
	}

}
