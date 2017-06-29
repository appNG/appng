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
package org.appng.testsupport.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.bind.JAXBException;

import org.appng.xml.BaseObject;
import org.appng.xml.MarshallService;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utility class that supports testing if two XML documents have the same content.
 * 
 * @author Matthias Müller
 * 
 */
public class WritingXmlValidator {

	private static final Logger log = LoggerFactory.getLogger(WritingXmlValidator.class);

	/**
	 * Set to {@code true} to (over)write the control-files on (default {@code false}) (see also
	 * {@link #controlFileSource}).
	 */
	public static boolean writeXml = false;

	/**
	 * Set to {@code true} to log the actual XML document (parsed from an JAXB object or a {@link String}) when
	 * validating (default {@code false}).
	 */
	public static boolean logXml = false;

	/**
	 * The default relative path to write control-files to when {@link #writeXml} is {@code true} (default:
	 * {@code src/test/resources/}).
	 */
	public static String controlFileSource = "src/test/resources/";

	static {
		XMLUnit.setNormalizeWhitespace(true);
		XMLUnit.setIgnoreWhitespace(true);
	}

	/**
	 * Writes the document represented by {@code data} to a {@link File}.
	 * 
	 * @param data
	 *            a JAXB object from package {@code org.appng.xml.platform}
	 * @param name
	 *            the path to the file (relative to {@link #controlFileSource} )
	 * @throws IOException
	 *             if an I/O error occurs while writing the file
	 * @return the generated {@link File}
	 */
	public static File writeToDisk(Object data, String name) throws IOException {
		File target = new File(controlFileSource + name);
		FileOutputStream out = new FileOutputStream(target);
		try {
			getMarshallService().marshallNonRoot(data, out);
		} catch (JAXBException e) {
			throw new IOException("error while marshalling " + data, e);
		}
		return target;
	}

	/**
	 * Writes the document represented by {@code data} to a {@link File}.
	 * 
	 * @param data
	 *            a XML string
	 * @param name
	 *            the path to the file (relative to {@link #controlFileSource} )
	 * @throws IOException
	 *             if an I/O error occurs while writing the file
	 * @return the generated {@link File}
	 */
	public static File writeToDiskPlain(String data, String name) throws IOException {
		File target = new File(controlFileSource + name);
		FileOutputStream out = new FileOutputStream(target);
		out.write(data.getBytes());
		out.close();
		return target;
	}

