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
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.generic.SafeConfig;
import org.apache.velocity.tools.generic.ValueParser;
import org.codehaus.plexus.interpolation.InterpolatorFilterReader;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * An Apache Velocity tool that provides Maven property interpolation for content.
 * <p>
 * This tool leverages Maven's interpolation mechanism to replace ${...} expressions
 * in content with their corresponding property values. It supports all standard Maven
 * properties including:
 * </p>
 * <ul>
 * <li>Project properties (e.g., ${project.version}, ${project.name})</li>
 * <li>Custom properties from pom.xml &lt;properties&gt; section</li>
 * <li>System properties (e.g., ${java.version})</li>
 * <li>Environment variables (e.g., ${env.PATH})</li>
 * </ul>
 * <p>
 * Undefined properties are left as-is in the content. Use backslash escaping
 * (\${...}) to output literal ${...} expressions.
 * </p>
 *
 * @author Bertrand Martin
 * @since 1.6
 */
@DefaultKey("mavenFilteringTool")
public class MavenFilteringTool extends SafeConfig {

	private MavenProject mavenProject;
	private MavenSession mavenSession;
	private Properties additionalProperties;

	/**
	 * Create a new instance
	 */
	public MavenFilteringTool() {
		/* Do nothing */
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see SafeConfig#configure(ValueParser)
	 */
	@Override
	protected void configure(final ValueParser values) {

		// Retrieve the Velocity context
		Object velocityContext = values.get("velocityContext");

		if (!(velocityContext instanceof ToolContext)) {
			return;
		}

		ToolContext context = (ToolContext) velocityContext;

		// Get the Maven project object from context
		Object projectObj = context.get("project");
		if (projectObj instanceof MavenProject) {
			mavenProject = (MavenProject) projectObj;
		}

		// Get the Maven session from context (if available)
		Object sessionObj = context.get("session");
		if (sessionObj instanceof MavenSession) {
			mavenSession = (MavenSession) sessionObj;
		}

		// Get additional properties from context
		additionalProperties = new Properties();
		Object propertiesObj = context.get("properties");
		if (propertiesObj instanceof java.util.Map) {
			@SuppressWarnings("unchecked")
			java.util.Map<String, String> propsMap = (java.util.Map<String, String>) propertiesObj;
			additionalProperties.putAll(propsMap);
		}
	}

	/**
	 * Creates a Maven interpolator with all the standard value sources.
	 * <p>
	 * This method sets up an interpolator with the following value sources in order:
	 * </p>
	 * <ol>
	 * <li>Project properties</li>
	 * <li>Maven project object properties</li>
	 * <li>System properties</li>
	 * <li>Environment variables</li>
	 * </ol>
	 *
	 * @return A configured RegexBasedInterpolator
	 */
	private RegexBasedInterpolator createInterpolator() {
		RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

		List<ValueSource> valueSources = new ArrayList<>();

		// Add project properties
		if (additionalProperties != null && !additionalProperties.isEmpty()) {
			valueSources.add(new PropertiesBasedValueSource(additionalProperties));
		}

		// Add Maven project properties
		if (mavenProject != null) {
			valueSources.add(new PropertiesBasedValueSource(mavenProject.getProperties()));
			valueSources
					.add(
							new org.codehaus.plexus.interpolation.PrefixedObjectValueSource(
									java.util.Collections.singletonList("project"),
									mavenProject,
									true));
			valueSources
					.add(
							new org.codehaus.plexus.interpolation.PrefixedObjectValueSource(
									java.util.Collections.singletonList("pom"),
									mavenProject,
									true));
		}

		// Add system properties
		valueSources.add(new PropertiesBasedValueSource(System.getProperties()));

		// Add environment variables with env. prefix
		Properties envProperties = new Properties();
		for (java.util.Map.Entry<String, String> entry : System.getenv().entrySet()) {
			envProperties.setProperty("env." + entry.getKey(), entry.getValue());
		}
		valueSources.add(new PropertiesBasedValueSource(envProperties));

		// Add all value sources to the interpolator
		for (ValueSource valueSource : valueSources) {
			interpolator.addValueSource(valueSource);
		}

		return interpolator;
	}

	/**
	 * Filters the given content by interpolating Maven properties.
	 * <p>
	 * This method replaces ${...} expressions in the content with their corresponding
	 * property values using Maven's standard filtering mechanism. The following property
	 * sources are searched in order:
	 * </p>
	 * <ol>
	 * <li>Additional properties (custom properties from context)</li>
	 * <li>Project properties (from pom.xml &lt;properties&gt; section)</li>
	 * <li>Project object properties (e.g., ${project.version})</li>
	 * <li>System properties (e.g., ${java.version})</li>
	 * <li>Environment variables (e.g., ${env.PATH})</li>
	 * </ol>
	 * <p>
	 * If a property is not found, the expression is left unchanged in the content.
	 * Expressions can be escaped using a backslash: \${...} will output ${...}.
	 * </p>
	 *
	 * @param content
	 *        The content to filter. Can be null or empty.
	 * @return The filtered content with properties interpolated. Returns the original
	 *         content if it's null, empty, or if interpolation fails.
	 * @since 1.6
	 */
	public String filter(final String content) {

		// Handle null or empty content
		if (content == null || content.isEmpty()) {
			return content;
		}

		try {
			// Create interpolator with Maven's standard value sources
			RegexBasedInterpolator interpolator = createInterpolator();

			// Create a reader wrapping the content with the interpolator
			java.io.StringReader reader = new java.io.StringReader(content);
			InterpolatorFilterReader filterReader = new InterpolatorFilterReader(
					reader,
					interpolator,
					"${",
					"}");

			// Read the filtered content
			StringBuilder result = new StringBuilder();
			char[] buffer = new char[8192];
			int length;
			while ((length = filterReader.read(buffer)) != -1) {
				result.append(buffer, 0, length);
			}
			filterReader.close();

			return result.toString();

		} catch (java.io.IOException e) {
			// If interpolation fails, return the original content
			// This ensures we don't break content even if there's an error
			return content;
		}
	}
}
