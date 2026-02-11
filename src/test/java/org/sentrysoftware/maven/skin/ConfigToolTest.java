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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigToolTest {

	private ConfigTool configTool;

	/**
	 * Mock site model for testing
	 */
	private static class MockSiteModel {
		private final java.util.Map<String, String> customValues = new java.util.HashMap<>();

		public void setCustomValue(final String key, final String value) {
			customValues.put(key, value);
		}

		public String getCustomValue(final String key) {
			return customValues.get(key);
		}
	}

	@BeforeEach
	void setUp() {
		configTool = new ConfigTool();
	}

	@Test
	void testGetValueWithFrontMatter() {
		String headContent = "<meta name=\"interpolation\" content=\"none\"/>";
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("interpolation", "maven");

		// Front matter should override site value
		String result = configTool.getValue(site, headContent, "interpolation", "default");
		assertEquals("none", result);
	}

	@Test
	void testGetValueFromSiteOnly() {
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("interpolation", "maven");

		// Should get value from site.xml
		String result = configTool.getValue(site, null, "interpolation", "default");
		assertEquals("maven", result);
	}

	@Test
	void testGetValueWithDefault() {
		MockSiteModel site = new MockSiteModel();

		// Should return default value
		String result = configTool.getValue(site, null, "nonExistent", "default");
		assertEquals("default", result);
	}

	@Test
	void testGetValueWithNullSiteAndHeadContent() {
		// Should return default value
		String result = configTool.getValue(null, null, "test", "default");
		assertEquals("default", result);
	}

	@Test
	void testGetValueWithEmptyHeadContent() {
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("test", "siteValue");

		// Empty head content should fall back to site value
		String result = configTool.getValue(site, "", "test", "default");
		assertEquals("siteValue", result);
	}

	@Test
	void testGetValueWithMultipleMetaTags() {
		String headContent = "<meta name=\"interpolation\" content=\"none\"/>" +
				"<meta name=\"showToc\" content=\"false\"/>" +
				"<meta name=\"tocMaxDepth\" content=\"3\"/>";

		String result1 = configTool.getValue(null, headContent, "interpolation", "default");
		assertEquals("none", result1);

		String result2 = configTool.getValue(null, headContent, "showToc", "default");
		assertEquals("false", result2);

		String result3 = configTool.getValue(null, headContent, "tocMaxDepth", "default");
		assertEquals("3", result3);
	}

	@Test
	void testGetBooleanValueTrue() {
		String headContent = "<meta name=\"showToc\" content=\"true\"/>";

		assertTrue(configTool.getBooleanValue(null, headContent, "showToc", false));
	}

	@Test
	void testGetBooleanValueFalse() {
		String headContent = "<meta name=\"showToc\" content=\"false\"/>";

		assertFalse(configTool.getBooleanValue(null, headContent, "showToc", true));
	}

	@Test
	void testGetBooleanValueYes() {
		String headContent = "<meta name=\"checkImages\" content=\"yes\"/>";

		assertTrue(configTool.getBooleanValue(null, headContent, "checkImages", false));
	}

	@Test
	void testGetBooleanValueNo() {
		String headContent = "<meta name=\"checkImages\" content=\"no\"/>";

		assertFalse(configTool.getBooleanValue(null, headContent, "checkImages", true));
	}

	@Test
	void testGetBooleanValueNumeric() {
		String headContent1 = "<meta name=\"enabled\" content=\"1\"/>";
		assertTrue(configTool.getBooleanValue(null, headContent1, "enabled", false));

		String headContent0 = "<meta name=\"disabled\" content=\"0\"/>";
		assertFalse(configTool.getBooleanValue(null, headContent0, "disabled", true));
	}

	@Test
	void testGetBooleanValueCaseInsensitive() {
		String headContent = "<meta name=\"test\" content=\"TRUE\"/>";

		assertTrue(configTool.getBooleanValue(null, headContent, "test", false));
	}

	@Test
	void testGetBooleanValueInvalid() {
		String headContent = "<meta name=\"test\" content=\"invalid\"/>";

		// Should return default for invalid boolean value
		assertTrue(configTool.getBooleanValue(null, headContent, "test", true));
		assertFalse(configTool.getBooleanValue(null, headContent, "test", false));
	}

	@Test
	void testGetBooleanValueFromSite() {
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("showToc", "true");

		assertTrue(configTool.getBooleanValue(site, null, "showToc", false));
	}

	@Test
	void testGetBooleanValueWithDefault() {
		// Should return default when not found
		assertTrue(configTool.getBooleanValue(null, null, "nonExistent", true));
		assertFalse(configTool.getBooleanValue(null, null, "nonExistent", false));
	}

	@Test
	void testGetIntValue() {
		String headContent = "<meta name=\"tocMaxDepth\" content=\"5\"/>";

		assertEquals(5, configTool.getIntValue(null, headContent, "tocMaxDepth", 3));
	}

	@Test
	void testGetIntValueFromSite() {
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("maxWidth", "1024");

		assertEquals(1024, configTool.getIntValue(site, null, "maxWidth", 800));
	}

	@Test
	void testGetIntValueInvalid() {
		String headContent = "<meta name=\"test\" content=\"notAnInteger\"/>";

		// Should return default for invalid integer value
		assertEquals(42, configTool.getIntValue(null, headContent, "test", 42));
	}

	@Test
	void testGetIntValueWithDefault() {
		// Should return default when not found
		assertEquals(100, configTool.getIntValue(null, null, "nonExistent", 100));
	}

	@Test
	void testGetIntValueWithWhitespace() {
		String headContent = "<meta name=\"test\" content=\" 123 \"/>";

		assertEquals(123, configTool.getIntValue(null, headContent, "test", 0));
	}

	@Test
	void testPrecedenceOrder() {
		// Test that front matter takes precedence over site.xml
		String headContent = "<meta name=\"interpolation\" content=\"frontMatter\"/>";
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("interpolation", "siteValue");

		String result = configTool.getValue(site, headContent, "interpolation", "default");
		assertEquals("frontMatter", result);
	}

	@Test
	void testSitePrecedenceOverDefault() {
		// Test that site.xml takes precedence over default
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("test", "siteValue");

		String result = configTool.getValue(site, null, "test", "default");
		assertEquals("siteValue", result);
	}

	@Test
	void testMetaTagWithoutContent() {
		String headContent = "<meta name=\"test\"/>";

		// Meta tag without content should be ignored
		String result = configTool.getValue(null, headContent, "test", "default");
		assertEquals("default", result);
	}

	@Test
	void testMetaTagWithoutName() {
		String headContent = "<meta content=\"value\"/>";

		// Meta tag without name should be ignored
		String result = configTool.getValue(null, headContent, "test", "default");
		assertEquals("default", result);
	}

	@Test
	void testComplexHeadContent() {
		String headContent = "<title>Test Page</title>" +
				"<meta charset=\"UTF-8\"/>" +
				"<meta name=\"interpolation\" content=\"none\"/>" +
				"<meta name=\"description\" content=\"Test page description\"/>" +
				"<link rel=\"stylesheet\" href=\"style.css\"/>" +
				"<meta name=\"showToc\" content=\"true\"/>";

		String result1 = configTool.getValue(null, headContent, "interpolation", "default");
		assertEquals("none", result1);

		boolean result2 = configTool.getBooleanValue(null, headContent, "showToc", false);
		assertTrue(result2);
	}

	@Test
	void testCaching() {
		// Test that parsing result is cached
		String headContent = "<meta name=\"test\" content=\"value\"/>";

		// First call should parse
		String result1 = configTool.getValue(null, headContent, "test", "default");
		assertEquals("value", result1);

		// Second call with same headContent should use cache
		String result2 = configTool.getValue(null, headContent, "test", "default");
		assertEquals("value", result2);

		// Call with different key but same headContent should also use cache
		String result3 = configTool.getValue(null, headContent, "other", "default");
		assertEquals("default", result3);
	}

	@Test
	void testNullSiteModelHandling() {
		String headContent = "<meta name=\"test\" content=\"value\"/>";

		// Should handle null site model gracefully
		String result = configTool.getValue(null, headContent, "test", "default");
		assertEquals("value", result);
	}

	@Test
	void testMalformedHtml() {
		String headContent = "<meta name=\"test\" content=\"value\">" + // Missing closing tag
				"<div><meta name=\"other\" content=\"otherValue\"></div>";

		// Should still parse despite malformed HTML (Jsoup is lenient)
		String result = configTool.getValue(null, headContent, "test", "default");
		assertEquals("value", result);
	}

	@Test
	void testEmptyMetaContent() {
		String headContent = "<meta name=\"test\" content=\"\"/>";

		// Empty content should be ignored
		String result = configTool.getValue(null, headContent, "test", "default");
		assertEquals("default", result);
	}

	@Test
	void testSiteModelWithNullCustomValue() {
		MockSiteModel site = new MockSiteModel();
		// Don't set any custom value

		String result = configTool.getValue(site, null, "test", "default");
		assertEquals("default", result);
	}

	@Test
	void testIntegrationScenario() {
		// Simulate a real-world scenario
		MockSiteModel site = new MockSiteModel();
		site.setCustomValue("interpolation", "maven");
		site.setCustomValue("showToc", "true");
		site.setCustomValue("tocMaxDepth", "3");

		String headContent = "<meta name=\"interpolation\" content=\"none\"/>" +
				"<meta name=\"showToc\" content=\"false\"/>";

		// interpolation overridden by front matter
		assertEquals("none", configTool.getValue(site, headContent, "interpolation", "default"));

		// showToc overridden by front matter
		assertFalse(configTool.getBooleanValue(site, headContent, "showToc", true));

		// tocMaxDepth from site.xml (not in front matter)
		assertEquals(3, configTool.getIntValue(site, headContent, "tocMaxDepth", 5));

		// nonExistent uses default
		assertEquals("default", configTool.getValue(site, headContent, "nonExistent", "default"));
	}
}