	/**
	 * Validates that the document represented by {@code object} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param object
	 *            a JAXB object from package {@code org.appng.xml.platform}
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(BaseObject object, String controlFile) throws IOException {
		validateXml(object, controlFile, null);
	}

	/**
	 * Validates that the document represented by {@code object} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param object
	 *            a JAXB object from package {@code org.appng.xml.platform}
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @param differenceListener
	 *            an optional {@link DifferenceListener} that is applied when building the {@link Diff} between the
	 *            documents
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(BaseObject object, String controlFile, DifferenceListener differenceListener)
			throws IOException {
		try {
			String resultXml = getMarshallService().marshallNonRoot(object);
			if (writeXml) {
				File target = writeToDisk(object, controlFile);
				validateXml(resultXml, target, false, differenceListener);
			} else {
				validateXml(resultXml, controlFile, false, differenceListener);
			}
		} catch (JAXBException e) {
			throw new IOException("error while marshalling " + object, e);
		}
	}

	/**
	 * Validates that the document represented by {@code result} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param result
	 *            a XML string
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(String result, String controlFile) throws IOException {
		validateXml(result, controlFile, null);
	}

	/**
	 * Validates that the document represented by {@code result} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param result
	 *            a XML string
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @param differenceListener
	 *            an optional {@link DifferenceListener} that is applied when building the {@link Diff} between the
	 *            documents
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(String result, String controlFile, DifferenceListener differenceListener)
			throws IOException {
		if (writeXml) {
			writeToDiskPlain(result, controlFile);
		}
		InputSource expected = getControlSource(controlFile);
		validate(expected, result, differenceListener);
	}

	/**
	 * Validates that the document represented by {@code result} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param result
	 *            a XML string
	 * @param controlFile
	 *            the control file
	 * @param differenceListener
	 *            an optional {@link DifferenceListener} that is applied when building the {@link Diff} between the
	 *            documents
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(String result, File controlFile, DifferenceListener differenceListener)
			throws IOException {
		InputSource expected = getControlSource(controlFile);
		validate(expected, result, differenceListener);
	}

	/**
	 * Validates that the document represented by {@code result} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param result
	 *            a XML string
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @param trimComment
	 *            set to {@code true} to un-comment previously commented elements
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(String result, String controlFile, boolean trimComment) throws IOException {
		validateXml(result, controlFile, trimComment, null);
	}

	/**
	 * Validates that the document represented by {@code result} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param result
	 *            a XML string
	 * @param controlFile
	 *            the path to the control file (relative to the classpath)
	 * @param trimComment
	 *            set to {@code true} to un-comment previously commented elements
	 * @param differenceListener
	 *            an optional {@link DifferenceListener} that is applied when building the {@link Diff} between the
	 *            documents
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(String result, String controlFile, boolean trimComment,
			DifferenceListener differenceListener) throws IOException {
		if (trimComment) {
			result = result.replace("<!--", "").trim();
			result = result.replace("-->", "").trim();
		}
		validateXml(result, controlFile, differenceListener);
	}

	/**
	 * Validates that the document represented by {@code result} is equal to the document parsed from the
	 * {@code controlFile}.
	 * 
	 * @param result
	 *            a XML string
	 * @param controlFile
	 *            the control file
	 * @param trimComment
	 *            set to {@code true} to un-comment previously commented elements
	 * @param differenceListener
	 *            an optional {@link DifferenceListener} that is applied when building the {@link Diff} between the
	 *            documents
	 * @throws IOException
	 *             if an I/O error occurs while validating
	 * @throws AssertionError
	 *             if the validation fails
	 */
	public static void validateXml(String result, File controlFile, boolean trimComment,
			DifferenceListener differenceListener) throws IOException {
		if (trimComment) {
			result = result.replace("<!--", "").trim();
			result = result.replace("-->", "").trim();
		}
		validateXml(result, controlFile, differenceListener);
	}

	private static void validate(InputSource expected, String resultXml, DifferenceListener differenceListener)
			throws IOException {
		InputSource actual = new InputSource(new StringReader(resultXml));
		Document controlDoc = getDocument(expected, null);
		Document testDoc = getDocument(actual, resultXml);
		Diff myDiff = new Diff(controlDoc, testDoc);
		if (null != differenceListener) {
			myDiff.overrideDifferenceListener(differenceListener);
		}
		if (logXml) {
			log.debug("\r\n" + resultXml);
		}
		String message = "XML does not match control XML\r\n" + myDiff.toString() + "\r\n" + resultXml;
		XMLAssert.assertXMLIdentical(message, myDiff, true);

	}

	private static Document getDocument(InputSource source, String input) throws IOException {
		try {
			return XMLUnit.buildDocument(XMLUnit.newControlParser(), source);
		} catch (SAXException e) {
			String message = "failed to parse xml";
			if (e instanceof SAXParseException) {
				SAXParseException spe = ((SAXParseException) e);
				int lineNumber = spe.getLineNumber();
				int columnNumber = spe.getColumnNumber();
				message += " at line " + lineNumber + ", column " + columnNumber;
			}
			if (null != input) {
				message += "\r\n" + input;
			}
			throw new IOException(message, e);
		}
	}

	private static InputSource getControlSource(String controlFile) throws IOException {
		InputStream is = WritingXmlValidator.class.getClassLoader().getResourceAsStream(controlFile);
		if (null == is) {
			throw new FileNotFoundException(controlFile);
		}
		return new InputSource(is);
	}

	private static InputSource getControlSource(File controlFile) throws IOException {
		return new InputSource(new FileInputStream(controlFile));
	}

	private static MarshallService getMarshallService() throws JAXBException {
		return MarshallService.getMarshallService();
	}

}
