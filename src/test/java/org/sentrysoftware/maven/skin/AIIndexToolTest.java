package org.sentrysoftware.maven.skin;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Sentry Maven Skin Tools
 * ჻჻჻჻჻჻
 * Copyright 2017 - 2024 Sentry Software
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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AIIndexToolTest {

	private AIIndexTool aiIndexTool;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		aiIndexTool = new AIIndexTool();
	}

	@Test
	void testExtractFrontmatter() {
		// Test with valid meta tags
		Document doc = Jsoup
				.parse(
						"<html><head>" +
								"<meta name=\"author\" content=\"John Doe\">" +
								"<meta name=\"description\" content=\"A sample document\">" +
								"<meta name=\"keywords\" content=\"test, sample, document\">" +
								"</head><body></body></html>");
		Element headElement = doc.head();

		String frontmatter = aiIndexTool.extractFrontmatter(headElement);

		assertTrue(frontmatter.contains("author: John Doe"));
		assertTrue(frontmatter.contains("description: A sample document"));
		assertTrue(frontmatter.contains("keywords: test, sample, document"));
	}

	@Test
	void testExtractFrontmatterWithSpecialCharacters() {
		// Test with content containing special characters
		Document doc = Jsoup
				.parse(
						"<html><head>" +
								"<meta name=\"description\" content=\"A document with: colons\">" +
								"<meta name=\"quote\" content=\"He said &quot;hello&quot;\">" +
								"</head><body></body></html>");
		Element headElement = doc.head();

		String frontmatter = aiIndexTool.extractFrontmatter(headElement);

		assertTrue(frontmatter.contains("description: \"A document with: colons\""));
		assertTrue(frontmatter.contains("quote: \"He said \\\"hello\\\"\""));
	}

	@Test
	void testExtractFrontmatterEmpty() {
		// Test with null input
		assertEquals("", aiIndexTool.extractFrontmatter(null));

		// Test with head element without meta tags
		Document doc = Jsoup.parse("<html><head><title>Test</title></head><body></body></html>");
		assertEquals("", aiIndexTool.extractFrontmatter(doc.head()));
	}

	@Test
	void testConvertHtmlToMarkdown() {
		// Test basic HTML to Markdown conversion
		Document doc = Jsoup
				.parse(
						"<html><head></head><body>" +
								"<h2>Title</h2><p>This is a paragraph with <strong>bold</strong> and <em>italic</em> text.</p>" +
								"</body></html>");
		Element bodyElement = doc.body();

		String markdown = HtmlToMarkdownConverter.convert(bodyElement);

		assertTrue(markdown.contains("Title"), "Should contain heading text");
		assertTrue(markdown.contains("**bold**"), "Should contain bold text");
		assertTrue(markdown.contains("*italic*"), "Should contain italic text");
	}

	@Test
	void testConvertHtmlToMarkdownWithList() {
		// Test list conversion
		Document doc = Jsoup
				.parse(
						"<html><head></head><body>" +
								"<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>" +
								"</body></html>");
		Element bodyElement = doc.body();

		String markdown = HtmlToMarkdownConverter.convert(bodyElement);

		assertTrue(markdown.contains("- Item 1"));
		assertTrue(markdown.contains("- Item 2"));
		assertTrue(markdown.contains("- Item 3"));
	}

	@Test
	void testConvertHtmlToMarkdownEmpty() {
		// Test with null input
		assertEquals("", HtmlToMarkdownConverter.convert((Element) null));

		// Test with empty body
		Document doc = Jsoup.parse("<html><head></head><body></body></html>");
		assertEquals("", HtmlToMarkdownConverter.convert(doc.body()));
	}

	@Test
	void testConvertToMarkdown() throws Exception {
		// Create a markdown file in a subdirectory
		Path subdir = tempDir.resolve("subdir");
		Path mdPath = subdir.resolve("feature.html.md");
		String outputDirectory = tempDir.toString();
		String docPath = "subdir/feature.html";
		Date publishDate = new Date(1736467200000L); // 2025-01-10
		String projectUrl = "https://example.com/docs";

		Document doc = Jsoup
				.parse(
						"<html><head>" +
								"<meta name=\"author\" content=\"Test Author\">" +
								"<meta name=\"description\" content=\"Test Description\">" +
								"</head><body>" +
								"<h1>Introduction</h1><p>This is the <strong>body</strong> content.</p>" +
								"</body></html>");

		String linkElement = aiIndexTool
				.convertToMarkdown(outputDirectory, docPath, doc.head(), doc.body(), publishDate, projectUrl);

		// Verify the returned link element (href is just the filename, relative to current doc, per llmstxt.org convention)
		assertEquals("<link rel=\"alternate\" type=\"text/markdown\" href=\"feature.html.md\">", linkElement);

		// Verify the file was created
		assertTrue(Files.exists(mdPath), "Markdown file should be created");

		// Read and verify content
		String content = Files.readString(mdPath);

		// Check frontmatter contains meta tags (no title - H1 is in body)
		assertTrue(content.startsWith("---"), "Should start with frontmatter delimiter");
		assertTrue(content.contains("author: Test Author"));
		assertTrue(content.contains("description: Test Description"));
		assertTrue(content.contains("date_published: 2025-01-10"));
		assertTrue(content.contains("date_modified: 2025-01-10"));
		assertTrue(content.contains("canonical_url: https://example.com/docs/subdir/feature.html"));

		// Check body content - H1 from HTML should be in the body
		assertTrue(content.contains("Introduction"), "Should contain Introduction heading from body");
		assertTrue(content.contains("**body**"));
	}

	@Test
	void testConvertToMarkdownWithoutMetaTags() throws Exception {
		// Test creating markdown without meta tags (no frontmatter block)
		Path mdPath = tempDir.resolve("simple.html.md");
		String outputDirectory = tempDir.toString();
		String docPath = "simple.html";

		Document doc = Jsoup
				.parse(
						"<html><head></head><body>" +
								"<h1>Simple Page</h1><p>Simple content</p>" +
								"</body></html>");

		String linkElement = aiIndexTool.convertToMarkdown(outputDirectory, docPath, doc.head(), doc.body(), null, null);

		// Verify the returned link element (href is just the filename, per llmstxt.org convention)
		assertEquals("<link rel=\"alternate\" type=\"text/markdown\" href=\"simple.html.md\">", linkElement);

		// Verify the file was created
		assertTrue(Files.exists(mdPath), "Markdown file should be created");

		// Read and verify content
		String content = Files.readString(mdPath);

		// No frontmatter when there are no meta tags
		assertFalse(content.startsWith("---"), "Should not have frontmatter when no meta tags");

		// Check content - H1 should be in body
		assertTrue(content.contains("Simple Page"));
		assertTrue(content.contains("Simple content"));
	}

	@Test
	void testConvertToMarkdownNullDocPath() throws Exception {
		// Test with null outputDirectory or docPath - should return empty string
		Document doc = Jsoup.parse("<html><head></head><body></body></html>");
		assertEquals("", aiIndexTool.convertToMarkdown(null, "doc.html", doc.head(), doc.body(), null, null));
		assertEquals("", aiIndexTool.convertToMarkdown(tempDir.toString(), null, doc.head(), doc.body(), null, null));
	}

	@Test
	void testConvertToMarkdownEmptyDocPath() throws Exception {
		// Test with empty outputDirectory or docPath - should return empty string
		Document doc = Jsoup.parse("<html><head></head><body></body></html>");
		assertEquals("", aiIndexTool.convertToMarkdown("", "doc.html", doc.head(), doc.body(), null, null));
		assertEquals("", aiIndexTool.convertToMarkdown(tempDir.toString(), "", doc.head(), doc.body(), null, null));
	}

	@Test
	void testConvertToMarkdownUpdatesExistingFile() throws Exception {
		// Test that the tool updates existing files
		Path mdPath = tempDir.resolve("update.html.md");
		String outputDirectory = tempDir.toString();
		String docPath = "update.html";

		// Create initial file
		Document doc1 = Jsoup
				.parse(
						"<html><head></head><body>" +
								"<h1>Original Title</h1><p>Original content</p>" +
								"</body></html>");
		aiIndexTool.convertToMarkdown(outputDirectory, docPath, doc1.head(), doc1.body(), null, null);

		// Update the file
		Document doc2 = Jsoup
				.parse(
						"<html><head></head><body>" +
								"<h1>Updated Title</h1><p>Updated content</p>" +
								"</body></html>");
		aiIndexTool.convertToMarkdown(outputDirectory, docPath, doc2.head(), doc2.body(), null, null);

		// Verify the file was updated
		String content = Files.readString(mdPath);
		assertTrue(content.contains("Updated Title"), "H1 should be updated in body");
		assertTrue(content.contains("Updated content"));
		assertFalse(content.contains("Original"));
	}

	@Test
	void testConvertToMarkdownWithRealHtmlContent() throws Exception {
		// Test with content similar to real documentation
		Path mdPath = tempDir.resolve("docs").resolve("guide.html.md");
		String outputDirectory = tempDir.toString();
		String docPath = "docs/guide.html";

		Document doc = Jsoup
				.parse(
						"<html><head>" +
								"<meta name=\"author\" content=\"Sentry Software\">" +
								"<meta name=\"description\" content=\"User Guide for the Application\">" +
								"<meta name=\"keywords\" content=\"guide, documentation, help\">" +
								"</head><body>" +
								"<h1>Getting Started</h1>" +
								"<p>Welcome to the user guide. This document will help you get started.</p>" +
								"<h2>Prerequisites</h2>" +
								"<ul>" +
								"<li>Java 11 or later</li>" +
								"<li>Maven 3.6 or later</li>" +
								"</ul>" +
								"<h2>Installation</h2>" +
								"<p>Run the following command:</p>" +
								"<pre><code>mvn install</code></pre>" +
								"</body></html>");

		aiIndexTool.convertToMarkdown(outputDirectory, docPath, doc.head(), doc.body(), null, null);

		// Verify the file was created
		assertTrue(Files.exists(mdPath), "Markdown file should be created");

		// Read and verify content
		String content = Files.readString(mdPath);

		// Check structure - meta tags in frontmatter, H1 in body
		assertTrue(content.startsWith("---"), "Should have frontmatter at start");
		assertTrue(content.contains("author: Sentry Software"));
		// H1 should be in body, not in frontmatter
		assertTrue(content.contains("Getting Started"), "Should contain Getting Started heading from body");
		assertTrue(content.contains("Prerequisites"), "Should contain Prerequisites heading");
		assertTrue(content.contains("Java 11 or later"), "Should contain list item");
		assertTrue(content.contains("Installation"), "Should contain Installation heading");
	}

	// ==================== updateLlmsTxt Tests ====================

	@Test
	void testUpdateLlmsTxtCreatesNewFile() throws Exception {
		// Test creating a new llms.txt file
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/getting-started.html",
						"Getting Started",
						"My Project",
						"A great project for doing things",
						"Documentation");

		assertTrue(Files.exists(llmsTxtPath), "llms.txt should be created");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("# My Project"));
		assertTrue(content.contains("> A great project for doing things"));
		assertTrue(content.contains("## Documentation"));
		assertTrue(content.contains("- [Getting Started](docs/getting-started.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtAddsToExistingSection() throws Exception {
		// Test adding to an existing section
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		// Add first entry
		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/page1.html",
						"Page 1",
						"My Project",
						"Project description",
						"Documentation");

		// Add second entry to same section
		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/page2.html",
						"Page 2",
						"My Project",
						"Project description",
						"Documentation");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("- [Page 1](docs/page1.html.md)"));
		assertTrue(content.contains("- [Page 2](docs/page2.html.md)"));
		// Should only have one Documentation section
		assertEquals(1, content.split("## Documentation").length - 1);
	}

	@Test
	void testUpdateLlmsTxtAddsToDifferentSections() throws Exception {
		// Test adding entries to different sections
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/guide.html",
						"User Guide",
						"My Project",
						"Project description",
						"Documentation");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"api/index.html",
						"API Reference",
						"My Project",
						"Project description",
						"API");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("## Documentation"));
		assertTrue(content.contains("## API"));
		assertTrue(content.contains("- [User Guide](docs/guide.html.md)"));
		assertTrue(content.contains("- [API Reference](api/index.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtUpdatesExistingEntry() throws Exception {
		// Test updating an existing entry (same docPath, different title)
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		// Add initial entry
		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/page.html",
						"Old Title",
						"My Project",
						"Project description",
						"Documentation");

		// Update with new title
		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/page.html",
						"New Title",
						"My Project",
						"Project description",
						"Documentation");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("- [New Title](docs/page.html.md)"));
		assertFalse(content.contains("Old Title"));
		// Should only have one entry for this path
		assertEquals(1, content.split("docs/page.html.md").length - 1);
	}

	@Test
	void testUpdateLlmsTxtUsesOtherSectionForNullSection() throws Exception {
		// Test that null section uses "Other"
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"misc/page.html",
						"Miscellaneous Page",
						"My Project",
						"Project description",
						null);

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("## Other"));
		assertTrue(content.contains("- [Miscellaneous Page](misc/page.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtUsesOtherSectionForEmptySection() throws Exception {
		// Test that empty section uses "Other"
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"misc/page.html",
						"Miscellaneous Page",
						"My Project",
						"Project description",
						"");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("## Other"));
		assertTrue(content.contains("- [Miscellaneous Page](misc/page.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtPreservesExistingContent() throws Exception {
		// Test that existing content is preserved when adding new entries
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		// Create initial file with some content
		String initialContent = "# Existing Project\n\n" +
				"> Existing description\n\n" +
				"## Existing Section\n\n" +
				"- [Existing Page](existing.html)\n";
		Files.writeString(llmsTxtPath, initialContent);

		// Add new entry
		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"new/page.html",
						"New Page",
						null, // Don't override project name
						null, // Don't override description
						"New Section");

		String content = Files.readString(llmsTxtPath);
		// Existing content should be preserved
		assertTrue(content.contains("# Existing Project"));
		assertTrue(content.contains("> Existing description"));
		assertTrue(content.contains("## Existing Section"));
		assertTrue(content.contains("- [Existing Page](existing.html)"));
		// New content should be added
		assertTrue(content.contains("## New Section"));
		assertTrue(content.contains("- [New Page](new/page.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtHandlesNullDocPath() {
		// Test with null docPath - should do nothing
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						null,
						"Title",
						"Project",
						"Description",
						"Section");

		assertFalse(Files.exists(llmsTxtPath), "Should not create file with null docPath");
	}

	@Test
	void testUpdateLlmsTxtHandlesEmptyDocPath() {
		// Test with empty docPath - should do nothing
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"",
						"Title",
						"Project",
						"Description",
						"Section");

		assertFalse(Files.exists(llmsTxtPath), "Should not create file with empty docPath");
	}

	@Test
	void testUpdateLlmsTxtHandlesNullLlmsTxtPath() {
		// Test with null llmsTxtPath - should do nothing (no exception)
		aiIndexTool
				.updateLlmsTxt(
						null,
						"docs/page.html",
						"Title",
						"Project",
						"Description",
						"Section");
		// No exception means success
	}

	@Test
	void testUpdateLlmsTxtUsesDocPathAsTitleWhenTitleIsNull() throws Exception {
		// Test that docPath is used as title when docTitle is null
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/my-page.html",
						null,
						"Project",
						"Description",
						"Section");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("- [docs/my-page.html](docs/my-page.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtWithAbsoluteUrls() throws Exception {
		// Test that absolute URLs are generated when projectUrl is provided
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"docs/getting-started.html",
						"Getting Started",
						"My Project",
						"A great project",
						"Documentation",
						"https://example.com/myproject");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("# My Project"));
		assertTrue(content.contains("## Documentation"));
		assertTrue(content.contains("- [Getting Started](https://example.com/myproject/docs/getting-started.html.md)"));
	}

	@Test
	void testUpdateLlmsTxtWithAbsoluteUrlsTrailingSlash() throws Exception {
		// Test that absolute URLs work correctly when projectUrl has trailing slash
		Path llmsTxtPath = tempDir.resolve("llms.txt");

		aiIndexTool
				.updateLlmsTxt(
						llmsTxtPath.toString(),
						"api/reference.html",
						"API Reference",
						"My Project",
						"A great project",
						"API",
						"https://example.com/myproject/");

		String content = Files.readString(llmsTxtPath);
		assertTrue(content.contains("- [API Reference](https://example.com/myproject/api/reference.html.md)"));
	}

	@Test
	void testParseLlmsTxt() {
		// Test parsing of llms.txt content
		String content = "# Test Project\n\n" +
				"> Test description\n\n" +
				"## Section One\n\n" +
				"- [Title 1](path1.html)\n" +
				"- [Title 2](path2.html)\n\n" +
				"## Section Two\n\n" +
				"- [Title 3](path3.html)\n";

		AIIndexTool.LlmsTxtContent parsed = aiIndexTool.parseLlmsTxt(content);

		assertEquals("Test Project", parsed.getProjectName());
		assertEquals("Test description", parsed.getProjectDescription());
		assertEquals(2, parsed.getSections().size());
		assertTrue(parsed.getSections().containsKey("Section One"));
		assertTrue(parsed.getSections().containsKey("Section Two"));
		assertEquals(2, parsed.getSections().get("Section One").size());
		assertEquals(1, parsed.getSections().get("Section Two").size());
		assertEquals("Title 1", parsed.getSections().get("Section One").get(0).getTitle());
		assertEquals("path1.html", parsed.getSections().get("Section One").get(0).getPath());
	}

	@Test
	void testParseLlmsTxtEmptyContent() {
		// Test parsing of empty content
		AIIndexTool.LlmsTxtContent parsed = aiIndexTool.parseLlmsTxt("");

		assertEquals("", parsed.getProjectName());
		assertEquals("", parsed.getProjectDescription());
		assertTrue(parsed.getSections().isEmpty());
	}

	@Test
	void testParseLlmsTxtNullContent() {
		// Test parsing of null content
		AIIndexTool.LlmsTxtContent parsed = aiIndexTool.parseLlmsTxt(null);

		assertEquals("", parsed.getProjectName());
		assertEquals("", parsed.getProjectDescription());
		assertTrue(parsed.getSections().isEmpty());
	}

	@Test
	void testBuildLlmsTxt() {
		// Test building llms.txt content
		AIIndexTool.LlmsTxtContent content = new AIIndexTool.LlmsTxtContent();
		content.setProjectName("My Project");
		content.setProjectDescription("My description");
		content.getSections().put("Section A", new java.util.ArrayList<>());
		content.getSections().get("Section A").add(new AIIndexTool.LinkEntry("Link 1", "link1.html"));
		content.getSections().get("Section A").add(new AIIndexTool.LinkEntry("Link 2", "link2.html"));

		String result = aiIndexTool.buildLlmsTxt(content);

		assertTrue(result.contains("# My Project"));
		assertTrue(result.contains("> My description"));
		assertTrue(result.contains("## Section A"));
		assertTrue(result.contains("- [Link 1](link1.html)"));
		assertTrue(result.contains("- [Link 2](link2.html)"));
	}

	@Test
	void testBuildLlmsTxtWithMultiLineDescription() {
		// Test that multi-line descriptions are properly formatted with > on each line
		AIIndexTool.LlmsTxtContent content = new AIIndexTool.LlmsTxtContent();
		content.setProjectName("My Project");
		content.setProjectDescription("First line of description.\nSecond line of description.\nThird line.");
		content.getSections().put("Docs", new java.util.ArrayList<>());

		String result = aiIndexTool.buildLlmsTxt(content);

		assertTrue(result.contains("# My Project"));
		assertTrue(result.contains("> First line of description.\n"));
		assertTrue(result.contains("> Second line of description.\n"));
		assertTrue(result.contains("> Third line.\n"));
		// Ensure we don't have improperly formatted blockquotes (lines without >)
		assertFalse(result.contains("\nSecond line"));
		assertFalse(result.contains("\nThird line."));
	}

	@Test
	void testParseLlmsTxtWithMultiLineDescription() {
		// Test parsing multi-line blockquote descriptions
		String content = "# Test Project\n\n" +
				"> First line of description.\n" +
				"> Second line of description.\n" +
				"> Third line.\n\n" +
				"## Section One\n\n" +
				"- [Title 1](path1.html)\n";

		AIIndexTool.LlmsTxtContent parsed = aiIndexTool.parseLlmsTxt(content);

		assertEquals("Test Project", parsed.getProjectName());
		assertEquals(
				"First line of description.\nSecond line of description.\nThird line.",
				parsed.getProjectDescription());
		assertEquals(1, parsed.getSections().size());
	}

}
