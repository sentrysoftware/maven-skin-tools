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
		processElement(element, result, new ConversionState());
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
			result.append("**");
			processElement(element, result, state);
			result.append("**");
			break;
		case "em":
		case "i":
			result.append("*");
			processElement(element, result, state);
			result.append("*");
			break;
		case "code":
			if (!state.isInPreformatted()) {
				result.append("`");
				processElement(element, result, state);
				result.append("`");
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
	 * Processes a heading element.
	 */
	private static void processHeading(
			final Element element,
			final StringBuilder result,
			final ConversionState state,
			final int level) {
		ensureBlankLine(result);
		result.append("#".repeat(level)).append(" ");
		processElement(element, result, state);
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
			result.append(codeElement.wholeText());
		} else {
			result.append(element.wholeText());
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
		String href = element.attr("href");
		String text = element.text();

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

		// Prefix each line with >
		String[] lines = quoteContent.toString().trim().split("\n");
		for (String line : lines) {
			result.append("> ").append(line).append("\n");
		}
		result.append("\n");
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
			result.append(" ").append(cellContent.toString().trim().replaceAll("\\s+", " ")).append(" |");
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
	 * Tracks the current state during HTML to Markdown conversion.
	 */
	private static class ConversionState {

		private boolean inPreformatted = false;
		private int listDepth = 0;

		ConversionState() {}

		ConversionState(final ConversionState other) {
			this.inPreformatted = other.inPreformatted;
			this.listDepth = other.listDepth;
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
