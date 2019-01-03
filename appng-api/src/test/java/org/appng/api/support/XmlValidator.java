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
package org.appng.api.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.appng.xml.MarshallService;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlValidator {

	public static boolean useFullClassName = false;
	protected static MarshallService marshallService;

	static class ValidationError extends Error {

		public ValidationError(String message, Throwable cause) {
			super(message, cause);
		}

	}

	static {
		try {
			marshallService = MarshallService.getMarshallService();
			XMLUnit.setIgnoreWhitespace(true);
		} catch (JAXBException e) {
			throw new IllegalStateException("failed creating MarshallService", e);
		}
	}

	public static void validate(Object object) {
		validateInternal(object, null);
	}

	public static void validate(Object object, String suffix) {
		validateInternal(object, suffix);
	}

	private static void validateInternal(Object object, String suffix) {
		try {
			String actualXml = marshallService.marshallNonRoot(object);
			StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
			String method = stackTraceElement.getMethodName();
			String className = stackTraceElement.getClassName();
			if (!useFullClassName) {
				className = className.substring(className.lastIndexOf('.') + 1);
			}
			String fileName = "xml/" + className + "-" + method + (null == suffix ? "" : suffix) + ".xml";
			ClassLoader classLoader = XmlValidator.class.getClassLoader();
			InputSource controlSource = new InputSource(classLoader.getResourceAsStream(fileName));
			InputSource actualSource = new InputSource(new ByteArrayInputStream(actualXml.getBytes()));
			Diff diff = new Diff(controlSource, actualSource);
			if (!diff.identical()) {
				Assert.fail(diff.toString() + System.getProperty("line.separator") + actualXml);
			}
		} catch (SAXException e) {
			throw new ValidationError("error creating Diff", e);
		} catch (IOException e) {
			throw new ValidationError("error creating Diff", e);
		} catch (JAXBException e) {
			throw new ValidationError("error while marshalling object", e);
		}
	}
}
