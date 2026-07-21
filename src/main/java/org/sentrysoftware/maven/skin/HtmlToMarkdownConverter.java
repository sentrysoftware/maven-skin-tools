package org.sentrysoftware.maven.skin;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Sentry Maven Skin Tools
 * ჻჻჻჻჻჻
 * Copyright (C) 2017 - 2026 Sentry Software
 * ჻჻჻჻჻჻
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * A simple HTML to Markdown converter using JSoup.
 * <p>
 * This converter handles common HTML elements and converts them to their
 * Markdown equivalents. It is designed to work without external dependencies
 * that might conflict with the Maven Site Plugin's classloader.
 * </p>
 */
public final class HtmlToMarkdownConverter {
	private static final Pattern TECHNICAL_QUOTED_VALUE = Pattern
			.compile(
					"([\\p{Alnum}_.:-]+=)([\\u2018\\u2019\\u201c\\u201d])([^\\u2018\\u2019\\u201c\\u201d]*)([\\u2018\\u2019\\u201c\\u201d])");

	/**
	 * Private constructor to prevent instantiation.
	 */
	private HtmlToMarkdownConverter() {
		// Utility class
	}

	/**
	 * Converts HTML content to Markdown format.
	 *
	 * @param html HTML content to convert
	 * @return Markdown representation of the HTML
	 */
	public static String convert(final String html) {
		if (html == null || html.isEmpty()) {
			return "";
		}

		Document doc = Jsoup.parseBodyFragment(html);
		return convert(doc.body());
	}

	/**
	 * Converts an HTML element to Markdown format.
	 * <p>
	 * This method is more efficient than {@link #convert(String)} when you already
	 * have a parsed JSoup Element, as it avoids re-parsing the HTML.
	 * </p>
	 *
	 * @param element HTML element to convert
	 * @return Markdown representation of the HTML element
	 */
	public static String convert(final Element element) {
		if (element == null) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		processElement(element, result, new ConversionState(element));
		return result.toString().trim();
	}

	/**
	 * Processes an element and its children, converting to Markdown.
	 *
	 * @param element The element to process
	 * @param result The StringBuilder to append Markdown to
	 * @param state The current conversion state
	 */
	private static void processElement(final Element element, final StringBuilder result, final ConversionState state) {
		for (Node child : element.childNodes()) {
			if (child instanceof TextNode) {
				processTextNode((TextNode) child, result, state);
			} else if (child instanceof Element) {
				processHtmlElement((Element) child, result, state);
			}
		}
	}

	/**
	 * Processes a text node.
	 *
	 * @param textNode The text node to process
	 * @param result The StringBuilder to append text to
	 * @param state The current conversion state
	 */
	private static void processTextNode(
			final TextNode textNode,
			final StringBuilder result,
			final ConversionState state) {
		String text = textNode.getWholeText();

		// In preformatted blocks, preserve the text as-is
		if (state.isInPreformatted()) {
			result.append(text);
			return;
		}

		// Collapse whitespace for normal text
		text = text.replaceAll("\\s+", " ");

		// Don't add leading space at start of line
		if (result.length() > 0 && result.charAt(result.length() - 1) == '\n' && text.startsWith(" ")) {
			text = text.stripLeading();
		}
		if (result.length() > 0 && result.charAt(result.length() - 1) == ' ' && text.startsWith(" ")) {
			text = text.stripLeading();
		}

		result.append(text);
	}

