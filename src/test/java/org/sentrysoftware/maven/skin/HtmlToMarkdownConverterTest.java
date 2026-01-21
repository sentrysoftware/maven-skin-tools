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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HtmlToMarkdownConverter}.
 */
class HtmlToMarkdownConverterTest {

	@Test
	void testNullInput() {
		assertEquals("", HtmlToMarkdownConverter.convert((String) null));
		assertEquals("", HtmlToMarkdownConverter.convert((Element) null));
	}

	@Test
	void testEmptyInput() {
		assertEquals("", HtmlToMarkdownConverter.convert(""));
	}

	@Test
	void testPlainText() {
		assertEquals("Hello World", HtmlToMarkdownConverter.convert("Hello World"));
	}

	@Test
	void testHeadings() {
		assertEquals("# Heading 1", HtmlToMarkdownConverter.convert("<h1>Heading 1</h1>"));
		assertEquals("## Heading 2", HtmlToMarkdownConverter.convert("<h2>Heading 2</h2>"));
		assertEquals("### Heading 3", HtmlToMarkdownConverter.convert("<h3>Heading 3</h3>"));
		assertEquals("#### Heading 4", HtmlToMarkdownConverter.convert("<h4>Heading 4</h4>"));
		assertEquals("##### Heading 5", HtmlToMarkdownConverter.convert("<h5>Heading 5</h5>"));
		assertEquals("###### Heading 6", HtmlToMarkdownConverter.convert("<h6>Heading 6</h6>"));
	}

	@Test
	void testParagraph() {
		String result = HtmlToMarkdownConverter.convert("<p>This is a paragraph.</p>");
		assertTrue(result.contains("This is a paragraph."));
	}

	@Test
	void testBold() {
		assertEquals("**bold text**", HtmlToMarkdownConverter.convert("<strong>bold text</strong>"));
		assertEquals("**bold text**", HtmlToMarkdownConverter.convert("<b>bold text</b>"));
	}

	@Test
	void testItalic() {
		assertEquals("*italic text*", HtmlToMarkdownConverter.convert("<em>italic text</em>"));
		assertEquals("*italic text*", HtmlToMarkdownConverter.convert("<i>italic text</i>"));
	}

	@Test
	void testInlineCode() {
		assertEquals("`code`", HtmlToMarkdownConverter.convert("<code>code</code>"));
	}

	@Test
	void testLink() {
		assertEquals(
				"[link text](http://example.com)",
				HtmlToMarkdownConverter.convert("<a href=\"http://example.com\">link text</a>"));
	}

	@Test
	void testLinkWithoutText() {
		assertEquals(
				"[http://example.com](http://example.com)",
				HtmlToMarkdownConverter.convert("<a href=\"http://example.com\"></a>"));
	}

	@Test
	void testImage() {
		assertEquals(
				"![alt text](image.png)",
				HtmlToMarkdownConverter.convert("<img src=\"image.png\" alt=\"alt text\">"));
	}

	@Test
	void testUnorderedList() {
		String html = "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("- Item 1"));
		assertTrue(result.contains("- Item 2"));
		assertTrue(result.contains("- Item 3"));
	}

	@Test
	void testOrderedList() {
		String html = "<ol><li>First</li><li>Second</li><li>Third</li></ol>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("1. First"));
		assertTrue(result.contains("2. Second"));
		assertTrue(result.contains("3. Third"));
	}

	@Test
	void testCodeBlock() {
		String html = "<pre><code>function test() {\n    return true;\n}</code></pre>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("```"));
		assertTrue(result.contains("function test()"));
	}

	@Test
	void testCodeBlockWithLanguage() {
		String html = "<pre><code class=\"language-java\">public class Test {}</code></pre>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("```java"));
	}

	@Test
	void testBlockquote() {
		String html = "<blockquote>This is a quote.</blockquote>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("> This is a quote."));
	}

	@Test
	void testHorizontalRule() {
		String html = "<hr>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("---"));
	}

	@Test
	void testTable() {
		String html = "<table><thead><tr><th>Header 1</th><th>Header 2</th></tr></thead>" +
				"<tbody><tr><td>Cell 1</td><td>Cell 2</td></tr></tbody></table>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("| Header 1 | Header 2 |"));
		assertTrue(result.contains("| --- | --- |"));
		assertTrue(result.contains("| Cell 1 | Cell 2 |"));
	}

	@Test
	void testNestedList() {
		String html = "<ul><li>Item 1<ul><li>Nested 1</li><li>Nested 2</li></ul></li><li>Item 2</li></ul>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("- Item 1"));
		assertTrue(result.contains("- Nested 1"));
		assertTrue(result.contains("- Item 2"));
	}

	@Test
	void testLineBreak() {
		String html = "Line 1<br>Line 2";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("Line 1"));
		assertTrue(result.contains("Line 2"));
	}

	@Test
	void testScriptAndStyleIgnored() {
		String html = "<script>alert('test');</script><style>.test{}</style><p>Content</p>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("Content"));
		assertTrue(!result.contains("alert"));
		assertTrue(!result.contains(".test"));
	}

	@Test
	void testComplexHtml() {
		String html = "<h1>Title</h1>" +
				"<p>This is <strong>bold</strong> and <em>italic</em> text.</p>" +
				"<ul><li>Item 1</li><li>Item 2</li></ul>" +
				"<p>See <a href=\"http://example.com\">this link</a>.</p>";
		String result = HtmlToMarkdownConverter.convert(html);
		assertTrue(result.contains("# Title"));
		assertTrue(result.contains("**bold**"));
		assertTrue(result.contains("*italic*"));
		assertTrue(result.contains("- Item 1"));
		assertTrue(result.contains("[this link](http://example.com)"));
	}
}
