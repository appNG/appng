/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.tools.markup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads/inserts an attribute from/to a given XHTML-Tag
 * 
 * @author Matthias Herlitzius
 */
public class XHTML {

	/**
	 * Removes the attribute from the tag
	 * 
	 * @param tag
	 * @param attr
	 * @return the tag without the attribute
	 */
	public static String removeAttr(String tag, String attr) {
		String pattern = getAttributeExpression(attr);
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(tag);
		if (m.find()) {
			tag = tag.replaceAll(pattern, "");
		}
		return tag;
	}

	/**
	 * Sets the attribute of the tag
	 * 
	 * @param tag
	 * @param attr
	 * @param value
	 * @return the tag with the attribute set
	 */
	public static String setAttr(String tag, String attr, String value) {
		// Inject attribut...
		// falls es bereits attribut geben sollte, leer oder vorbelegt
		Pattern p = Pattern.compile(getAttributeExpression(attr));
		Matcher m = p.matcher(tag);

		if (m.find()) {
			tag = tag.replaceFirst(getAttributeExpression(attr), " " + attr + "=\"" + value + "\"");
		} else {
			// noch kein attribut="" vorhanden, füge nach dem Tag ein
			p = Pattern.compile("(<\\w+?)([\\s|>])");
			m = p.matcher(tag);
			if (m.find()) {
				String suffix = (m.group(2).equals(">")) ? ">" : " ";
				tag = m.replaceFirst(m.group(1) + " " + attr + "=\"" + value + "\"" + suffix);
			}
		}
		return tag;
	}

	private static String getAttributeExpression(String attr) {
		return " " + attr + "=\"(.*?)\"";
	}

	public static String getBody(String tag, String content) {
		String regex = "(<" + tag + "(.)*>)(.*)(</( )?" + tag + ">)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		if (matcher.matches() && matcher.groupCount() > 2) {
			String body = matcher.group(3);
			return body;
		}
		return null;
	}

	/**
	 * Sets the body content for the tag
	 * 
	 * @param tag
	 * @param value
	 * @return the tag with the body content
	 */
	public static String setBody(String tag, String value) {
		// detect tagname
		String name = getTag(tag);

		// Inject body of tag...
		tag = tag.replaceFirst(">.*?</" + name + ">", ">" + value + "</" + name + ">");
		return tag;
	}

	/**
	 * Retrieves the attribute of the tag
	 * 
	 * @param tag
	 * @param attr
	 * @return the attribute value
	 */
	public static String getAttr(String tag, String attr) {
		String value = "";
		Pattern p = Pattern.compile(getAttributeExpression(attr));
		Matcher m = p.matcher(tag);
		if (m.find()) {
			value = m.group(1);
		}
		return value;
	}

	/**
	 * Retrieves the tag's name
	 * 
	 * @param tag
	 * @return the tag's name
	 */
	public static String getTag(String tag) {
		String value = "";
		Pattern p = Pattern.compile("<(\\w+?)\\s");
		Matcher m = p.matcher(tag);
		if (m.find()) {
			value = m.group(1);
		}
		return value;
	}
}
