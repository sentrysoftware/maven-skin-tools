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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.script.ScriptException;

import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.generic.SafeConfig;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

/**
 * indexTool is a reference-able in a Velocity template.
 * <p>
 * It creates the elasticlunr index of the specified HTML content.
 * </p>
 */
@DefaultKey("indexTool")
public class IndexTool extends SafeConfig {

	/**
	 * UTF-8 Charset
	 */
	static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Static initialization exception
	 */
	private static Throwable lastError;

	/**
	 * Javascript function that adds a document to the specified elasticlunr index.
	 * This function is referenced to through GraalVM Javascript engine
	 */
	private static final Value ADD_DOCUMENT_FUNCTION;
	static {
		// Build a Graal context for Javascript (with no warnings!)
		final Context graalContext = Context
				.newBuilder("js")
				.allowAllAccess(true)
				.option("engine.WarnInterpreterOnly", "false")
				.build();

		Value tempFunction;
		try {
			// Load elasticlunr (http://elasticlunr.com/)
			graalContext.eval("js", Helper.readResourceAsString("/elasticlunr.min.js"));

			// Load our own JS script and retrieve the pointer to our indexing function
			tempFunction = graalContext.eval("js", Helper.readResourceAsString("/build-index.js"));

		} catch (IOException | PolyglotException e) {
			/* Can't do much about it here */
			tempFunction = null;
			lastError = e;
		}
		ADD_DOCUMENT_FUNCTION = tempFunction;

	}

	/**
	 * Creates a new instance
	 */
	public IndexTool() {
		if (lastError != null) {
			getLog().error("IndexTool: Could not load the indexing Javascript code", lastError);
		}
	}

	/**
	 * Builds and update the specified elasticlunr.js index.
	 * <p>
	 * If the specified elasticlunr.js index file doesn't exist, it will be created.
	 * Otherwise, it will be updated with the specified document.
	 * </p>
	 * <p>
	 * The elasticlunr.js index file is the JSON-serialized index that needs to be loaded
	 * into elasticlunr.js with:
	 * </p>
	 * <p>
	 * {@code elasticlunr.Index.load(indexJson);}
	 * </p>
	 * <p>
	 * This uses http://elasticlunrjs.com version 0.9.5.
	 * </p>
	 *
	 * @param indexPathString Path to the JSON-serialized elasticlunr.js index
	 * @param id ID of the document to add/update (typically it's URL)
	 * @param title Title of the document
	 * @param keywords Keywords of the document (separated with any non alphabetical characters)
	 * @param body Content of the document to be added to the index
	 * @throws IOException when cannot read or write the index file
	 * @throws ScriptException when anything bad happens with the Javascript (should never happen except when developing)
	 * @throws NoSuchMethodException when developer broke the Javascript code
	 */
	public void buildElasticLunrIndex(
			final String indexPathString,
			final String id,
			final String title,
			final String keywords,
			final String body)
			throws IOException,
			ScriptException,
			NoSuchMethodException {

		if (ADD_DOCUMENT_FUNCTION == null) {
			getLog().debug("IndexTool: Will not index anything as elasticlunr.js couldn't be loaded");
			return;
		}

		// Make sure the index is updated only once at a time
		synchronized (ADD_DOCUMENT_FUNCTION) {

			// Read the index file, if any
			String indexJson;
			Path indexPath = Paths.get(indexPathString);
			if (indexPath.toFile().exists()) {
				indexJson = new String(Files.readAllBytes(indexPath), UTF8_CHARSET);
			} else {
				indexJson = "";
			}

			// Call our Javascript function
			getLog().debug("IndexTool: Adding {} to the index in {}", id, indexPathString);
			String result = ADD_DOCUMENT_FUNCTION.execute(indexJson, id, title, keywords, body).asString();

			// Write the result
			try {
				Files.write(indexPath, result.getBytes(UTF8_CHARSET));
			} catch (IOException e) {
				getLog()
						.warn(
								"IndexTool: Couldn't write index to " + indexPath.toString() + " (" + e.getClass().getSimpleName()
										+ ": " + e.getMessage() + ")");
			}

		}
	}

}
