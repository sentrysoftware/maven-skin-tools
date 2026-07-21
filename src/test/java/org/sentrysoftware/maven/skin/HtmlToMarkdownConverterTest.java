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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

	@Test
	void testIssue115AllCalloutTypesUseBalancedMarkdown() {
		String[] labels = { "Note", "Tip", "Important", "Warning", "Caution" };

		for (String label : labels) {
			String html = "<div class='callout callout-" + label.toLowerCase() + "'>" +
					"<div class='callout-title'><i class='fa-solid fa-icon'></i> " + label + "</div>" +
					"<p><code>expectedResult</code> only needs to match one line.</p></div>";

			assertEquals(
					"> **" + label + "**\n>\n> `expectedResult` only needs to match one line.",
					HtmlToMarkdownConverter.convert(html));
		}

		assertEquals(
				"> **Warning**\n>\n> Warning content.",
				HtmlToMarkdownConverter
						.convert(
								"<div class='callout callout-warning'><div class='callout-title'>" +
										"<i class='fa-solid fa-icon'></i></div><p>Warning content.</p></div>"));
		assertEquals(
				"> **Caution**\n>\n> Caution content.",
				HtmlToMarkdownConverter
						.convert(
								"<div class='callout callout-caution'><p>Caution content.</p></div>"));
	}

	@Test
	void testIssue115EmptyInlineIconsDoNotEmitMarkdownDelimiters() {
		String html = "<p>Before <i class='fa-solid fa-triangle-exclamation'></i> after</p>";

		assertEquals("Before after", HtmlToMarkdownConverter.convert(html));
		assertEquals("Before after", HtmlToMarkdownConverter.convert("<p>Before<i> </i>after</p>"));
	}

	@Test
	void testIssue116TabsUseOneCleanHeading() {
		String html = "<uib-tabset><uib-tab>" +
				"<uib-tab-heading><span class='fa-solid fa-table-list'></span> Input</uib-tab-heading>" +
				"<p class='sentry-print-tab-heading'><strong>" +
				"<span class='fa-solid fa-table-list'></span> Input</strong></p>" +
				"<table><tr><th>col</th></tr><tr><td>value</td></tr></table>" +
				"</uib-tab></uib-tabset>";

		assertEquals(
				"#### Input\n\n| col |\n| --- |\n| value |",
				HtmlToMarkdownConverter.convert(html));
	}

	@Test
	void testIssue117TocLinksUseMarkdownHeadingSlugs() {
		String longHeading = "Step 2 - Create the connector directory and YAML file";
		String html = "<ul id='toc'>" +
				"<li><a href='#ordering-strategy-28cheap-to-expensive-29'>" +
				"Ordering strategy (cheap to expensive)" +
				"<sup class='visible-print-inline'>[7]</sup></a></li>" +
				"<li><a href='#multiinstance-3a-one-run-for-all-instances'>" +
				"MultiInstance: one run for all instances</a></li>" +
				"<li><a href='#a1-prepare-it-resources-structure'>1. Prepare IT resources structure</a></li>" +
				"<li><a href='#step-2---create-the-connector-directory-and-yaml-file'>" + longHeading + "</a></li>" +
				"<li><a href='#hw-e2-80-94-hardware'>HW \u2014 Hardware</a></li>" +
				"</ul>" +
				"<a id='ordering-strategy-28cheap-to-expensive-29'></a>" +
				"<h2 id='ordering-strategy-cheap-to-expensive'>" +
				"<a href='#ordering-strategy-cheap-to-expensive'>Ordering strategy (cheap to expensive)</a></h2>" +
				"<h2 id='multiinstance-one-run-for-all-instances'>" +
				"<a href='#multiinstance-one-run-for-all-instances'>" +
				"MultiInstance: one run for all instances</a></h2>" +
				"<h2 id='a1-prepare-it-resources-structure'>" +
				"<a href='#a1-prepare-it-resources-structure'>1. Prepare IT resources structure</a></h2>" +
				"<h2 id='step-2---create-the-connector-directory-and-yaml-f'>" +
				"<a href='#step-2---create-the-connector-directory-and-yaml-f'>" + longHeading + "</a></h2>" +
				"<h2 id='hw--hardware'><a href='#hw--hardware'>HW \u2014 Hardware</a></h2>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("[Ordering strategy (cheap to expensive)](#ordering-strategy-cheap-to-expensive)"));
		assertTrue(
				result
						.contains(
								"[MultiInstance: one run for all instances](#multiinstance-one-run-for-all-instances)"));
		assertTrue(result.contains("[1. Prepare IT resources structure](#1-prepare-it-resources-structure)"));
		assertTrue(result.contains("[" + longHeading + "](#step-2---create-the-connector-directory-and-yaml-file)"));
		assertTrue(result.contains("[HW \u2014 Hardware](#hw--hardware)"));
		assertTrue(result.contains("## Ordering strategy (cheap to expensive)"));
		assertFalse(result.contains("#ordering-strategy-28cheap-to-expensive-29"));
		assertFalse(result.contains("[7]"));
		assertFalse(result.contains("#multiinstance-3a-one-run-for-all-instances"));
		assertFalse(result.contains("#a1-prepare-it-resources-structure"));
		assertFalse(result.contains("#step-2---create-the-connector-directory-and-yaml-f)"));
	}

	@Test
	void testIssue117DuplicateHeadingSlugsUseMarkdownSuffixes() {
		String html = "<ul id='toc'><li><a href='#syntax'>Syntax</a></li>" +
				"<li><a href='#syntax-1'>Syntax</a></li></ul>" +
				"<h3 id='syntax'><a href='#syntax'>Syntax</a></h3>" +
				"<a id='syntax-1'></a><h3 id='syntax_2'><a href='#syntax_2'>Syntax</a></h3>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("[Syntax](#syntax)"));
		assertTrue(result.contains("[Syntax](#syntax-1)"));
		assertFalse(result.contains("#syntax_2"));
	}

	@Test
	void testIssue117SlugMapMatchesOnlyRenderedHeadings() {
		String html = "<ul id='toc'>" +
				"<li><a href='#overview-heading'>Overview</a></li>" +
				"<li><a href='#title-heading'>Title</a></li>" +
				"<li><a href='#syntax-literal'>Syntax-1</a></li></ul>" +
				"<uib-tab-heading>Overview</uib-tab-heading>" +
				"<h2 id='overview-heading'><a href='#overview-heading'>Overview" +
				"<sup class='visible-print-inline'>[9]</sup></a></h2>" +
				"<table><tr><td><h2>Title</h2></td></tr></table>" +
				"<h2 id='title-heading'>Title</h2>" +
				"<h2>Syntax</h2><h2>Syntax</h2>" +
				"<h2 id='syntax-literal'>Syntax-1</h2>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("[Overview](#overview-1)"));
		assertTrue(result.contains("[Title](#title)"));
		assertTrue(result.contains("[Syntax-1](#syntax-1-1)"));
		assertFalse(result.contains("#overview-9"));
	}

	@Test
	void testIssue117TextFallbackDoesNotRewriteOrdinaryLinks() {
		String html = "<p><a href='#custom-anchor'>Overview</a></p>" +
				"<div id='custom-anchor'>Custom target</div><h2 id='overview'>Overview</h2>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("[Overview](#custom-anchor)"));
		assertFalse(result.contains("[Overview](#overview)"));
	}

	@Test
	void testIssue117HeadingCrossReferenceIsNotMistakenForSelfLink() {
		String html = "<h2 id='current'><a href='#other'>See the other section</a></h2>" +
				"<h2 id='other'>Other section</h2>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("## [See the other section](#other-section)"));
	}

	@Test
	void testIssue118TablePreservesEntityAndBackslashEscapedPipes() {
		String html = "<table><tr><th>Entity</th><th>Backslash</th></tr>" +
				"<tr><td>Model&#124;Size</td><td>2\\|11</td></tr>" +
				"<tr><td>ST4000NM&#124;4000</td><td>value</td></tr></table>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertEquals(
				"| Entity | Backslash |\n" +
						"| --- | --- |\n" +
						"| Model\\|Size | 2\\|11 |\n" +
						"| ST4000NM\\|4000 | value |",
				result);
	}

	@Test
	void testIssue119TableRestoresStraightQuotesInTechnicalContent() {
		String html = "<table><tr><th>Path</th></tr><tr>" +
				"<td>\\\\SRV01\\root\\cimv2:Win32_Account.Domain=\u201cCONTOSO\u201d," +
				"Name=\u201cjsmith\u201d</td></tr>" +
				"<tr><td>She said \u201chello\u201d.</td></tr></table>" +
				"<table><tr><th>Example</th></tr>" +
				"<tr><td>key=\u201cvalue\u201d</td></tr>" +
				"<tr><td>Equation x = \u201cunknown\u201d.</td></tr></table>" +
				"<p><code>key=\u201cvalue\u201d</code></p>" +
				"<pre><code>key=\u201cvalue\u201d</code></pre>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("Domain=\"CONTOSO\",Name=\"jsmith\""));
		assertTrue(result.contains("She said \u201chello\u201d."));
		assertTrue(result.contains("| key=\"value\" |"));
		assertTrue(result.contains("Equation x = \u201cunknown\u201d."));
		assertTrue(result.contains("`key=\"value\"`"));
		assertTrue(result.contains("```\nkey=\"value\"\n```"));
	}

	@Test
	void testIssue120PrintOnlyLinkFootnotesAreIgnored() {
		String html = "<p><a href='run-and-debug.html'>Run and Debug Locally</a>" +
				"<sup class='visible-print-inline'>[1]</sup></p>" +
				"<ul><li><a href='https://example.com/WBEMGenLUN.yaml'>WBEMGenLUN</a>" +
				"<sup class='visible-print-inline'>[1]</sup></li>" +
				"<li><a href='https://example.com/IpmiTool.yaml'>IpmiTool</a>" +
				"<sup class='visible-print-inline'>[2]</sup></li></ul>";

		String result = HtmlToMarkdownConverter.convert(html);

		assertTrue(result.contains("[Run and Debug Locally](run-and-debug.html)"));
		assertTrue(result.contains("[WBEMGenLUN](https://example.com/WBEMGenLUN.yaml)"));
		assertTrue(result.contains("[IpmiTool](https://example.com/IpmiTool.yaml)"));
		assertFalse(result.contains("[1]"));
		assertFalse(result.contains("[2]"));
	}
}
