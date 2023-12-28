package org.sentrysoftware.maven.skin;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Sentry Maven Skin Tools
 * ჻჻჻჻჻჻
 * Copyright 2017 - 2023 Sentry Software
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
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.velocity.tools.config.DefaultKey;

/**
 * indexTool is a reference-able in a Velocity template.
 * <p>
 * It creates the elasticlunr index of the specified HTML content.
 * </p>
 */
@DefaultKey("indexTool")
public class IndexTool {

	/**
	 * Creates a new instance
	 */
	public IndexTool() {
		/* Do nothing */
	}
	
	/**
	 * UTF-8 Charset
	 */
	static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

	/**
	 * GraalVM's graal.js engine to execute Javascript
	 */
	static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");

	static {

		try {
			// Load elasticlunr (http://elasticlunr.com/)
			engine.eval(new InputStreamReader(IndexTool.class.getResourceAsStream("/elasticlunr.min.js")));

			// Load our own JS script
			engine.eval(new InputStreamReader(IndexTool.class.getResourceAsStream("/build-index.js")));

		} catch (ScriptException e) {
			// What can we do in a static statement to handle exceptions? Not much...
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
	public static synchronized void buildElasticLunrIndex(String indexPathString, String id, String title, String keywords, String body) throws IOException, ScriptException, NoSuchMethodException {

		// Read the index file, if any
		String indexJson;
		Path indexPath = Paths.get(indexPathString);
		if (indexPath.toFile().exists()) {
			indexJson = new String(Files.readAllBytes(indexPath), UTF8_CHARSET);
		} else {
			indexJson = "";
		}

		// Call our Javascript function
		Invocable invocable = (Invocable)engine;
		String result = (String)invocable.invokeFunction("addDocumentToElasticLunr", indexJson, id, title, keywords, body);

		// Write the result
		Files.write(indexPath, result.getBytes(UTF8_CHARSET));

	}

}
