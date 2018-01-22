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
package org.appng.testsupport.validation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Datasource;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class that supports testing if two JSON documents have the same content.
 *
 * @author Matthias Müller
 *
 */
public class WritingJsonValidator {

	private static final Logger log = LoggerFactory.getLogger(WritingJsonValidator.class);

	/**
	 * Set to {@code true} to (over)write the control-files on (default {@code false}) (see also
	 * {@link #controlFileSource}).
	 */
	public static boolean writeJson = false;

	/**
	 * Set to {@code true} to log the actual JSON document (debug level) when validating (default {@code false}).
	 */
	public static boolean logJson = false;

	/**
	 * Set to {@code true} to let the object mapper sort the properties of an object alphabetically more details at
	 * {@link MapperFeature}.SORT_PROPERTIES_ALPHABETICALLY
	 */
	public static boolean sortPropertiesAlphabetically = false;

	/**
	 * The default relative path to write control-files to when {@link #writeJson} is {@code true} (default:
	 * {@code src/test/resources/}).
	 */
	public static String controlFileSource = "src/test/resources/";

	/**
	 * Writes the document represented by {@code data} to a {@link File}.
	 *
	 * @param data
	 *            a {@link JsonWrapper}
	 * @param name
	 *            the path to the file (relative to {@link #controlFileSource} )
	 * @throws IOException
	 *             if an I/O error occurs while writing the file
	 * @return the generated {@link File}
	 */
	public static File writeToDisk(Object data, String name) throws IOException {
		return writeToDiskPlain(toJSON(data), name);
	}

	/**
	 * Writes the document represented by {@code json} to a {@link File}.
	 *
	 * @param json
	 *            a JSON string
	 * @param controlFile
	 *            the path to the file (relative to {@link #controlFileSource} )
	 * @throws IOException
	 *             if an I/O error occurs while writing the file
	 * @return the generated {@link File}
	 */
	public static File writeToDiskPlain(String json, String controlFile) throws IOException {
		File target = new File(controlFileSource, controlFile);
		try (FileOutputStream out = new FileOutputStream(target)) {
			IOUtils.write(json.getBytes(), out);
		}
		return target;
	}

	/**
	 * Validates that the JSON created from the {@code action} is equal to the document parsed from the
	 * {@code controlFile}.
	 *
	 * @param action
	 *            the {@link Action} to create JSON from
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an error occurs while creating the JSON or while reading the control file
	 */
	public static void validate(Action action, String controlFile) throws IOException {
		validate(new JsonWrapper(action), controlFile);
	}

	/**
	 * Validates that the JSON created from the {@code datasource} is equal to the document parsed from the
	 * {@code controlFile}.
	 *
	 * @param datasource
	 *            the {@link Datasource} to create JSON from
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an error occurs while creating the JSON or while reading the control file
	 */
	public static void validate(Datasource datasource, String controlFile) throws IOException {
		validate(new JsonWrapper(datasource), controlFile);
	}

	/**
	 * Validates that the JSON created from the {@code object} is equal to the document parsed from the
	 * {@code controlFile}.
	 *
	 * @param object
	 *            the object to create JSON from
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an error occurs while creating the JSON or while reading the control file
	 */
	public static void validate(Object object, String controlFile) throws IOException {
		validate(toJSON(object), controlFile);
	}

	/**
	 * Validates that the JSON created from the {@code object} is equal to the document parsed from the
	 * {@code controlFile}.
	 *
	 * @param objectMapper
	 *            the custom {@link ObjectMapper} to use
	 * @param object
	 *            the object to create JSON from
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an error occurs while creating the JSON or while reading the control file
	 */
	public static void validate(ObjectMapper objectMapper, Object object, String controlFile) throws IOException {
		String json = toJSON(objectMapper, object);
		if (writeJson) {
			writeToDiskPlain(json, controlFile);
		}
		Assert.assertEquals(FileUtils.readFileToString(getControlFile(controlFile), StandardCharsets.UTF_8), json);
	}

	/**
	 * Validates that {@code json}-string is equal to the document parsed from the {@code controlFile}.
	 *
	 * @param json
	 *            the JSON string
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an error occurs while parsing the JSON string or while reading the control file
	 */
	public static void validate(String json, String controlFile) throws IOException {
		if (writeJson) {
			writeToDiskPlain(json, controlFile);
		}
		Assert.assertEquals(FileUtils.readFileToString(getControlFile(controlFile), StandardCharsets.UTF_8), json);
	}

	/**
	 * Reads the given control file from the classpath
	 *
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @return the file
	 * @throws IOException
	 *             if an error occurs while reading the control file
	 */
	public static File getControlFile(String controlFile) throws IOException {
		try {
			return new File(WritingJsonValidator.class.getClassLoader().getResource(controlFile).toURI());
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Maps the given {@code object} to a JSON string.
	 *
	 * @param object
	 *            the object to be mapped
	 * @return the JSON mapped from the object
	 * @throws IOException
	 *             if an error occurs while mapping the object to JSON
	 */
	public static String toJSON(Object object) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(Include.NON_EMPTY)
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, sortPropertiesAlphabetically);
		return toJSON(objectMapper, object);
	}

	/**
	 * Maps the given {@code object} to a JSON string using the given {@link ObjectMapper}.
	 *
	 * @param objectMapper
	 *            the the custom {@link ObjectMapper} to use
	 * @param object
	 *            the object to be mapped
	 * @return the JSON mapped from the object
	 * @throws IOException
	 *             if an error occurs while mapping the object to JSON
	 */
	public static String toJSON(ObjectMapper objectMapper, Object object) throws IOException {
		StringWriter jsonWriter = new StringWriter();
		objectMapper.writer().withDefaultPrettyPrinter().writeValue(jsonWriter, object);
		String json = jsonWriter.toString();
		if (logJson) {
			log.debug(json);
		}
		return json;
	}

	/**
	 * A wrapper class for {@link Action}s and {@link Datasource}s
	 *
	 * @author Matthias Müller
	 *
	 */
	public static class JsonWrapper {
		Action action;
		Datasource datasource;

		JsonWrapper(Action action) {
			this.action = action;
		}

		JsonWrapper(Datasource datasource) {
			this.datasource = datasource;
		}

		public Action getAction() {
			return action;
		}

		public Datasource getDatasource() {
			return datasource;
		}
	}

}
