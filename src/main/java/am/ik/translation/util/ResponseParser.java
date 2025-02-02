package am.ik.translation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser {

	public static TitleAndContent parseText(String text) {
		String titlePattern = "== title ==\\s*(.+?)\\s*== content ==";
		String contentPattern = "== content ==\\s*(.+)";

		Pattern patternTitle = Pattern.compile(titlePattern, Pattern.DOTALL);
		Pattern patternContent = Pattern.compile(contentPattern, Pattern.DOTALL);

		String thinkRemovedText = text.replaceAll("(?s)<think>.*?</think>", "").trim();
		Matcher matcherTitle = patternTitle.matcher(thinkRemovedText);
		Matcher matcherContent = patternContent.matcher(thinkRemovedText);

		String title = "";
		String content = "";

		if (matcherTitle.find()) {
			title = matcherTitle.group(1).trim();
		}

		if (matcherContent.find()) {
			content = matcherContent.group(1).trim();
		}

		return new TitleAndContent(title, content);
	}

	public record TitleAndContent(String title, String content) {
	}

	;

}
