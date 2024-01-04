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
/**
 * addDocumentToElasticLunr
 **/
(function(indexJson, id, title, keywords, body) {

	var idx;

	// New or existing index?
	if (indexJson == null || indexJson == "") {

		// Create a new index
		idx = elasticlunr(function () {
		    this.addField("title");
		    this.addField("body");
		    this.addField("keywords");
		    this.setRef("id");
		    this.saveDocument(true);
		});

	} else {

		// Load the specified index
		indexJsonObj = JSON.parse(indexJson);
		idx = elasticlunr.Index.load(indexJsonObj);
	}

	// Add the specified document
	idx.updateDoc({
		id: id,
		title: title,
		keywords: keywords,
		body: body
	});

	// Return the JSON-serialized index
	return JSON.stringify(idx);

})
