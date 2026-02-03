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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.generic.ValueParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link MavenFilteringTool}.
 */
class MavenFilteringToolTest {

	private static MavenFilteringTool tool;
	private static MockMavenProject mockProject;

	/**
	 * Mock Maven project for testing
	 */
	static class MockMavenProject {
		private String version = "1.0.0";
		private String name = "Test Project";
		private String groupId = "com.example";
		private String artifactId = "test-artifact";
		private String description = "A test project";
		private String url = "https://example.com";
		private String inceptionYear = "2024";

		public String getVersion() {
			return version;
		}

		public String getName() {
			return name;
		}

		public String getGroupId() {
			return groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

		public String getDescription() {
			return description;
		}

		public String getUrl() {
			return url;
		}

		public String getInceptionYear() {
			return inceptionYear;
		}
	}

	/**
	 * Mock ToolContext for testing
	 */
	static class MockToolContext extends ToolContext {
		private Map<String, Object> data = new HashMap<>();

		public Object put(String key, Object value) {
			return data.put(key, value);
		}

		@Override
		public Object get(String key) {
			return data.get(key);
		}
	}

	/**
	 * Mock ValueParser for testing
	 */
	static class MockValueParser extends ValueParser {
		private Map<String, Object> data = new HashMap<>();

		public Object put(String key, Object value) {
			return data.put(key, value);
		}

		@Override
		public Object get(String key) {
			return data.get(key);
		}
	}

	@BeforeAll
	static void setUp() {
		// Create the tool
		tool = new MavenFilteringTool();

		// Create a mock project
		mockProject = new MockMavenProject();

		// Create mock properties
		Map<String, String> properties = new HashMap<>();
		properties.put("custom.property", "customValue");
		properties.put("app.name", "MyApp");

		// Create mock context
		MockToolContext context = new MockToolContext();
		context.put("project", mockProject);
		context.put("properties", properties);

		// Create mock value parser
		MockValueParser valueParser = new MockValueParser();
		valueParser.put("velocityContext", context);

		// Configure the tool
		tool.configure(valueParser);
	}

	@Test
	void testFilterNull() {
		// Test null input
		String result = tool.filter(null);
		assertNull(result);
	}

	@Test
	void testFilterEmpty() {
		// Test empty input
		String result = tool.filter("");
		assertEquals("", result);
	}

	@Test
	void testFilterNoPlaceholders() {
		// Test content without placeholders
		String content = "This is plain text with no placeholders.";
		String result = tool.filter(content);
		assertEquals(content, result);
	}

	@Test
	void testFilterProjectVersion() {
		// Test ${project.version}
		String content = "Version: ${project.version}";
		String result = tool.filter(content);
		assertEquals("Version: 1.0.0", result);
	}

	@Test
	void testFilterProjectName() {
		// Test ${project.name}
		String content = "Project: ${project.name}";
		String result = tool.filter(content);
		assertEquals("Project: Test Project", result);
	}

	@Test
	void testFilterProjectGroupId() {
		// Test ${project.groupId}
		String content = "Group: ${project.groupId}";
		String result = tool.filter(content);
		assertEquals("Group: com.example", result);
	}

	@Test
	void testFilterProjectArtifactId() {
		// Test ${project.artifactId}
		String content = "Artifact: ${project.artifactId}";
		String result = tool.filter(content);
		assertEquals("Artifact: test-artifact", result);
	}

	@Test
	void testFilterProjectDescription() {
		// Test ${project.description}
		String content = "Description: ${project.description}";
		String result = tool.filter(content);
		assertEquals("Description: A test project", result);
	}

	@Test
	void testFilterProjectUrl() {
		// Test ${project.url}
		String content = "URL: ${project.url}";
		String result = tool.filter(content);
		assertEquals("URL: https://example.com", result);
	}

	@Test
	void testFilterProjectInceptionYear() {
		// Test ${project.inceptionYear}
		String content = "Year: ${project.inceptionYear}";
		String result = tool.filter(content);
		assertEquals("Year: 2024", result);
	}

	@Test
	void testFilterCustomProperty() {
		// Test custom property from pom.xml
		String content = "Custom: ${custom.property}";
		String result = tool.filter(content);
		assertEquals("Custom: customValue", result);
	}

	@Test
	void testFilterAppName() {
		// Test another custom property
		String content = "App: ${app.name}";
		String result = tool.filter(content);
		assertEquals("App: MyApp", result);
	}

	@Test
	void testFilterMultipleProperties() {
		// Test multiple properties in the same content
		String content = "${project.name} version ${project.version} (${project.groupId}:${project.artifactId})";
		String result = tool.filter(content);
		assertEquals("Test Project version 1.0.0 (com.example:test-artifact)", result);
	}

	@Test
	void testFilterUndefinedProperty() {
		// Test undefined property - should remain as-is
		String content = "Value: ${undefined.property}";
		String result = tool.filter(content);
		assertEquals("Value: ${undefined.property}", result);
	}

	@Test
	void testFilterMixedDefinedAndUndefined() {
		// Test mix of defined and undefined properties
		String content = "${project.name} uses ${undefined.property} version ${project.version}";
		String result = tool.filter(content);
		assertEquals("Test Project uses ${undefined.property} version 1.0.0", result);
	}

	@Test
	void testFilterSystemProperty() {
		// Test system property
		String javaVersion = System.getProperty("java.version");
		String content = "Java version: ${java.version}";
		String result = tool.filter(content);
		assertEquals("Java version: " + javaVersion, result);
	}

	@Test
	void testFilterEnvironmentVariable() {
		// Test environment variable
		String path = System.getenv("PATH");
		if (path != null) {
			String content = "PATH: ${env.PATH}";
			String result = tool.filter(content);
			assertEquals("PATH: " + path, result);
		}
	}

	@Test
	void testFilterComplexContent() {
		// Test more complex HTML-like content
		String content = "<h1>${project.name}</h1>\n" +
				"<p>Version: ${project.version}</p>\n" +
				"<p>Description: ${project.description}</p>\n" +
				"<p>Custom: ${custom.property}</p>";

		String expected = "<h1>Test Project</h1>\n" +
				"<p>Version: 1.0.0</p>\n" +
				"<p>Description: A test project</p>\n" +
				"<p>Custom: customValue</p>";

		String result = tool.filter(content);
		assertEquals(expected, result);
	}

	@Test
	void testFilterWithPomPrefix() {
		// Test ${pom.version} as an alternative to ${project.version}
		String content = "Version: ${pom.version}";
		String result = tool.filter(content);
		assertEquals("Version: 1.0.0", result);
	}

	@Test
	void testFilterWithNestedPlaceholders() {
		// Test content with nested-like structure
		String content = "GAV: ${project.groupId}:${project.artifactId}:${project.version}";
		String result = tool.filter(content);
		assertEquals("GAV: com.example:test-artifact:1.0.0", result);
	}

	@Test
	void testFilterUnconfiguredTool() {
		// Test tool that hasn't been configured
		MavenFilteringTool unconfiguredTool = new MavenFilteringTool();
		String content = "Version: ${project.version}";
		String result = unconfiguredTool.filter(content);

		// Should return original content since tool isn't configured
		assertEquals("Version: ${project.version}", result);
	}
}
