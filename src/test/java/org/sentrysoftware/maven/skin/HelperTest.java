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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class HelperTest {

	@Test
	void testTestLastModifiedTime() throws IOException {

		File testFile = File.createTempFile("temp", ".test");
		testFile.deleteOnExit();
		long timestamp = System.currentTimeMillis();
		Helper.writeText(testFile, "test", Charset.defaultCharset());

		assertEquals(
				Helper.getLastModifiedTime(testFile),
				Helper.getLastModifiedTime(testFile.getAbsolutePath()),
				"Value obtained from String or File must be identical");

		assertEquals(
				Helper.getLastModifiedTime(testFile),
				Helper.getLastModifiedTime(Paths.get(testFile.toURI())),
				"Value obtained from Path or File must be identical");

		long timeDiff = Helper.getLastModifiedTime(testFile) - timestamp;
		assertTrue(
				timeDiff >= -1000,
				"File should be marked as modified after timestamp (timeDiff " + timeDiff + " should be > 0 ms)");

	}

}
