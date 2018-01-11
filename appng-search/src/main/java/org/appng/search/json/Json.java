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
package org.appng.search.json;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Helper class to convert an object to it's JSON representation and vice versa.
 * 
 * @author Matthias Müller
 *
 */
public class Json {

	private boolean pretty = false;
	private ObjectMapper mapper;

	public Json(DateFormat dateFormat, boolean pretty) {
		this.pretty = pretty;
		this.mapper = new ObjectMapper();
		mapper.setDateFormat(dateFormat);
	}

	public Json() {
		this(new SimpleDateFormat(), false);
	}

	public String toJson(Object o, Include include) throws JsonProcessingException, IOException {
		ObjectMapper innerMapper = mapper.setSerializationInclusion(include);

		ObjectWriter writer = innerMapper.writer();
		if (pretty) {
			writer = writer.withDefaultPrettyPrinter();
		}
		StringWriter stringWriter = new StringWriter();
		writer.writeValue(stringWriter, o);
		return stringWriter.toString();
	}

	public String toJson(Object o) throws JsonProcessingException, IOException {
		return toJson(o, Include.NON_NULL);
	}

	public <T> T toObject(String json, Class<T> type, Module... modules) throws IOException {
		for (Module module : modules) {
			mapper.registerModule(module);
		}
		ObjectReader reader = mapper.readerFor(type);
		return reader.readValue(json);
	}
}
