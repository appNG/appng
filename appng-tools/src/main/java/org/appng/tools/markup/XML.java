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

import java.io.StringWriter;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
public class XML {

	private static Logger LOG = LoggerFactory.getLogger(XML.class);

	public static String transform(Source xmlSource, Source xsltSource) {
		String result = "";
		ErrorListener errorListener = getErrorListener();
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setErrorListener(errorListener);
		try {
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
				LOG.warn(te.getMessageAndLocation(), te);
			}

			public void fatalError(TransformerException te) throws TransformerException {
				LOG.error(te.getMessageAndLocation(), te);
			}

			public void error(TransformerException te) throws TransformerException {
				LOG.error(te.getMessageAndLocation(), te);
			}
		};
	}

	public static String transform(Source xmlSource, String xsltFile) {
		return transform(xmlSource, new StreamSource(xsltFile));
	}

}
