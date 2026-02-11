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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.generic.SafeConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * An Apache Velocity tool that provides unified configuration management for Maven sites,
 * merging site-wide configuration from site.xml with per-page overrides from Markdown front matter.
 * <p>
 * Configuration precedence (highest to lowest):
 * <ol>
 * <li>Front matter (converted to {@code <meta>} tags in headContent)</li>
 * <li>Site-wide configuration ({@code <custom>} element in site.xml)</li>
 * <li>Default value provided by the caller</li>
 * </ol>
 * <p>
 * Front matter in Markdown files:
 *
 * <pre>{@code
 * ---
 * interpolation: none
 * showToc: false
 * tocMaxDepth: 3
 * ---
 * }</pre>
 *
 * is converted by Doxia to {@code <meta>} tags:
 *
 * <pre>{@code
 * <meta name="interpolation" content="none"/>
 * <meta name="showToc" content="false"/>
 * <meta name="tocMaxDepth" content="3"/>
 * }</pre>
 *
 * @author Bertrand Martin
 * @since 1.6
 */
@DefaultKey("configTool")
public class ConfigTool extends SafeConfig {

	/**
	 * Cache for parsed head content to avoid re-parsing the same content multiple times.
	 * Uses weak references to allow garbage collection when memory is needed.
	 */
	private final Map<Integer, WeakReference<Map<String, String>>> metaCache = new HashMap<>();

	/**
	 * Creates a new instance of ConfigTool.
	 */
	public ConfigTool() {
		/* Do nothing */
	}

	/**
	 * Get a configuration value with front matter overriding site.xml settings.
	 * <p>
	 * Usage in Velocity:
	 *
	 * <pre>{@code
	 * #set($interpolation = $configTool.getValue($site, $headContent, "interpolation", "maven"))
	 * }</pre>
	 *
	 * @param siteModel The site decoration model ($site or $decoration) - can be null
	 * @param headContent The HTML head content string containing meta tags ($headContent) - can be null
	 * @param key The configuration key (e.g., "interpolation", "showToc")
	 * @param defaultValue The default value if not found in either source
	 * @return The configuration value (front matter &gt; site.xml &gt; default)
	 * @since 1.6
	 */
	public String getValue(
			final Object siteModel,
			final String headContent,
			final String key,
			final String defaultValue) {

		// Check front matter (highest priority)
		String frontMatterValue = getMetaValue(headContent, key);
		if (frontMatterValue != null) {
			return frontMatterValue;
		}

		// Check site.xml custom values
		String siteValue = getSiteCustomValue(siteModel, key);
		if (siteValue != null) {
			return siteValue;
		}

		// Return default
		return defaultValue;
	}

	/**
	 * Get a boolean configuration value with proper string-to-boolean conversion.
	 * <p>
	 * Recognizes "true", "yes", "1" as true (case-insensitive).
	 * Recognizes "false", "no", "0" as false (case-insensitive).
	 * Any other value returns the default.
	 * </p>
	 * <p>
	 * Usage in Velocity:
	 *
	 * <pre>{@code
	 * #set($showToc = $configTool.getBooleanValue($site, $headContent, "showToc", true))
	 * }</pre>
	 *
	 * @param siteModel The site decoration model ($site or $decoration) - can be null
	 * @param headContent The HTML head content string containing meta tags ($headContent) - can be null
	 * @param key The configuration key (e.g., "showToc", "checkImages")
	 * @param defaultValue The default value if not found or invalid
	 * @return The boolean configuration value (front matter &gt; site.xml &gt; default)
	 * @since 1.6
	 */
	public boolean getBooleanValue(
			final Object siteModel,
			final String headContent,
			final String key,
			final boolean defaultValue) {

		String value = getValue(siteModel, headContent, key, null);
		if (value == null) {
			return defaultValue;
		}

		return parseBoolean(value, defaultValue);
	}

