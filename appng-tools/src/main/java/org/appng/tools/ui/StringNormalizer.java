/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.tools.ui;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * A utility class offering some string operations
 * 
 * @author Matthais Müller
 *
 */
public class StringNormalizer {

	/**
	 * matches every character that is a control character (\p{Cc}, a unicode general category) and not tab (\t),
	 * carriage return (\r) or new line (\n)
	 */
	public static final Pattern NON_PRINTABLE_CHARACTER = Pattern.compile("[\\p{Cc}&&[^\\t\\r\\n]]");

	private static final char[] charsToRemove = new char[] { '!', '"', '§', '$', '%', '&', '/', '(', ')', '=', '?', '´',
			'{', '[', ']', '}', '\\', '`', '+', '-', '*', '%', ':', ',', ';', '<', '>', '°', '^', '#', '~', '\'', '|' };

	// http://glaforge.appspot.com/article/how-to-remove-accents-from-a-string
	// see also org.apache.commons.lang3.StringUtils#stripAccents(String input)
	private static final String NORMALIZE_PATTERN = "\\p{IsM}+";

	/**
	 * Removes the following characters from the given string:<br/>
	 * {@code ! " § $ % & / ( ) = ? ´ { [ ] } \ ` + - * % : , ; < > ° ^ # ~ ' |}<br/>
	 * Additionally, all <a href="https://en.wikipedia.org/wiki/Diacritic">diacritics</a> are removed from the
	 * string.<br/>
	 * Finally, it replaces german umlauts ( ä ö ü ß) with their two-letter representations (ae oe ue ss).
	 * 
	 * @param input
	 *            the input string
	 * @return the "normalized" string
	 */
	public static final String normalize(final String input) {
		String[] searchList = new String[] { "ä", "Ä", "ö", "Ö", "ü", "Ü", "ß" };
		String[] replacementList = new String[] { "ae", "Ae", "oe", "Oe", "ue", "Ue", "ss" };
		String replaced = StringUtils.replaceEachRepeatedly(input, searchList, replacementList);
		String normalizedString = java.text.Normalizer.normalize(replaced, java.text.Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile(NORMALIZE_PATTERN);
		String result = pattern.matcher(normalizedString).replaceAll("");
		for (char c : charsToRemove) {
			result = StringUtils.replaceChars(result, String.valueOf(c), "");
		}
		return result;
	}

	/**
	 * Removes all non-printable characters from the given string.
	 * 
	 * @param value
	 *            the string
	 * @return the string without any non printable characters
	 */
	public static String removeNonPrintableCharacters(final String value) {
		return replaceNonPrintableCharacters(value, StringUtils.EMPTY);
	}

	/**
	 * Replaces all non-printable characters of the given string with the given replacement
	 * 
	 * @param value
	 *            the string
	 * @param replacement
	 *            the replacement string
	 * @return the string with the replacements for non-printable characters
	 */
	public static String replaceNonPrintableCharacters(final String value, final String replacement) {
		if (null != value) {
			return NON_PRINTABLE_CHARACTER.matcher(value).replaceAll(replacement);
		}
		return value;
	}

}
