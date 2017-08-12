/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.api.model;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

/**
 * Defines the different types of a {@link Resource}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public enum ResourceType implements FilenameFilter {

	/**
	 * The {@link Application}'s {@code beans.xml} located in the root directory
	 */
	BEANS_XML("", "xml"),

	/** The {@link Application}'s JAR files ({@code *.jar}) located at {@code /lib} */
	JAR("lib", "jar"),

	/** The {@link Application}s XML sources ({@code *.xml}) located at {@code /conf} */
	XML("conf", true, "xml"),

	/**
	 * The {@link Application}s custom XSL stylesheets ({@code *.xsl}) located at {@code /xsl}
	 */
	XSL("xsl", "xsl"),

	/** The {@link Application}s SQL scripts ({@code *.sql}), located at {@code /sql} */
	SQL("sql", true, "sql"),

	/** The {@link Application}s custom (non-XSL) template resources */
	TPL("tpl", true),

	/** The {@link Application}s custom resources, such as .js, .css, .jpg, .png */
	RESOURCE("resources", true),

	/** not yet supported */
	ASSET("assets"),

	/**
	 * The {@link Application}'s dictionaries ({@code *.properties}), located at {@code /dictionary}
	 */
	DICTIONARY("dictionary", "properties"),

	/**
	 * The {@link Application}'s {@code application.xml} located in the root directory
	 */
	APPLICATION("", "xml");

	public static final String BEANS_XML_NAME = "beans.xml";
	public static final String APPLICATION_XML_NAME = "application.xml";
	private final String folder;
	private final Collection<String> allowedFileEndings;
	private final boolean supportsSubfolders;

	private ResourceType(String folder, String... fileTypes) {
		this(folder, false, fileTypes);
	}

	private ResourceType(String folder, boolean supportSubfolders, String... fileTypes) {
		this.folder = folder;
		this.supportsSubfolders = supportSubfolders;
		allowedFileEndings = Arrays.asList(fileTypes);
	}

	/**
	 * Returns the relative path for this type.
	 * 
	 * @return the relative path
	 */
	public String getFolder() {
		return folder;
	}

	/**
	 * Checks whether this type supports subfolders.
	 * 
	 * @return {@code true} if this type supports subfolder, {@code false} otherwise
	 */
	public boolean supportsSubfolders() {
		return supportsSubfolders;
	}

	/**
	 * Checks whether the given file-ending is valid for this type.
	 * 
	 * @param fileEnding
	 *            a file-ending, without '.'
	 * @return {@code true} if the given file-ending is valid for this type, {@code false} otherwise
	 */
	public boolean isValidFileEnding(String fileEnding) {
		if (null == fileEnding) {
			return false;
		}
		return allowedFileEndings.isEmpty() || allowedFileEndings.contains(fileEnding);
	}

	/**
	 * Checks whether the given file-name is valid for this type.
	 * 
	 * @param fileName
	 *            the name of the file
	 * @return {@code true} if the given file-name is valid for this type, {@code false} otherwise
	 */
	public boolean isValidFileName(String fileName) {
		if (this == BEANS_XML) {
			return (BEANS_XML_NAME.equals(fileName));
		} else if (this == APPLICATION) {
			return (APPLICATION_XML_NAME.equals(fileName));
		} else {
			String extension = FilenameUtils.getExtension(fileName);
			return isValidFileEnding(extension.toLowerCase());
		}
	}

	/**
	 * Returns an immutable set of all allowed file-endings (without '.')
	 * 
	 * @return a set of all allowed file-endings. An empty set means all file types are allowed.
	 */
	public Set<String> getAllowedFileEndings() {
		return Collections.unmodifiableSet(new HashSet<String>(allowedFileEndings));
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean accept(File dir, String name) {
		return isValidFileName(name);
	}

}