	/**
	 * Get an integer configuration value.
	 * <p>
	 * If the value cannot be parsed as an integer, returns the default value.
	 * </p>
	 * <p>
	 * Usage in Velocity:
	 *
	 * <pre>{@code
	 * #set($tocMaxDepth = $configTool.getIntValue($site, $headContent, "tocMaxDepth", 3))
	 * }</pre>
	 *
	 * @param siteModel The site decoration model ($site or $decoration) - can be null
	 * @param headContent The HTML head content string containing meta tags ($headContent) - can be null
	 * @param key The configuration key (e.g., "tocMaxDepth", "maxImageWidth")
	 * @param defaultValue The default value if not found or invalid
	 * @return The integer configuration value (front matter &gt; site.xml &gt; default)
	 * @since 1.6
	 */
	public int getIntValue(
			final Object siteModel,
			final String headContent,
			final String key,
			final int defaultValue) {

		String value = getValue(siteModel, headContent, key, null);
		if (value == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Extract a meta tag value from the head content HTML.
	 *
	 * @param headContent The HTML head content containing meta tags
	 * @param key The meta tag name to look for
	 * @return The content attribute value, or null if not found
	 */
	private String getMetaValue(final String headContent, final String key) {

		if (headContent == null || headContent.isEmpty()) {
			return null;
		}

		// Check cache first
		int cacheKey = Objects.hash(headContent);
		Map<String, String> metaMap = getCachedMetaMap(cacheKey);

		if (metaMap == null) {
			// Parse and cache
			metaMap = parseMetaTags(headContent);
			metaCache.put(cacheKey, new WeakReference<>(metaMap));
		}

		return metaMap.get(key);
	}

	/**
	 * Get cached meta map from weak reference.
	 *
	 * @param cacheKey The cache key
	 * @return The cached map or null if not in cache or garbage collected
	 */
	private Map<String, String> getCachedMetaMap(final int cacheKey) {
		WeakReference<Map<String, String>> ref = metaCache.get(cacheKey);
		if (ref != null) {
			Map<String, String> map = ref.get();
			if (map != null) {
				return map;
			}
			// Reference was cleared, remove stale entry
			metaCache.remove(cacheKey);
		}
		return null;
	}

	/**
	 * Parse all meta tags from head content into a map.
	 *
	 * @param headContent The HTML head content
	 * @return Map of meta name to content value
	 */
	@SuppressWarnings("PMD.EmptyCatchBlock")
	private Map<String, String> parseMetaTags(final String headContent) {

		Map<String, String> metaMap = new HashMap<>();

		try {
			Document doc = Jsoup.parseBodyFragment(headContent);
			Elements metaTags = doc.select("meta[name][content]");

			for (Element meta : metaTags) {
				String name = meta.attr("name");
				String content = meta.attr("content");
				if (!name.isEmpty() && !content.isEmpty()) {
					metaMap.put(name, content);
				}
			}
		} catch (Exception e) {
			// Return empty map on parsing error
		}

		return metaMap;
	}

	/**
	 * Get a custom value from the site decoration model.
	 *
	 * @param siteModel The site model object (DecorationModel or SiteModel)
	 * @param key The custom value key
	 * @return The custom value, or null if not found or error
	 */
	@SuppressWarnings("PMD.EmptyCatchBlock")
	private String getSiteCustomValue(final Object siteModel, final String key) {

		if (siteModel == null) {
			return null;
		}

		try {
			// Try to call getCustomValue(String) using reflection
			// This works with both Maven Site Plugin 3.x (DecorationModel) and 4.x (SiteModel)
			java.lang.reflect.Method method = siteModel.getClass().getMethod("getCustomValue", String.class);
			Object result = method.invoke(siteModel, key);
			if (result instanceof String) {
				return (String) result;
			}
		} catch (ReflectiveOperationException e) {
			// Method not found or invocation failed - return null
		}

		return null;
	}

	/**
	 * Parse a string value as boolean with support for common truthy/falsy values.
	 *
	 * @param value The string value to parse
	 * @param defaultValue The default value if parsing fails
	 * @return true if value is "true", "yes", or "1" (case-insensitive), false if "false", "no", or "0",
	 *         otherwise defaultValue
	 */
	private boolean parseBoolean(final String value, final boolean defaultValue) {

		if (value == null) {
			return defaultValue;
		}

		String normalized = value.trim().toLowerCase();

		switch (normalized) {
		case "true":
		case "yes":
		case "1":
			return true;
		case "false":
		case "no":
		case "0":
			return false;
		default:
			return defaultValue;
		}
	}
}