	/**
	 * Processes an HTML element and converts it to Markdown.
	 *
	 * @param element The HTML element to process
	 * @param result The StringBuilder to append Markdown to
	 * @param state The current conversion state
	 */
	private static void processHtmlElement(
			final Element element,
			final StringBuilder result,
			final ConversionState state) {
		String tagName = element.tagName().toLowerCase();

		if (element.hasClass("visible-print-inline") || element.hasClass("sentry-print-tab-heading")) {
			return;
		}

		if (element.hasClass("callout")) {
			processCallout(element, result, state);
			return;
		}

		switch (tagName) {
		case "h1":
			processHeading(element, result, state, 1);
			break;
		case "h2":
			processHeading(element, result, state, 2);
			break;
		case "h3":
			processHeading(element, result, state, 3);
			break;
		case "h4":
			processHeading(element, result, state, 4);
			break;
		case "h5":
			processHeading(element, result, state, 5);
			break;
		case "h6":
			processHeading(element, result, state, 6);
			break;
		case "p":
			processParagraph(element, result, state);
			break;
		case "br":
			result.append("  \n");
			break;
		case "hr":
			ensureBlankLine(result);
			result.append("---\n\n");
			break;
		case "strong":
		case "b":
			processEmphasis(element, result, state, "**");
			break;
		case "em":
		case "i":
			processEmphasis(element, result, state, "*");
			break;
		case "code":
			if (!state.isInPreformatted()) {
				StringBuilder code = new StringBuilder();
				processElement(element, code, state);
				result.append("`").append(normalizeTypographicQuotes(code.toString())).append("`");
			} else {
				processElement(element, result, state);
			}
			break;
		case "pre":
			processPreformatted(element, result);
			break;
		case "a":
			processLink(element, result, state);
			break;
		case "img":
			processImage(element, result);
			break;
		case "ul":
			processList(element, result, state, false);
			break;
		case "ol":
			processList(element, result, state, true);
			break;
		case "li":
			// List items are handled by processList
			processElement(element, result, state);
			break;
		case "blockquote":
			processBlockquote(element, result, state);
			break;
		case "table":
			processTable(element, result, state);
			break;
		case "uib-tab-heading":
			processTabHeading(element, result);
			break;
		case "div":
		case "section":
		case "article":
		case "main":
		case "header":
		case "footer":
		case "nav":
		case "aside":
			// Block-level containers - just process children
			processElement(element, result, state);
			break;
		case "span":
			// Inline container - just process children
			processElement(element, result, state);
			break;
		case "script":
		case "style":
		case "noscript":
			// Skip these elements entirely
			break;
		default:
			// For unknown elements, just process children
			processElement(element, result, state);
			break;
		}
	}

	/**
	 * Processes bold or italic content without emitting empty Markdown delimiters.
	 */
	private static void processEmphasis(
			final Element element,
			final StringBuilder result,
			final ConversionState state,
			final String delimiter) {
		StringBuilder content = new StringBuilder();
		processElement(element, content, state);
		if (content.toString().isBlank()) {
			if (content.length() > 0 && (result.length() == 0 || result.charAt(result.length() - 1) != ' ')) {
				result.append(' ');
			}
		} else {
			result.append(delimiter).append(content).append(delimiter);
		}
	}

	/**
	 * Processes a heading element.
	 */
	private static void processHeading(
			final Element element,
			final StringBuilder result,
			final ConversionState state,
			final int level) {
		ensureBlankLine(result);
		result.append("#".repeat(level)).append(" ");

		Element selfLink = null;
		if (element.childrenSize() == 1 && element.ownText().isBlank()) {
			Element onlyChild = element.child(0);
			if ("a".equals(onlyChild.tagName())
					&& !element.id().isEmpty()
					&& onlyChild.attr("href").equals("#" + element.id())) {
				selfLink = onlyChild;
			}
		}

		if (selfLink == null) {
			processElement(element, result, state);
		} else {
			processElement(selfLink, result, state);
		}
		result.append("\n\n");
	}

	/**
	 * Processes a paragraph element.
	 */
	private static void processParagraph(final Element element, final StringBuilder result, final ConversionState state) {
		ensureBlankLine(result);
		processElement(element, result, state);
		result.append("\n\n");
	}

