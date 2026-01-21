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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.generic.SafeConfig;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * aiIndexTool is a Velocity tool that creates AI-ready Markdown index files.
 * <p>
 * It converts HTML documentation pages to Markdown format with YAML frontmatter,
 * making them suitable for AI consumption and processing.
 * </p>
 */
@DefaultKey("aiIndexTool")
public class AIIndexTool extends SafeConfig {

	/**
	 * UTF-8 Charset
	 */
	static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Static initialization error, if any
	 */
	private static final Throwable INIT_ERROR;

	static {
		Throwable error = null;
		try {
			// Force loading of HtmlToMarkdownConverter to catch any class loading issues early
			Class.forName("org.sentrysoftware.maven.skin.HtmlToMarkdownConverter");
		} catch (Throwable t) {
			error = t;
		}
		INIT_ERROR = error;
	}

	/**
	 * Creates a new instance
	 */
	public AIIndexTool() {
		// Log any static initialization errors
		if (INIT_ERROR != null) {
			getLog().error("AIIndexTool: Static initialization failed", INIT_ERROR);
		}
	}

	/**
	 * Converts an HTML document to a Markdown file.
	 * <p>
	 * The Markdown file will contain:
	 * </p>
	 * <ul>
	 * <li>YAML frontmatter with meta tags extracted from HTML header</li>
	 * <li>The body content converted from HTML to Markdown</li>
	 * </ul>
	 *
	 * @param outputDirectory Actual root directory of the site on the file system
	 * @param docPath Logical path to the document relative to outputDirectory
	 *        (e.g., "subdir/feature.html"), the .md file will be created at the
	 *        same location with .md extension
	 * @param headElement The HTML head element (containing meta tags)
	 * @param bodyElement The HTML body element
	 * @param publishDate The publication date for the document (used for date_published
	 *        and date_modified frontmatter fields)
	 * @param projectUrl The base URL of the project site (used to build canonical_url)
	 * @return An HTML link element for the alternate Markdown version, e.g.
	 *         {@code <link rel="alternate" type="text/markdown" href="/docs/page.md">},
	 *         or an empty string if conversion fails
	 */
	public String convertToMarkdown(
			final String outputDirectory,
			final String docPath,
			final Element headElement,
			final Element bodyElement,
			final Date publishDate,
			final String projectUrl) {

		if (outputDirectory == null || outputDirectory.isEmpty() || docPath == null || docPath.isEmpty()) {
			return "";
		}

		try {
			// Calculate the Markdown file path (replace .html with .md)
			String mdRelativePath = docPath.replaceFirst("\\.html$", ".md");
			Path markdownPath = Paths.get(outputDirectory, mdRelativePath);

			// Calculate the href for the link element (just the filename, relative to current doc)
			Path fileNamePath = markdownPath.getFileName();
			if (fileNamePath == null) {
				return "";
			}
			String mdFilename = fileNamePath.toString();

			// Build the Markdown content
			StringBuilder markdownContent = new StringBuilder();

			// Extract frontmatter from meta tags
			String metaFrontmatter = extractFrontmatter(headElement);

			// Build additional frontmatter fields (date_published, date_modified, canonical_url)
			StringBuilder additionalFrontmatter = new StringBuilder();
			if (publishDate != null) {
				String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(publishDate);
				additionalFrontmatter.append("date_published: ").append(formattedDate).append("\n");
				additionalFrontmatter.append("date_modified: ").append(formattedDate).append("\n");
			}
			if (projectUrl != null && !projectUrl.isEmpty()) {
				// Build canonical URL: projectUrl + docPath (normalized)
				String baseUrl = projectUrl.endsWith("/") ? projectUrl : projectUrl + "/";
				String canonicalUrl = baseUrl + docPath.replace("\\", "/");
				additionalFrontmatter.append("canonical_url: ").append(canonicalUrl).append("\n");
			}

			// Combine frontmatter if we have any content
			String combinedFrontmatter = metaFrontmatter + additionalFrontmatter.toString();

			// Build the YAML frontmatter block if we have any frontmatter
			if (!combinedFrontmatter.isEmpty()) {
				markdownContent.append("---\n");
				markdownContent.append(combinedFrontmatter);
				markdownContent.append("---\n\n");
			}

			// Convert body HTML to Markdown and append
			String bodyMarkdown = HtmlToMarkdownConverter.convert(bodyElement);
			markdownContent.append(bodyMarkdown);

			// Ensure parent directories exist
			Path parentDir = markdownPath.getParent();
			if (parentDir != null) {
				Files.createDirectories(parentDir);
			}

			// Write the Markdown file
			Helper.writeText(markdownPath.toFile(), markdownContent.toString(), UTF8_CHARSET);

			// Return the link element for the HTML header (relative href, just the filename)
			return "<link rel=\"alternate\" type=\"text/markdown\" href=\"" + mdFilename + "\">";

		} catch (Exception e) {
			// Log error but don't interrupt the site generation
			getLog().error("AIIndexTool: Failed to convert {} to Markdown: {}", docPath, e.getMessage());
			return "";
		}
	}

