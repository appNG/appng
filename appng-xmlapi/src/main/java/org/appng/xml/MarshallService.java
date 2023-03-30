/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.xml;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.output.WriterOutputStream;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarshallService {

	/** The namespace for appNG application XML-resources */
	public static final String NS_PLATFORM = "http://www.appng.org/schema/platform";
	/** the namespace for a application-info.xml file */
	public static final String NS_APPLICATION = "http://www.appng.org/schema/application";

	private DocumentBuilderFactory documentBuilderFactory;
	private TransformerFactory transformerFactory;
	private boolean throwMarshallingError = false;
	private boolean throwUnmarshallingError = false;
	private List<String> cdataElements;
	private boolean useSchema = false;
	private boolean prettyPrint = false;
	private AppNGSchema schema;
	private String schemaLocation;
	private Schema validationSchema;

	public AppNGSchema getSchema() {
		return schema;
	}

	public void setSchema(AppNGSchema schema) {
		this.schema = schema;
	}

	public boolean isUseSchema() {
		return useSchema;
	}

	public void setUseSchema(boolean useSchema) {
		this.useSchema = useSchema;
	}

	public boolean isPrettyPrint() {
		return prettyPrint;
	}

	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	public enum AppNGSchema {
		PLATFORM("org.appng.xml.platform", NS_PLATFORM, "appng-platform.xsd"),
		APPLICATION("org.appng.xml.application", NS_APPLICATION, "appng-application.xsd");

		private final String xsd;
		private final String namespace;
		private final JAXBContext context;

		private AppNGSchema(String contextPath, String namespace, String xsd) {
			this.xsd = xsd;
			this.namespace = namespace;
			try {
				this.context = JAXBContext.newInstance(contextPath);
			} catch (JAXBException e) {
				throw new IllegalStateException("error while creating JAXBContext for path '" + contextPath + "'");
			}
		}

		public String getXsd() {
			return xsd;
		}

		public String getNamespace() {
			return namespace;
		}

		public JAXBContext getContext() {
			return context;
		}

	}

	public MarshallService() {
	}

	MarshallService(AppNGSchema schema) {
		setSchema(schema);
		init();
	}

	protected Marshaller getMarshaller() throws JAXBException {
		Marshaller marshaller = schema.getContext().createMarshaller();
		if (prettyPrint) {
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		}
		if (useSchema) {
			marshaller.setEventHandler(new MarshallEventHandler(throwMarshallingError));
			if (null != schemaLocation) {
				LOGGER.trace("schemaLocation is {}", schemaLocation);
				marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
						this.schema.getNamespace() + " " + schemaLocation);
			}
			marshaller.setSchema(validationSchema);
		}
		return marshaller;
	}

	protected Unmarshaller getUnmarshaller() throws JAXBException {
		Unmarshaller unmarshaller = schema.getContext().createUnmarshaller();
		if (useSchema) {
			unmarshaller.setEventHandler(new MarshallEventHandler(throwUnmarshallingError));
		}
		return unmarshaller;
	}

	public void init() {
		URL schemaUrl = MarshallService.class.getClassLoader().getResource(schema.getXsd());
		if (useSchema) {
			try {
				LOGGER.trace("using schema {}", schemaUrl);
				SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
				this.validationSchema = sf.newSchema(schemaUrl);
			} catch (SAXException e) {
				LOGGER.error(String.format("error while loading Schema from url %s", schemaUrl), e);
			}
		}
	}

	public static MarshallService getApplicationMarshallService() throws JAXBException {
		MarshallService marshallService = new MarshallService();
		marshallService.setSchema(AppNGSchema.APPLICATION);
		marshallService.init();
		return marshallService;
	}

	public void marshal(Object data, OutputStream out)
			throws ParserConfigurationException, JAXBException, TransformerException {
		Document document = marshallToDocument(data);
		buildTransformer().transform(new DOMSource(document), new StreamResult(out));
		logValidationMessages(new MarshallEventHandler(throwMarshallingError));
	}

	public String marshal(Object data) throws ParserConfigurationException, JAXBException, TransformerException {
		Document document = marshallToDocument(data);
		DOMSource xmlSource = new DOMSource(document);
		StringWriter writer = new StringWriter();
		buildTransformer().transform(xmlSource, new StreamResult(writer));
		logValidationMessages(new MarshallEventHandler(throwMarshallingError));
		return writer.toString();
	}

	private void logValidationMessages(MarshallEventHandler eventHandler) {
		if (null != eventHandler) {
			Collection<ErrorItem> errorItems = eventHandler.getErrorItems().values();
			String separator = System.lineSeparator();
			for (ErrorItem errorItem : errorItems) {
				StringBuilder sb = new StringBuilder("error on " + errorItem.object + ":");
				sb.append(separator);
				sb.append(errorItem.content);
				sb.append(separator);
				for (String message : errorItem.errors) {
					sb.append(message);
					sb.append(separator);
				}
				LOGGER.error(sb.toString());
			}
			eventHandler.clear();
		}
	}

	private Document marshallToDocument(Object data)
			throws ParserConfigurationException, JAXBException, TransformerException {
		Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
		getMarshaller().marshal(data, document);
		return document;
	}

	private Transformer buildTransformer() throws TransformerConfigurationException {
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, prettyPrint ? "yes" : "no");
		StringBuilder cdataSectElement = new StringBuilder();
		for (String element : cdataElements) {
			cdataSectElement.append("{" + schema.getNamespace() + "}" + element + " ");
		}
		transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, cdataSectElement.toString());
		return transformer;
	}

	private String marshalNoValidation(Object data) throws JAXBException {
		StringWriter writer = new StringWriter();
		marshalNoValidation(data, new WriterOutputStream(writer, "UTF-8"));
		return writer.toString();
	}

	private void marshalNoValidation(Object data, OutputStream stream) throws JAXBException {
		try {
			Marshaller marshaller = getMarshaller();
			marshaller.setSchema(null);
			marshaller.marshal(data, stream);
		} finally {
			close(stream);
		}
	}

	private void close(Closeable closeable) {
		if (null != closeable) {
			try {
				closeable.close();
			} catch (IOException e) {
				LOGGER.error("error during close", e);
			}
		}
	}

	public void marshalNoValidation(Object data, Writer writer) throws JAXBException {
		try {
			Marshaller marshaller = getMarshaller();
			marshaller.setSchema(null);
			marshaller.marshal(data, writer);
		} finally {
			close(writer);
		}
	}

	public <T> String marshallNonRoot(Object data) throws JAXBException {
		JAXBElement<?> element = getJAXBElement(data);
		return marshalNoValidation(element);
	}

	public <T, E extends T> String marshallNonRoot(E data, Class<T> clazz) throws JAXBException {
		JAXBElement<?> element = getJAXBElement(data, clazz);
		return marshalNoValidation(element);
	}

	public <T> void marshallNonRoot(T data, OutputStream out) throws JAXBException {
		JAXBElement<T> element = getJAXBElement(data);
		marshalNoValidation(element, out);
	}

	public <T> T unmarshall(Source source, Class<T> clazz) throws JAXBException {
		JAXBElement<T> unmarshal = getUnmarshaller().unmarshal(source, clazz);
		logValidationMessages(new MarshallEventHandler(throwUnmarshallingError));
		return unmarshal.getValue();
	}

	public <T> T unmarshall(File f, Class<T> clazz) throws JAXBException {
		return unmarshall(new StreamSource(f), clazz);
	}

	public <T> T unmarshall(InputStream is, Class<T> clazz) throws JAXBException {
		return unmarshall(new StreamSource(is), clazz);
	}

	public <T> T unmarshall(String data, Class<T> clazz) throws JAXBException {
		return unmarshall(new StreamSource(new StringReader(data)), clazz);
	}

	private Object unmarshall(Source source) throws JAXBException {
		Object object = getUnmarshaller().unmarshal(source);
		logValidationMessages(new MarshallEventHandler(throwUnmarshallingError));
		if (object instanceof JAXBElement<?>) {
			return ((JAXBElement<?>) object).getValue();
		}
		return object;
	}

	public Object unmarshall(File f) throws JAXBException {
		return unmarshall(new StreamSource(f));
	}

	public Object unmarshall(InputStream is) throws JAXBException {
		return unmarshall(new StreamSource(is));
	}

	public Object unmarshall(String data) throws JAXBException {
		return unmarshall(new StreamSource(new StringReader(data)));
	}

	public Object unmarshall(InputSource inputSource) throws JAXBException {
		Object object = getUnmarshaller().unmarshal(inputSource);
		logValidationMessages(new MarshallEventHandler(throwUnmarshallingError));
		return object;
	}

	@SuppressWarnings("unchecked")
	private <T> JAXBElement<T> getJAXBElement(T data) {
		Class<T> clazz = (Class<T>) data.getClass();
		return getJAXBElement(data, clazz);
	}

	private <T, E extends T> JAXBElement<T> getJAXBElement(E data, Class<T> clazz) {
		String simpleName = clazz.getSimpleName();
		String type = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
		QName qName = new QName(schema.getNamespace(), type);
		JAXBElement<T> element = new JAXBElement<T>(qName, clazz, data);
		return element;
	}

	public static MarshallService getMarshallService() throws JAXBException {
		MarshallService marshallService = new MarshallService();
		marshallService.setSchema(AppNGSchema.PLATFORM);
		marshallService.setPrettyPrint(true);
		marshallService.init();
		return marshallService;
	}

	public String getSchemaLocation() {
		return schemaLocation;
	}

	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	public DocumentBuilderFactory getDocumentBuilderFactory() {
		return documentBuilderFactory;
	}

	public void setDocumentBuilderFactory(DocumentBuilderFactory documentBuilderFactory) {
		this.documentBuilderFactory = documentBuilderFactory;
	}

	public TransformerFactory getTransformerFactory() {
		return transformerFactory;
	}

	public void setTransformerFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}

	public List<String> getCdataElements() {
		return cdataElements;
	}

	public void setCdataElements(List<String> cdataElements) {
		this.cdataElements = cdataElements;
	}

	public boolean isThrowMarshallingError() {
		return throwMarshallingError;
	}

	public void setThrowMarshallingError(boolean throwMarshallingError) {
		this.throwMarshallingError = throwMarshallingError;
	}

	public boolean isThrowUnmarshallingError() {
		return throwUnmarshallingError;
	}

	public void setThrowUnmarshallingError(boolean throwUnmarshallingError) {
		this.throwUnmarshallingError = throwUnmarshallingError;
	}

	class ErrorItem {
		private Object object;
		private List<String> errors;
		private String content;

		ErrorItem(Object object, String content) {
			this.object = object;
			this.content = content;
			this.errors = new ArrayList<>();
		}

		void addError(String error) {
			errors.add(error);
		}
	}

	class MarshallEventHandler implements ValidationEventHandler {
		private MarshallService inner;
		private Map<Integer, ErrorItem> errorItems;
		private boolean throwError;

		MarshallEventHandler(boolean throwError) throws JAXBException {
			this.inner = new MarshallService(schema);
			this.throwError = throwError;
			this.errorItems = new HashMap<>();
		}

		public void clear() {
			errorItems.clear();
		}

		public Map<Integer, ErrorItem> getErrorItems() {
			return errorItems;
		}

		public boolean handleEvent(ValidationEvent event) {
			ValidationEventLocator locator = event.getLocator();
			if (throwError) {
				return event.getSeverity() < ValidationEvent.ERROR;
			}
			Object object = locator.getObject();
			int lineNumber = locator.getLineNumber();
			int columnNumber = locator.getColumnNumber();
			if (null != object) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try {
					inner.marshallNonRoot(object, out);
					int id = object.hashCode();
					if (!errorItems.containsKey(id)) {
						errorItems.put(id, new ErrorItem(object, out.toString()));
					}
					ErrorItem errorItem = errorItems.get(id);
					if (null != errorItem) {
						String message = "";
						if (lineNumber > -1 && columnNumber > -1) {
							message += "line " + lineNumber + ", column " + columnNumber;
						}
						message += event.getMessage();
						errorItem.addError(message);
					}
				} catch (JAXBException e) {
					LOGGER.warn("error while marshalling object {}", object);
				}
			} else {
				String message = "";
				if (lineNumber > -1 && columnNumber > -1) {
					message += "error on line " + lineNumber + ", column " + columnNumber + ": ";
				}
				message += event.getMessage();
				LOGGER.error(message);
			}
			return true;
		}
	}

}
