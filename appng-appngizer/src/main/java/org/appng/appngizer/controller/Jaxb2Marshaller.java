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
package org.appng.appngizer.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.apache.commons.lang3.StringUtils;
import org.appng.appngizer.model.xml.Linkable;
import org.springframework.oxm.UncategorizedMappingException;

@SuppressWarnings("restriction")
public class Jaxb2Marshaller extends org.springframework.oxm.jaxb.Jaxb2Marshaller
		implements com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler {

	private static final String CHARACTER_ESCAPE_HANDLER = "com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler";
	private final String[] searchList = new String[] { "<", ">", "&" };
	private final String[] replacementList = new String[] { "&lt;", "&gt;", "&amp;" };

	@Override
	public boolean supports(Class<?> clazz) {
		return Linkable.class.isAssignableFrom(clazz);
	}

	@Override
	protected Marshaller createMarshaller() {
		javax.xml.bind.Marshaller marshaller = super.createMarshaller();
		try {
			marshaller.setProperty(CHARACTER_ESCAPE_HANDLER, this);
		} catch (PropertyException e) {
			throw new UncategorizedMappingException("error setting " + CHARACTER_ESCAPE_HANDLER, e);
		}
		return marshaller;
	}

	public void escape(char[] buf, int start, int len, boolean isAttValue, Writer out) throws IOException {
		StringWriter buffer = new StringWriter();
		for (int i = start; i < start + len; i++) {
			buffer.write(buf[i]);
		}
		String value = buffer.toString();
		if (value.contains(StringUtils.CR) || value.contains(StringUtils.LF)) {
			out.write("<![CDATA[");
			out.write(value);
			out.write("]]>");
		} else {
			out.write(StringUtils.replaceEach(value, searchList, replacementList));
		}
	}

}
