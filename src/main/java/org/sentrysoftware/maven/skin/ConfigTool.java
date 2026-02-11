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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	 * Logger for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(ConfigTool.class.getName());

	/**
	 * Cache for parsed head content to avoid re-parsing the same content multiple times.
	 * Uses a WeakHashMap so entries can be garbage collected when headContent strings are no longer referenced.
	 * Wrapped with synchronizedMap for thread safety since ConfigTool is in application scope.
	 * Note: Failed parses are not cached to allow retry on subsequent calls.
	 */
	private final Map<String, Map<String, String>> metaCache = Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * Cache for resolved getCustomValue Method per siteModel class.
	 * Uses Optional to distinguish between "not yet looked up" (absent from map) and
	 * "looked up but not found" (Optional.empty() in map).
	 * ConcurrentHashMap for thread safety.
	 */
	private final Map<Class<?>, Optional<Method>> methodCache = new ConcurrentHashMap<>();

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
		Map<String, String> metaMap = metaCache.get(headContent);
		if (metaMap != null) {
			return metaMap.get(key);
		}

		// Parse and cache only on success
		metaMap = parseMetaTags(headContent);
		if (metaMap != null) {
			metaCache.put(headContent, metaMap);
			return metaMap.get(key);
		}

		return null;
	}

	/**
	 * Parse all meta tags from head content into a map.
	 *
	 * @param headContent The HTML head content
	 * @return Map of meta name to content value, or null if parsing fails
	 */
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
			LOGGER
					.log(
							Level.WARNING,
							"Failed to parse head content for meta tags, will retry on next access",
							e);
			return null;
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
	private String getSiteCustomValue(final Object siteModel, final String key) {

		if (siteModel == null) {
			return null;
		}

		Class<?> siteModelClass = siteModel.getClass();

		// Get cached method lookup result, or perform lookup
		Optional<Method> cachedMethod = methodCache.computeIfAbsent(siteModelClass, clazz -> {
			try {
				// Try to find getCustomValue(String) method
				// This works with both Maven Site Plugin 3.x (DecorationModel) and 4.x (SiteModel)
				return Optional.of(clazz.getMethod("getCustomValue", String.class));
			} catch (NoSuchMethodException e) {
				LOGGER
						.log(
								Level.FINE,
								String
										.format(
												"Site model class %s does not have getCustomValue(String) method, "
														+ "configuration will fall back to defaults",
												clazz.getName()));
				return Optional.empty();
			}
		});

		// If method was not found, return null
		if (cachedMethod.isEmpty()) {
			return null;
		}

		// Invoke the cached method
		try {
			Object result = cachedMethod.get().invoke(siteModel, key);
			if (result instanceof String) {
				return (String) result;
			}
		} catch (ReflectiveOperationException e) {
			LOGGER
					.log(
							Level.WARNING,
							String
									.format(
											"Failed to invoke getCustomValue('%s') on site model of type %s",
											key,
											siteModelClass.getName()),
							e);
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

	/**
	 * Returns the current size of the meta cache.
	 * This method is package-private for testing purposes only.
	 *
	 * @return The number of entries in the meta cache
	 */
	int getCacheSize() {
		return metaCache.size();
	}
}