	/**
	 * Extracts frontmatter content from HTML meta tags.
	 * <p>
	 * Meta tags with name and content attributes are converted to YAML-style frontmatter.
	 * </p>
	 *
	 * @param headElement The HTML head element containing meta tags
	 * @return YAML-formatted frontmatter string (without the --- delimiters)
	 */
	String extractFrontmatter(final Element headElement) {
		if (headElement == null) {
			return "";
		}

		// Select meta tags with name and content attributes
		Elements metaTags = headElement.select("meta[name][content]");

		// Use LinkedHashMap to preserve insertion order
		Map<String, String> frontmatterMap = new LinkedHashMap<>();

		for (Element meta : metaTags) {
			String name = meta.attr("name");
			String content = meta.attr("content");
			if (!name.isEmpty() && !content.isEmpty()) {
				frontmatterMap.put(name, content);
			}
		}

		if (frontmatterMap.isEmpty()) {
			return "";
		}

		// Build YAML frontmatter
		StringBuilder frontmatter = new StringBuilder();
		for (Map.Entry<String, String> entry : frontmatterMap.entrySet()) {
			frontmatter.append(entry.getKey()).append(": ").append(escapeYamlValue(entry.getValue())).append("\n");
		}

		return frontmatter.toString();
	}

	/**
	 * Escapes a value for use in YAML frontmatter.
	 *
	 * @param value The value to escape
	 * @return The escaped value, quoted if necessary
	 */
	private String escapeYamlValue(final String value) {
		if (value.contains(":") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
		}
		return value;
	}

	/**
	 * Default section name for entries without a specified section.
	 */
	private static final String DEFAULT_SECTION = "Other";

	/**
	 * Pattern to match Markdown links in the format [title](path).
	 */
	private static final Pattern LINK_PATTERN = Pattern.compile("^-\\s*\\[([^\\]]+)\\]\\(([^)]+)\\)\\s*$");

	/**
	 * Updates or creates an llms.txt file by adding or updating a documentation entry.
	 * <p>
	 * The llms.txt file follows a specific Markdown format with sections containing
	 * links to documentation pages. This method is typically called for each generated
	 * HTML file during site generation.
	 * </p>
	 *
	 * @param llmsTxtPath Path to the llms.txt file
	 * @param docPath Path relative to the root of the site (e.g., "subdir/page.html")
	 * @param docTitle Title of the document
	 * @param projectName Name of the project (used in the H1 header)
	 * @param projectDescription Description of the project (used in the blockquote)
	 * @param section Name of the section where this entry should be placed;
	 *        if null or empty, the entry is placed in the "Other" section
	 */
	public void updateLlmsTxt(
			final String llmsTxtPath,
			final String docPath,
			final String docTitle,
			final String projectName,
			final String projectDescription,
			final String section) {

		if (llmsTxtPath == null || llmsTxtPath.isEmpty() || docPath == null || docPath.isEmpty()) {
			return;
		}

		try {
			Path path = Paths.get(llmsTxtPath);

			// Parse existing content or create new structure
			LlmsTxtContent content;
			if (Files.exists(path)) {
				String existingContent = Files.readString(path, UTF8_CHARSET);
				content = parseLlmsTxt(existingContent);
				// Update project info if provided (don't overwrite with null/empty)
				if (projectName != null && !projectName.isEmpty()) {
					content.setProjectName(projectName);
				}
				if (projectDescription != null && !projectDescription.isEmpty()) {
					content.setProjectDescription(projectDescription);
				}
			} else {
				content = new LlmsTxtContent();
				content.setProjectName(projectName != null ? projectName : "");
				content.setProjectDescription(projectDescription != null ? projectDescription : "");
			}

			// Determine target section
			String targetSection = (section == null || section.isEmpty()) ? DEFAULT_SECTION : section;

			// Get or create the section
			List<LinkEntry> sectionEntries = content.getSections().computeIfAbsent(targetSection, k -> new ArrayList<>());

			// Convert docPath from .html to .md for the link
			String mdDocPath = docPath.replaceFirst("\\.html$", ".md");

			// Update or add the entry
			String title = docTitle != null ? docTitle : docPath;
			boolean found = false;
			for (LinkEntry entry : sectionEntries) {
				if (entry.getPath().equals(mdDocPath)) {
					entry.setTitle(title);
					found = true;
					break;
				}
			}
			if (!found) {
				sectionEntries.add(new LinkEntry(title, mdDocPath));
			}

			// Ensure parent directories exist
			Path parentDir = path.getParent();
			if (parentDir != null) {
				Files.createDirectories(parentDir);
			}

			// Write the updated content
			Helper.writeText(path.toFile(), buildLlmsTxt(content), UTF8_CHARSET);

		} catch (Exception e) {
			// Log error but don't interrupt the site generation
			getLog().error("AIIndexTool: Failed to update llms.txt at {}: {}", llmsTxtPath, e.getMessage());
		}
	}

