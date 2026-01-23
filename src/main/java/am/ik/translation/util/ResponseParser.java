package am.ik.translation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser {

	public static TranslatedContent parseText(String text) {
		// Pattern for title: between "== title ==" and either "== summary ==" or "==
		// content =="
		String titlePattern = "== title ==\\s*(.+?)\\s*(?:== summary ==|== content ==)";
		// Pattern for summary: between "== summary ==" and "== content ==" (optional)
		String summaryPattern = "== summary ==\\s*(.+?)\\s*== content ==";
		// Pattern for content: after "== content =="
		String contentPattern = "== content ==\\s*(.+)";

		Pattern patternTitle = Pattern.compile(titlePattern, Pattern.DOTALL);
		Pattern patternSummary = Pattern.compile(summaryPattern, Pattern.DOTALL);
		Pattern patternContent = Pattern.compile(contentPattern, Pattern.DOTALL);

		String thinkRemovedText = text.replaceAll("(?s)<think>.*?</think>", "").trim();
		Matcher matcherTitle = patternTitle.matcher(thinkRemovedText);
		Matcher matcherSummary = patternSummary.matcher(thinkRemovedText);
		Matcher matcherContent = patternContent.matcher(thinkRemovedText);

		String title = "";
		String summary = null;
		String content = "";

		if (matcherTitle.find()) {
			title = matcherTitle.group(1).trim();
		}

		if (matcherSummary.find()) {
			summary = matcherSummary.group(1).trim();
		}

		if (matcherContent.find()) {
			content = matcherContent.group(1).trim();
		}

		return new TranslatedContent(title, summary, content);
	}

	public record TranslatedContent(String title, String summary, String content) {
	}

}
