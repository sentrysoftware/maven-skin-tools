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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class with various static helper functions.
 */
public final class Helper {

	private Helper() {
		/* Do nothing */
	}

	/**
	 * Write a text file using System.lineSeparator()
	 *
	 * @param file The file instance to write to
	 * @param text The text to write
	 * @param charset The charset to use when writing text
	 * @throws IOException when anything goes wrong
	 */
	public static void writeText(final File file, final String text, final Charset charset) throws IOException {

		// Write the result using system line separator
		try (
				BufferedReader reader = new BufferedReader(new StringReader(text));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
		) {
			String line;
			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	/**
	 * Returns the time of last modification of specified Path in milliseconds since
	 * EPOCH.
	 *
	 * @param path
	 *            Path to the file
	 * @return Milliseconds since EPOCH, or 0 (zero) if file does not exist
	 */
	public static long getLastModifiedTime(final Path path) {

		try {
			return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * Returns the time of last modification of specified File in milliseconds since
	 * EPOCH.
	 *
	 * @param file
	 *            File instance
	 * @return Milliseconds since EPOCH, or 0 (zero) if file does not exist
	 */
	public static long getLastModifiedTime(final File file) {
		return getLastModifiedTime(Paths.get(file.toURI()));
	}

	/**
	 * Returns the time of last modification of specified file in milliseconds since
	 * EPOCH.
	 *
	 * @param filePathString
	 *            Path to the file (as a String)
	 * @return Milliseconds since EPOCH, or 0 (zero) if file does not exist
	 */
	public static long getLastModifiedTime(final String filePathString) {
		return getLastModifiedTime(Paths.get(filePathString));
	}

		/**
		 * Reads the content of a resource file and returns it as a String.
		 *
		 * @param resourcePath The path to the resource file
		 * @return The content of the resource file as a String
		 * @throws IOException If an I/O error occurs
		 */
		public static String readResourceAsString(final String resourcePath) throws IOException {
				try (InputStream inputStream = Helper.class.getResourceAsStream(resourcePath)) {
						if (inputStream == null) {
								throw new IOException("Resource not found: " + resourcePath);
						}
						byte[] bytes = inputStream.readAllBytes();
						return new String(bytes, StandardCharsets.UTF_8);
				}
		}

}