	/**
	 * Parses an existing llms.txt file content.
	 *
	 * @param content The file content to parse
	 * @return Parsed content structure
	 */
	LlmsTxtContent parseLlmsTxt(final String content) {
		LlmsTxtContent result = new LlmsTxtContent();

		if (content == null || content.isEmpty()) {
			return result;
		}

		String[] lines = content.split("\n");
		String currentSection = null;

		for (String line : lines) {
			String trimmedLine = line.trim();

			// Parse project name (H1)
			if (trimmedLine.startsWith("# ") && result.getProjectName().isEmpty()) {
				result.setProjectName(trimmedLine.substring(2).trim());
				continue;
			}

			// Parse project description (blockquote)
			if (trimmedLine.startsWith("> ") && result.getProjectDescription().isEmpty()) {
				result.setProjectDescription(trimmedLine.substring(2).trim());
				continue;
			}

			// Parse section header (H2)
			if (trimmedLine.startsWith("## ")) {
				currentSection = trimmedLine.substring(3).trim();
				result.getSections().computeIfAbsent(currentSection, k -> new ArrayList<>());
				continue;
			}

			// Parse link entry
			if (currentSection != null && trimmedLine.startsWith("- ")) {
				Matcher matcher = LINK_PATTERN.matcher(trimmedLine);
				if (matcher.matches()) {
					String title = matcher.group(1);
					String path = matcher.group(2);
					result.getSections().get(currentSection).add(new LinkEntry(title, path));
				}
			}
		}

		return result;
	}

	/**
	 * Builds the llms.txt file content from the parsed structure.
	 *
	 * @param content The content structure to serialize
	 * @return The formatted llms.txt content
	 */
	String buildLlmsTxt(final LlmsTxtContent content) {
		StringBuilder sb = new StringBuilder();

		// Write project name
		sb.append("# ").append(content.getProjectName()).append("\n\n");

		// Write project description
		if (!content.getProjectDescription().isEmpty()) {
			sb.append("> ").append(content.getProjectDescription()).append("\n");
		}

		// Write sections
		for (Map.Entry<String, List<LinkEntry>> section : content.getSections().entrySet()) {
			sb.append("\n## ").append(section.getKey()).append("\n\n");
			for (LinkEntry entry : section.getValue()) {
				sb.append("- [").append(entry.getTitle()).append("](").append(entry.getPath()).append(")\n");
			}
		}

		return sb.toString();
	}

	/**
	 * Internal class to hold the parsed structure of an llms.txt file.
	 */
	static class LlmsTxtContent {

		private String projectName = "";
		private String projectDescription = "";
		private Map<String, List<LinkEntry>> sections = new LinkedHashMap<>();

		String getProjectName() {
			return projectName;
		}

		void setProjectName(final String projectName) {
			this.projectName = projectName;
		}

		String getProjectDescription() {
			return projectDescription;
		}

		void setProjectDescription(final String projectDescription) {
			this.projectDescription = projectDescription;
		}

		Map<String, List<LinkEntry>> getSections() {
			return sections;
		}
	}

	/**
	 * Internal class to hold a link entry (title and path).
	 */
	static class LinkEntry {

		private String title;
		private String path;

		LinkEntry(final String title, final String path) {
			this.title = title;
			this.path = path;
		}

		String getTitle() {
			return title;
		}

		void setTitle(final String title) {
			this.title = title;
		}

		String getPath() {
			return path;
		}
	}

}
