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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexToolTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testBuildElasticLunrIndex() throws Exception {

		// The index will be stored in a temporary file
		Path indexPath = Files.createTempFile("test-indexing", ".json");

		// Get a page content
		String studioAgentBody = HtmlToolTest.getResourceAsString("/studio-agent.html");
		HtmlTool htmlTool = new HtmlTool();
		String studioAgentText = htmlTool.text(htmlTool.parseContent(studioAgentBody), "body").get(0);

		// Get a new IndexTool
		IndexTool indexTool = new IndexTool();
		
		// Add it to the index, twice! (it's supposed to be updated properly and the page present only once)
		indexTool.buildElasticLunrIndex(indexPath.toString(), "agent.html", "Agent", "testagent", studioAgentText + " dragon");
		indexTool.buildElasticLunrIndex(indexPath.toString(), "agent.html", "Agent", "testagent", studioAgentText);

		// And now add another fake entry
		indexTool.buildElasticLunrIndex(indexPath.toString(), "fake.html", "Fake", "fake", "This is a fake test.");

		String indexContent = new String(Files.readAllBytes(indexPath), "UTF-8");

		assertTrue(indexContent.contains("{\"id\":\"agent.html\",\"title\":\"Agent\",\"keywords\":\"testagent\""));
		assertTrue(indexContent.contains("{\"id\":\"fake.html\",\"title\":\"Fake\",\"keywords\":\"fake\""));

		assertFalse(indexContent.contains("dragon"), "Existing entries must be overwritten with new ones");

		// Delete the temporary file
		Files.delete(indexPath);
	}


}
