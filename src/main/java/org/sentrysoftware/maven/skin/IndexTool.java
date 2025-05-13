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
 * indexTool is reference-able in a Velocity template.
 * It creates the elasticlunr index of the specified HTML content.
 */
@DefaultKey("indexTool")
public class IndexTool extends SafeConfig {

	/**
	 * UTF-8 Charset
	 */
	private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

	/**
	 * JS add-document function loaded from build-index.js
	 */
	private static volatile Value addDocumentFunction = null;

	/**
	 * Initialization error (if any)
	 */
	private static volatile Throwable lastError = null;

	/**
	 * Creates a new instance
	 */
	public IndexTool() {
		if (lastError != null) {
			getLog().error("IndexTool: Could not load the indexing Javascript code", lastError);
		}
	}

	/**
	 * Builds and updates the elasticlunr.js index.
	 *
	 * @param indexPathString Path to the JSON-serialized elasticlunr.js index
	 * @param id ID of the document (typically its URL)
	 * @param title Title of the document
	 * @param keywords Keywords of the document
	 * @param body Content of the document
	 * @throws IOException if the index cannot be read or written
	 * @throws ScriptException if the JS engine fails
	 * @throws NoSuchMethodException if JS is malformed
	 */
	public void buildElasticLunrIndex(
		final String indexPathString,
		final String id,
		final String title,
		final String keywords,
		final String body
	) throws IOException, ScriptException, NoSuchMethodException {

		initGraalIfNeeded();

		if (addDocumentFunction == null) {
			getLog().debug("IndexTool: Will not index anything as elasticlunr.js couldn't be loaded");
			return;
		}

		synchronized (addDocumentFunction) {
			// Load existing index if any
			String indexJson;
			Path indexPath = Paths.get(indexPathString);
			if (Files.exists(indexPath)) {
				indexJson = Files.readString(indexPath, UTF8_CHARSET);
			} else {
				indexJson = "";
			}

			// Execute JS indexing
			getLog().debug("IndexTool: Adding {} to the index in {}", id, indexPathString);
			String result = addDocumentFunction.execute(indexJson, id, title, keywords, body).asString();

			// Write result back to file
			try {
				Files.writeString(indexPath, result, UTF8_CHARSET);
			} catch (IOException e) {
				getLog().warn("IndexTool: Couldn't write index to " + indexPath + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
			}
		}
	}

	/**
	 * Lazy-loads the GraalVM JS context and function
	 */
	private static synchronized void initGraalIfNeeded() {
		if (addDocumentFunction != null || lastError != null) {
			return;
		}
		try {
			Context graalContext = Context.newBuilder("js")
				.allowAllAccess(true)
				.option("engine.WarnInterpreterOnly", "false")
				.build();

			graalContext.eval("js", Helper.readResourceAsString("/elasticlunr.min.js"));
			addDocumentFunction = graalContext.eval("js", Helper.readResourceAsString("/build-index.js"));

		} catch (IOException | PolyglotException e) {
			lastError = e;
			addDocumentFunction = null;
		}
	}
}
