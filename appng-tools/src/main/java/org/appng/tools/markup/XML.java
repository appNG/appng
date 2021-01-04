/*
 * Copyright 2011-2021 the original author or authors.
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

import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
public class XML {

	public static String transform(Source xmlSource, Source xsltSource) {
		String result = "";
		ErrorListener errorListener = getErrorListener();
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			tf.setErrorListener(errorListener);
			Templates templates = tf.newTemplates(xsltSource);
			Transformer transformer = templates.newTransformer();
			transformer.setErrorListener(errorListener);
			StringWriter output = new StringWriter();
			transformer.transform(xmlSource, new StreamResult(output));
			result = output.toString();
		} catch (TransformerException te) {
			// logging handled by ErrorListener
		}
		return result;
	}

	private static ErrorListener getErrorListener() {
		return new ErrorListener() {
			public void warning(TransformerException te) throws TransformerException {
				LOGGER.warn(te.getMessageAndLocation(), te);
			}

			public void fatalError(TransformerException te) throws TransformerException {
				LOGGER.error(te.getMessageAndLocation(), te);
			}

			public void error(TransformerException te) throws TransformerException {
				LOGGER.error(te.getMessageAndLocation(), te);
			}
		};
	}

	public static String transform(Source xmlSource, String xsltFile) {
		return transform(xmlSource, new StreamSource(xsltFile));
	}

}