	/**
	 * Processes a preformatted/code block element.
	 */
	private static void processPreformatted(final Element element, final StringBuilder result) {
		ensureBlankLine(result);

		// Try to detect the language from class attributes
		String language = "";
		Element codeElement = element.selectFirst("code");
		if (codeElement != null) {
			String classAttr = codeElement.className();
			if (classAttr != null && !classAttr.isEmpty()) {
				// Look for language-xxx or xxx class
				for (String cls : classAttr.split("\\s+")) {
					if (cls.startsWith("language-")) {
						language = cls.substring(9);
						break;
					} else if (cls.matches("^[a-z]+$")) {
						language = cls;
					}
				}
			}
		}

		result.append("```").append(language).append("\n");

		if (codeElement != null) {
			result.append(normalizeTypographicQuotes(codeElement.wholeText()));
		} else {
			result.append(normalizeTypographicQuotes(element.wholeText()));
		}

		// Ensure the code block ends with a newline
		if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
			result.append("\n");
		}
		result.append("```\n\n");
	}

	/**
	 * Processes a link element.
	 */
	private static void processLink(final Element element, final StringBuilder result, final ConversionState state) {
		String href = state.resolveHref(element.attr("href"));
		String text = markdownVisibleText(element);

		if (href.isEmpty()) {
			// No href, just output the text
			processElement(element, result, state);
		} else if (text.isEmpty()) {
			// No text, use href as text
			result.append("[").append(href).append("](").append(href).append(")");
		} else {
			result.append("[").append(text).append("](").append(href).append(")");
		}
	}

	/**
	 * Processes an image element.
	 */
	private static void processImage(final Element element, final StringBuilder result) {
		String src = element.attr("src");
		String alt = element.attr("alt");

		if (src.isEmpty()) {
			return;
		}

		result.append("![").append(alt).append("](").append(src).append(")");
	}

	/**
	 * Processes a list (ordered or unordered).
	 */
	private static void processList(
			final Element element,
			final StringBuilder result,
			final ConversionState state,
			final boolean ordered) {
		ensureBlankLine(result);

		int itemNumber = 1;
		String indent = "  ".repeat(state.getListDepth());

		ConversionState listState = new ConversionState(state);
		listState.incrementListDepth();

		for (Element child : element.children()) {
			if ("li".equals(child.tagName().toLowerCase())) {
				result.append(indent);
				if (ordered) {
					result.append(itemNumber++).append(". ");
				} else {
					result.append("- ");
				}

				// Process list item content
				StringBuilder itemContent = new StringBuilder();
				processElement(child, itemContent, listState);
				String content = itemContent.toString().trim();

				// Handle multi-line content in list items
				String[] lines = content.split("\n");
				for (int i = 0; i < lines.length; i++) {
					if (i > 0) {
						result.append(indent).append("  ");
					}
					result.append(lines[i]);
					if (i < lines.length - 1) {
						result.append("\n");
					}
				}
				result.append("\n");
			}
		}
		result.append("\n");
	}

	/**
	 * Processes a blockquote element.
	 */
	private static void processBlockquote(
			final Element element,
			final StringBuilder result,
			final ConversionState state) {
		ensureBlankLine(result);

		StringBuilder quoteContent = new StringBuilder();
		processElement(element, quoteContent, state);
		appendBlockquote(quoteContent.toString(), result);
	}

	/**
	 * Processes a Sentry Maven Skin callout as a Markdown blockquote with a bold title.
	 */
	private static void processCallout(
			final Element element,
			final StringBuilder result,
			final ConversionState state) {
		ensureBlankLine(result);

		Element calloutContent = element.clone();
		Element title = directCalloutTitle(calloutContent);
		String label = title == null ? "" : markdownVisibleText(title);
		if (title != null) {
			title.remove();
		}
		if (label.isBlank()) {
			label = calloutLabelFromClass(element);
		}

		StringBuilder quoteContent = new StringBuilder();
		quoteContent.append("**").append(label).append("**\n\n");
		processElement(calloutContent, quoteContent, state);
		appendBlockquote(quoteContent.toString(), result);
	}

	/**
	 * Finds the title that belongs directly to a callout, excluding nested callout titles.
	 */
	private static Element directCalloutTitle(final Element callout) {
		for (Element child : callout.children()) {
			if (child.hasClass("callout-title")) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Appends content with Markdown blockquote prefixes.
	 */
	private static void appendBlockquote(final String content, final StringBuilder result) {
		String[] lines = content.trim().split("\n", -1);
		for (String line : lines) {
			result.append(">");
			if (!line.isEmpty()) {
				result.append(" ").append(line);
			}
			result.append("\n");
		}
		result.append("\n");
	}

	/**
	 * Processes an interactive tab heading as a linear Markdown heading.
	 */
	private static void processTabHeading(final Element element, final StringBuilder result) {
		String heading = markdownVisibleText(element);
		if (!heading.isEmpty()) {
			ensureBlankLine(result);
			result.append("#### ").append(heading).append("\n\n");
		}
	}

	/**
	 * Processes a table element.
	 */
	private static void processTable(final Element element, final StringBuilder result, final ConversionState state) {
		ensureBlankLine(result);

		// Find header row
		Element thead = element.selectFirst("thead");
		Element tbody = element.selectFirst("tbody");

		// Process header: look in thead first, otherwise first row in table
		Element headerRow;
		if (thead != null) {
			headerRow = thead.selectFirst("tr");
		} else {
			// First row might be header
			headerRow = element.selectFirst("tr");
		}

		if (headerRow != null) {
			processTableRow(headerRow, result, state, true);
		}

		// Process body rows
		if (tbody != null) {
			for (Element row : tbody.select("tr")) {
				if (row != headerRow) {
					processTableRow(row, result, state, false);
				}
			}
		} else {
			boolean first = true;
			for (Element row : element.select("tr")) {
				if (first && row == headerRow) {
					first = false;
					continue;
				}
				processTableRow(row, result, state, false);
			}
		}
		result.append("\n");
	}

	/**
	 * Processes a table row.
	 */
	private static void processTableRow(
			final Element row,
			final StringBuilder result,
			final ConversionState state,
			final boolean isHeader) {
		result.append("|");
		int cellCount = 0;

		for (Element cell : row.select("th, td")) {
			StringBuilder cellContent = new StringBuilder();
			processElement(cell, cellContent, state);
			String content = cellContent.toString().trim().replaceAll("\\s+", " ");
			content = normalizeTechnicalAssignments(content);
			content = escapeTablePipes(content);
			result.append(" ").append(content).append(" |");
			cellCount++;
		}
		result.append("\n");

		// Add separator after header row
		if (isHeader && cellCount > 0) {
			result.append("|");
			for (int i = 0; i < cellCount; i++) {
				result.append(" --- |");
			}
			result.append("\n");
		}
	}

	/**
	 * Ensures there's a blank line before the next content.
	 */
	private static void ensureBlankLine(final StringBuilder result) {
		if (result.length() == 0) {
			return;
		}

		// Check if we already end with blank line(s)
		int len = result.length();
		if (len >= 2 && result.charAt(len - 1) == '\n' && result.charAt(len - 2) == '\n') {
			return;
		}

		// Check if we end with single newline
		if (result.charAt(len - 1) == '\n') {
			result.append("\n");
			return;
		}

		// No newline at end, add two
		result.append("\n\n");
	}

	/**
	 * Replaces typographic quotes with their machine-readable ASCII equivalents in technical
	 * content.
	 */
	private static String normalizeTypographicQuotes(final String value) {
		StringBuilder normalized = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
			case '\u2018':
			case '\u2019':
			case '\u201a':
			case '\u201b':
				normalized.append('\'');
				break;
			case '\u201c':
			case '\u201d':
			case '\u201e':
			case '\u201f':
				normalized.append('"');
				break;
			default:
				normalized.append(character);
				break;
			}
		}
		return normalized.toString();
	}

	/**
	 * Normalizes typographic quotes only within compact technical assignments such as
	 * {@code key=“value”}, preserving typographic quotes in surrounding prose.
	 */
	private static String normalizeTechnicalAssignments(final String content) {
		Matcher matcher = TECHNICAL_QUOTED_VALUE.matcher(content);
		StringBuffer normalized = new StringBuffer(content.length());
		while (matcher.find()) {
			char openingQuote = matcher.group(2).charAt(0);
			String quote = openingQuote == '\u201c' || openingQuote == '\u201d' ? "\"" : "'";
			String replacement = matcher.group(1) + quote + matcher.group(3) + quote;
			matcher.appendReplacement(normalized, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(normalized);
		return normalized.toString();
	}

	/**
	 * Escapes pipe characters that would otherwise create extra Markdown table cells. An odd
	 * number of existing backslashes is retained as an existing Markdown escape; an even number is
	 * extended so literal backslashes and the pipe can both survive Markdown rendering.
	 */
	private static String escapeTablePipes(final String value) {
		StringBuilder escaped = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (character == '|') {
				int precedingBackslashes = 0;
				for (int j = i - 1; j >= 0 && value.charAt(j) == '\\'; j--) {
					precedingBackslashes++;
				}
				if (precedingBackslashes % 2 == 0) {
					escaped.append('\\');
				}
			}
			escaped.append(character);
		}
		return escaped.toString();
	}

	/**
	 * Creates the anchor slug used by common Markdown renderers for a heading.
	 */
	private static String markdownHeadingSlug(final String heading) {
		String lowerCaseHeading = heading.toLowerCase(Locale.ROOT);
		StringBuilder slug = new StringBuilder(lowerCaseHeading.length());
		for (int i = 0; i < lowerCaseHeading.length(); i++) {
			char character = lowerCaseHeading.charAt(i);
			if (Character.isLetterOrDigit(character) || character == '-' || character == '_') {
				slug.append(character);
			} else if (Character.isWhitespace(character)) {
				slug.append('-');
			}
		}
		return slug.toString();
	}

	/**
	 * Returns text that is visible in the Markdown export.
	 */
	private static String markdownVisibleText(final Element element) {
		Element visibleContent = element.clone();
		visibleContent.select(".visible-print-inline, .sentry-print-tab-heading, script, style, noscript").remove();
		for (Element image : visibleContent.select("img")) {
			image.before(new TextNode(image.attr("alt")));
			image.remove();
		}
		return visibleContent.text().trim();
	}

	/**
	 * Derives a callout title from its modifier class.
	 */
	private static String calloutLabelFromClass(final Element element) {
		for (String className : element.classNames()) {
			if (className.startsWith("callout-") && className.length() > "callout-".length()) {
				String type = className.substring("callout-".length());
				return type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1).toLowerCase(Locale.ROOT);
			}
		}
		return "Note";
	}

	/**
	 * Tracks the current state during HTML to Markdown conversion.
	 */
	private static class ConversionState {

		private boolean inPreformatted = false;
		private int listDepth = 0;
		private final Map<String, String> fragmentSlugs;

		ConversionState(final Element root) {
			fragmentSlugs = buildFragmentSlugs(root);
		}

		ConversionState(final ConversionState other) {
			this.inPreformatted = other.inPreformatted;
			this.listDepth = other.listDepth;
			this.fragmentSlugs = other.fragmentSlugs;
		}

		private static Map<String, String> buildFragmentSlugs(final Element root) {
			Map<String, String> slugs = new HashMap<>();
			Set<String> usedSlugs = new HashSet<>();
			Map<String, List<String>> slugsByHeadingText = new HashMap<>();

			for (Element heading : root.select("h1, h2, h3, h4, h5, h6, uib-tab-heading")) {
				if (heading.closest("table, .visible-print-inline, .sentry-print-tab-heading") != null) {
					continue;
				}

				String headingText = markdownVisibleText(heading);
				String baseSlug = markdownHeadingSlug(headingText);
				if (baseSlug.isEmpty()) {
					continue;
				}
				String headingSlug = uniqueHeadingSlug(baseSlug, usedSlugs);
				boolean isHtmlHeading = heading.tagName().matches("h[1-6]");

				if (isHtmlHeading && !heading.id().isEmpty()) {
					slugs.put(heading.id(), headingSlug);
				}
				slugs.putIfAbsent(headingSlug, headingSlug);
				slugsByHeadingText.computeIfAbsent(headingText, ignored -> new ArrayList<>()).add(headingSlug);

				Element previousElement = heading.previousElementSibling();
				if (isHtmlHeading
						&& previousElement != null
						&& "a".equals(previousElement.tagName())
						&& !previousElement.id().isEmpty()
						&& previousElement.text().isBlank()
						&& !previousElement.hasAttr("href")) {
					slugs.put(previousElement.id(), headingSlug);
				}
			}

			for (Element link : root.select("a[href^=#]")) {
				String target = link.attr("href").substring(1);
				List<String> matchingSlugs = slugsByHeadingText.get(markdownVisibleText(link));
				boolean isTocLink = link.closest("#toc, #right-toc, .toc, .toc-inline-container") != null;
				if (isTocLink && !slugs.containsKey(target) && matchingSlugs != null && matchingSlugs.size() == 1) {
					slugs.put(target, matchingSlugs.get(0));
				}
			}

			return slugs;
		}

		private static String uniqueHeadingSlug(final String baseSlug, final Set<String> usedSlugs) {
			String candidate = baseSlug;
			int suffix = 1;
			while (!usedSlugs.add(candidate)) {
				candidate = baseSlug + "-" + suffix++;
			}
			return candidate;
		}

		String resolveHref(final String href) {
			if (!href.startsWith("#")) {
				return href;
			}

			String resolved = fragmentSlugs.get(href.substring(1));
			return resolved == null ? href : "#" + resolved;
		}

		boolean isInPreformatted() {
			return inPreformatted;
		}

		void setInPreformatted(final boolean inPreformatted) {
			this.inPreformatted = inPreformatted;
		}

		int getListDepth() {
			return listDepth;
		}

		void incrementListDepth() {
			this.listDepth++;
		}
	}
}
