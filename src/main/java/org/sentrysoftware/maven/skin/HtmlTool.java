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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.generic.SafeConfig;
import org.apache.velocity.tools.generic.ValueParser;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * An Apache Velocity tool that provides utility methods to manipulate HTML code using
 * <a href="http://jsoup.org/">jsoup</a> HTML5 parser.
 * <p>
 * The methods utilise <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">CSS
 * selectors</a> to refer to specific elements for manipulation.
 * </p>
 *
 * @author Bertrand Martin (originally inspired by Andrius Velykis)
 * @since 1.0
 * @see <a href="http://jsoup.org/">jsoup HTML parser</a>
 * @see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">jsoup CSS selectors</a>
 */
@DefaultKey("htmlTool")
public class HtmlTool extends SafeConfig {

	/** A list of all HTML heading classes (h1-6) */
	private static final List<String> HEADINGS = Collections.unmodifiableList(
			Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6"));


	private String outputEncoding = "UTF-8";

	/**
	 * Create a new instance
	 */
	public HtmlTool() {
		/* Do nothing */
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see SafeConfig#configure(ValueParser)
	 */
	@Override
	protected void configure(final ValueParser values) {

		// retrieve the Velocity context for output encoding
		Object velocityContext = values.get("velocityContext");

		if (!(velocityContext instanceof ToolContext)) {
			return;
		}

		ToolContext ctxt = (ToolContext) velocityContext;

		// get the output encoding
		Object outputEncodingObj = ctxt.get("outputEncoding");
		if (outputEncodingObj instanceof String) {
			this.outputEncoding = (String) outputEncodingObj;
		}
	}

	/**
	 * Sets attribute to the given value on elements in HTML.
	 *
	 * @param body
	 *            HTML content to set attributes on
	 * @param selector
	 *            CSS selector for elements to modify
	 * @param attributeKey
	 *            Attribute name
	 * @param value
	 *            Attribute value
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 * @since 1.0
	 */
	public Element setAttr(final Element body, final String selector, final String attributeKey, final String value) {

		List<Element> elements = body.select(selector);

		for (Element element : elements) {
			element.attr(attributeKey, value);
		}

		return body;

	}

	/**
	 * Parses HTML fragment
	 *
	 * @param content HTML fragment to parse
	 * @return Element of the specified HTML fragment
	 */
	public Element parseContent(final String content) {
		Document doc = Jsoup.parseBodyFragment(content);
		doc.outputSettings().charset(outputEncoding);
		return doc.body();
	}

	/**
	 * Retrieves attribute value on elements in HTML. Will return all attribute values for the
	 * selector, since there can be more than one element.
	 *
	 * @param body
	 *            HTML content to read attributes from
	 * @param selector
	 *            CSS selector for elements to find
	 * @param attributeKey
	 *            Attribute name
	 * @return Attribute values for all matching elements. If no elements are found, empty list is
	 *         returned.
	 * @since 1.0
	 */
	public List<String> getAttr(final Element body, final String selector, final String attributeKey) {

		List<Element> elements = body.select(selector);
		List<String> attrs = new ArrayList<String>();

		for (Element element : elements) {
			String attrValue = element.attr(attributeKey);
			attrs.add(attrValue);
		}

		return attrs;
	}

	/**
	 * Adds given class names to the elements in HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements to add classes to
	 * @param classNames
	 *            Names of classes to add to the selected elements
	 * @param amount
	 *            Maximum number of elements to modify
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 * @since 1.0
	 */
	public Element addClass(final Element body, final String selector, final List<String> classNames, final int amount) {

		List<Element> elements = body.select(selector);
		if (amount >= 0) {
			// limit to the indicated amount
			elements = elements.subList(0, Math.min(amount, elements.size()));
		}

		for (Element element : elements) {
			for (String className : classNames) {
				element.addClass(className);
			}
		}

		return body;

	}

	/**
	 * Adds given class names to the elements in HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements to add classes to
	 * @param classNames
	 *            Names of classes to add to the selected elements
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 * @since 1.0
	 */
	public Element addClass(final Element body, final String selector, final List<String> classNames) {
		return addClass(body, selector, classNames, -1);
	}

	/**
	 * Adds given class to the elements in HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements to add the class to
	 * @param className
	 *            Name of class to add to the selected elements
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 * @since 1.0
	 */
	public Element addClass(final Element body, final String selector, final String className) {
		return addClass(body, selector, Collections.singletonList(className));
	}

	/**
	 * Wraps elements in HTML with the given HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements to wrap
	 * @param wrapHtml
	 *            HTML to use for wrapping the selected elements
	 * @param amount
	 *            Maximum number of elements to modify
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 * @since 1.0
	 */
	public Element wrap(final Element body, final String selector, final String wrapHtml, final int amount) {

		List<Element> elements = body.select(selector);
		if (amount >= 0) {
			// limit to the indicated amount
			elements = elements.subList(0, Math.min(amount, elements.size()));
		}

		for (Element element : elements) {
			element.wrap(wrapHtml);
		}

		return body;

	}

	/**
	 * Append HTML elements to specified elements in the given HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements that will get the appendice
	 * @param appendHtml
	 *            HTML to append to the selected elements
	 * @param amount
	 *            Maximum number of elements to modify
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 */
	public Element append(final Element body, final String selector, final String appendHtml, final int amount) {

		List<Element> elements = body.select(selector);
		if (amount >= 0) {
			// limit to the indicated amount
			elements = elements.subList(0, Math.min(amount, elements.size()));
		}

		for (Element element : elements) {
			element.append(appendHtml);
		}

		return body;

	}


	/**
	 * Prepend HTML elements to specified elements in the given HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements that will get the "pre-pendice"
	 * @param prependHtml
	 *            HTML to prepend to the selected elements
	 * @param amount
	 *            Maximum number of elements to modify
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 */
	public Element prepend(final Element body, final String selector, final String prependHtml, final int amount) {

		List<Element> elements = body.select(selector);
		if (amount >= 0) {
			// limit to the indicated amount
			elements = elements.subList(0, Math.min(amount, elements.size()));
		}

		for (Element element : elements) {
			element.prepend(prependHtml);
		}

		return body;

	}


	/**
	 * Removes elements from HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements to remove
	 * @return HTML content with removed elements. If no elements are found, the original content is
	 *         returned.
	 * @since 1.0
	 */
	public Element remove(final Element body, final String selector) {

		List<Element> elements = body.select(selector);

		for (Element element : elements) {
			element.remove();
		}

		return body;

	}

	/**
	 * Replace selected elements with specified elements in the given HTML.
	 *
	 * @param body
	 *            HTML content to modify
	 * @param selector
	 *            CSS selector for elements that will get the appendice
	 * @param replaceHtml
	 *            HTML to append to the selected elements
	 * @param amount
	 *            Maximum number of elements to modify (-1 for unlimited)
	 * @return HTML content with modified elements. If no elements are found, the original content
	 *         is returned.
	 */
	public Element replace(final Element body, final String selector, final String replaceHtml, final int amount) {

		List<Element> elements = body.select(selector);
		if (amount >= 0) {
			// limit to the indicated amount
			elements = elements.subList(0, Math.min(amount, elements.size()));
		}

		for (Element element : elements) {
			element.before(replaceHtml).remove();
		}

		return body;

	}


	/**
	 * Retrieves text content of the selected elements in HTML. Renders the element's text as it
	 * would be displayed on the web page (including its children).
	 *
	 * @param body
	 *            HTML content with the elements
	 * @param selector
	 *            CSS selector for elements to extract contents
	 * @return A list of element texts as rendered to display. Empty list if no elements are found.
	 * @since 1.0
	 */
	public List<String> text(final Element body, final String selector) {

		List<Element> elements = body.select(selector);
		List<String> texts = new ArrayList<String>();

		for (Element element : elements) {
			texts.add(element.text());
		}

		return texts;
	}

	/**
	 * Transforms the given HTML content by moving anchor ({@code <a name="myheading">}) names to
	 * IDs for heading elements.
	 * <p>
	 * The anchors are used to indicate positions within a HTML page. In HTML5, however, the
	 * {@code name} attribute is no longer supported on {@code <a>}) tag. The positions within pages
	 * are indicated using {@code id} attribute instead, e.g. {@code <h1 id="myheading">}.
	 * </p>
	 * <p>
	 * The method finds anchors inside, immediately before or after the heading tags and uses their
	 * name as heading {@code id} instead. The anchors themselves are removed.
	 * </p>
	 *
	 * @param body
	 *            HTML content to modify
	 * @return HTML content with modified elements. Anchor names are used for adjacent headings, and
	 *         anchor tags are removed. If no elements are found, the original content is returned.
	 * @since 1.0
	 */
	public Element headingAnchorToId(final Element body) {

		// selectors for headings without IDs
		List<String> headNoIds = concat(HEADINGS, ":not([id])", true);

		// selector for anchor with name attribute only
		String nameA = "a[name]:not([href])";

		// select all headings that have inner named anchor
		List<Element> headingsInnerA = body.select(StringUtil.join(
				concat(headNoIds, ":has(" + nameA + ")", true), ", "));

		for (Element heading : headingsInnerA) {
			List<Element> anchors = heading.select(nameA);
			// take first
			if (!anchors.isEmpty()) {
				anchorToId(heading, anchors.get(0));
			}
		}

		// select all headings that have a preceding named anchor
		List<Element> headingsPreA = body.select(StringUtil.join(
				concat(headNoIds, nameA + " + ", false), ", "));

		for (Element heading : headingsPreA) {
			Element anchor = heading.previousElementSibling();
			if (anchor != null) {
				anchorToId(heading, anchor);
			}
		}

		// select all headings that are followed by a named anchor
		// no selector available for that, so first select the anchors
		// then retrieve the headings
		List<Element> anchorsPreH = body.select(StringUtil.join(
				concat(headNoIds, " + " + nameA, true), ", "));

		for (Element anchor : anchorsPreH) {
			Element heading = anchor.previousElementSibling();
			if (heading != null) {
				anchorToId(heading, anchor);
			}
		}

		return body;
	}

	/**
	 * Moves anchor name to heading id, if one does not exist. Removes the anchor.
	 *
	 * @param heading
	 * @param anchor
	 */
	private static void anchorToId(final Element heading, final Element anchor) {

		if ("a".equals(anchor.tagName()) && heading.id().isEmpty()) {
			String aName = anchor.attr("name");
			if (!aName.isEmpty()) {
				// set the anchor name as heading ID
				heading.attr("id", aName);

				// remove the anchor
				anchor.remove();
			}
		}
	}


	/**
	 * Utility method to concatenate a String to a list of Strings. The text can be either appended
	 * or prepended.
	 *
	 * @param elements
	 *            list of elements to append/prepend the text to
	 * @param text
	 *            the given text to append/prepend
	 * @param append
	 *            if {@code true}, text will be appended to the elements. If {@code false}, it will
	 *            be prepended
	 * @return list of elements with the text appended/prepended
	 * @since 1.0
	 */
	public static List<String> concat(final List<String> elements, final String text, final boolean append) {
		List<String> concats = new ArrayList<String>();

		for (String element : elements) {
			concats.add(append ? element + text : text + element);
		}

		return concats;
	}


	/**
	 * Transforms the given HTML content by adding IDs to all heading elements ({@code h1-6}) that
	 * do not have one.
	 * <p>
	 * IDs on heading elements are used to indicate positions within a HTML page in HTML5. If a
	 * heading tag without an {@code id} is found, its "slug" is generated automatically based on
	 * the heading contents and used as the ID.
	 * </p>
	 *
	 * @param body
	 *            HTML content to modify
	 * @return HTML content with all heading elements having {@code id} attributes. If all headings
	 *         were with IDs already, the original content is returned.
	 * @since 1.0
	 */
	public Element ensureHeadingIds(final Element body) {

		// Find all existing IDs (to avoid generating duplicates)
		Map<String, Integer> ids = new HashMap<String, Integer>();
		List<Element> idElems = body.select("*[id]");
		for (Element idElem : idElems) {
			ids.put(idElem.id(), 0);
		}

		// select all headings that do not have an ID
		List<Element> headingsNoId = body.select("h1:not([id]), h2:not([id]), h3:not([id]), h4:not([id]), h5:not([id]), h6:not([id])");

		for (Element heading : headingsNoId) {

			// Take the text content of the title
			String headingText = heading.text();

			// Create an ID out of it (trim all unwanted chars)
			String headingSlug = slug(headingText);
			if (headingSlug.length() > 50) {
				headingSlug = headingSlug.substring(0, 50);
			}

			// If the ID already exists, add an increasing number to it
			int slugNumber = ids.merge(headingSlug, 1, (oldValue, newValue) -> oldValue + 1);

			// Set the ID attribute with slug_number
			if (slugNumber > 1) {
				headingSlug = headingSlug + "_" + slugNumber;
			}
			heading.attr("id", headingSlug);
		}

		return body;

	}

	/**
	 * Transforms the given HTML content to replace IDs that have symbols not allowed in CSS
	 * selectors, e.g. ":", ".", etc. The symbols are removed.
	 * <p>
	 * Naturally, the references to these IDs (in {@code <a href="#my_id">}) are also modified.
	 * </p>
	 *
	 * @param body
	 *            HTML content to modify
	 * @return HTML content fixed IDs.
	 * @since 1.0
	 */
	public Element fixIds(final Element body) {

		// Find all IDs and remove unsupported characters
		List<Element> idElems = body.select("*[id]");
		for (Element idElem : idElems) {

			String id = idElem.id();
			String newId = slug(id);
			if (!id.equals(newId)) {
				idElem.attr("id", newId);
			}
		}

		// Then find all <a href="#..."> instances and update their values accordingly
		List<Element> aElems = body.select("a[href^=#]");
		for (Element aElem : aElems) {
			// fix all existing IDs - remove colon and other symbols which mess up jQuery
			String href = aElem.attr("href");
			String newHref = "#" + slug(href.substring(1));
			if (!href.equals(newHref)) {
				aElem.attr("href", newHref);
			}
		}

		// Return result
		return body;
	}

	/**
	 * Fixes table heads: wraps rows with {@code <th>} (table heading) elements into {@code <thead>}
	 * element if they are currently in {@code <tbody>}.
	 *
	 * @param body
	 *            HTML content to modify
	 * @return HTML content with all table heads fixed. If all heads were correct, the original
	 *         content is returned.
	 * @since 1.0
	 */
	public Element fixTableHeads(final Element body) {

		// select rows with <th> tags within <tbody>
		List<Element> tableHeadRows = body.select("table > tbody > tr:has(th)");

		for (Element row : tableHeadRows) {

			// get the row's table
			Element table = row.parent().parent();

			// remove row from its original position
			row.remove();

			// create table header element with the row
			Element thead = new Element(Tag.valueOf("thead"), "");
			thead.appendChild(row);
			// add at the beginning of the table
			table.prependChild(thead);
		}

		return body;
	}


	/**
	 * Regex that matches with all non-latin chars... and dash
	 */
	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");

	/**
	 * Regex that matches with all white spaces and other common word separators
	 */
	private static final Pattern WORD_SEPARATORS = Pattern.compile("[\\s_'()\\[\\]{}/\\|+=*,;:\\.]+");

	/**
	 * Regex that matches with leading and trailing dashes
	 */
	private static final Pattern LEADING_TRAILING_DASHES = Pattern.compile("^-+|-+$");

	/**
	 * Creates a slug (latin text with no whitespace or other symbols) for a longer text (i.e. to
	 * use in URLs). Uses "-" as a word separator.
	 *
	 * @param input The string (free) to be transformed into a valid element ID
	 * @return the proper slug
	 */
	public static String slug(final String input) {
		String normalized = Normalizer.normalize(input, Form.NFD);
		String nowhitespace = WORD_SEPARATORS.matcher(normalized).replaceAll("-");
		String noSpecialChars = NONLATIN.matcher(nowhitespace).replaceAll("");
		return LEADING_TRAILING_DASHES.matcher(noSpecialChars).replaceAll("").toLowerCase();
	}

	/**
	 * Replace all <code>&lt;a href="//..."&gt;</code> links with protocol-relative URLs with
	 * proper HTTPS URLs
	 *
	 * @param body
	 *            HTML content to modify
	 * @return HTML content fixed linkss
	 */
	public Element fixProtocolRelativeUrls(final Element body) {

		// Find all links with HREF that starts with //
		// (i.e. protocol-relative)
		List<Element> aElems = body.select("*[href^=//]");

		// Nothing? Exit immediately
		if (aElems.isEmpty()) {
			return body;
		}

		for (Element aElem : aElems) {

			// Prepend "https:" in front of each protocol-relative link
			String href = aElem.attr("href");
			aElem.attr("href", "https:" + href);

		}

		// Return result
		return body;
	}


}
