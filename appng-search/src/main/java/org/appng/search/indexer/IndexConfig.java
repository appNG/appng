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
package org.appng.search.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.appng.search.Search;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@code IndexConfig} is used to provide different index configurations for different folders.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class IndexConfig {

	private static final String SLASH = "/";

	private List<ConfigEntry> entries = new ArrayList<>();

	private Map<String, ConfigEntry> entryMap = new HashMap<>();

	private OpenMode openMode;

	private String tagPrefix;

	/**
	 * Creates a new {@code ParseTags} using the given tag-prefix (usually {@code appNG}).
	 * 
	 * @param tagPrefix
	 *            the tag prefix to use
	 */
	public IndexConfig(String tagPrefix) {
		this.tagPrefix = tagPrefix;
	}

	/**
	 * Parses a given String to an {@link IndexConfig}, using the pipe '|' for separating folders.<br/>
	 * format:<br/>
	 * {@code <folder>;<language>;<analyzer-class>|}<br/>
	 * Example:<br/>
	 * {@code /de;de-DE;GermanAnalyzer|/en;en-US;org.apache.lucene.analysis.en.EnglishAnalyzer|/assets;de-DE;GermanAnalyzer}
	 * </p>
	 * If the analyzer class isn't fully qualified, the following name-schema is applied:<br/>
	 * {@code org.apache.lucene.analysis.<folder>.<analyzer-class>}
	 * 
	 * @param configString
	 *            the string to parse the {@link IndexConfig} from
	 * @param tagPrefix
	 *            the tag prefix to use
	 * @return the {@link IndexConfig} instance
	 */
	public static IndexConfig getInstance(String configString, String tagPrefix) {
		String[] splitted = StringUtils.split(configString, "|");
		return getInstance(Arrays.asList(splitted), tagPrefix);
	}

	/**
	 * Parses a list of strings to an {@link IndexConfig}. Each string has the following format:<br/>
	 * {@code <folder>;<language>;<analyzer-class>}<br/>
	 * Example:<br/>
	 * {@code /en;en-US;org.apache.lucene.analysis.en.EnglishAnalyzer}
	 * 
	 * @param configEntries
	 *            a list of config entries
	 * @param tagPrefix
	 *            the tag prefix to use
	 * @return the {@link IndexConfig} instance
	 */
	public static IndexConfig getInstance(List<String> configEntries, String tagPrefix) {
		IndexConfig config = new IndexConfig(tagPrefix);
		for (String line : configEntries) {
			String[] splitted = StringUtils.split(line, ";");
			String folder = splitted[0].trim();
			if (!folder.startsWith(SLASH)) {
				folder += SLASH;
			}
			String language = splitted[1].trim();
			String analyzerClass = splitted[2].trim();
			config.addEntry(folder, language, analyzerClass);
		}
		return config;
	}

	/**
	 * A {@code ConfigEntry} provides informations about how to index a certion folder, which includes the name of the
	 * folder itself, the language-code for the contents of this folder, and the name of the Lucene {@link Analyzer} to
	 * use.
	 * 
	 * @author Matthias Müller
	 * 
	 */
	public class ConfigEntry {
		private final String folder;
		private final String language;
		private final String analyzerClass;

		public ConfigEntry(String folder, String language, String analyzerClass) {
			super();
			this.folder = folder;
			this.language = language;
			this.analyzerClass = analyzerClass;
		}

		@SuppressWarnings("unchecked")
		public Analyzer getAnalyzer() {
			Analyzer analyzer = null;
			Class<? extends Analyzer> clazz = null;
			try {
				clazz = (Class<? extends Analyzer>) Class.forName(analyzerClass);
			} catch (ClassNotFoundException e1) {
				String className = "org.apache.lucene.analysis." + getFolder().replaceAll("/", "") + "."
						+ analyzerClass;
				LOGGER.info("could not find analyzer class '{}', trying '{}'", analyzerClass, className);
				try {
					clazz = (Class<? extends Analyzer>) Class.forName(className);
				} catch (ClassNotFoundException e2) {
					LOGGER.info("could not find analyzer class '{}', trying '{}'", analyzerClass, className);
				}
			}
			if (null != clazz) {
				try {
					return clazz.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					LOGGER.warn("could not instanciate class '{}'", clazz.getName());
				}
			}
			if (null == analyzer) {
				LOGGER.info("no analyzer found for folder '{}', using default {}", getFolder(),
						Search.getDefaultAnalyzerClass().getName());
				analyzer = Search.getDefaultAnalyzer();
			}
			return analyzer;
		}

		public String getFolder() {
			return folder;
		}

		public String getLanguage() {
			return language;
		}

		public String getAnalyzerClass() {
			return analyzerClass;
		}

	}

	public SortedSet<String> getFolders() {
		SortedSet<String> folders = new TreeSet<>();
		folders.addAll(entryMap.keySet());
		return folders;
	}

	public ConfigEntry getEntry(String folder) {
		return entryMap.get(folder);
	}

	public void addEntry(String folder, String language, String analyzerClass) {
		ConfigEntry entry = new ConfigEntry(folder, language, analyzerClass);
		entryMap.put(entry.getFolder(), entry);
		this.entries.add(entry);
	}

	public OpenMode getOpenMode() {
		return openMode;
	}

	public void setOpenMode(OpenMode openMode) {
		this.openMode = openMode;
	}

	public String getTagPrefix() {
		return tagPrefix;
	}

}
