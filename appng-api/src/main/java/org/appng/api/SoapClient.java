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
package org.appng.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Result;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;
import org.springframework.xml.transform.StringResult;

/**
 * 
 * A simple SOAP-client for <a href="http://jaxb.java.net/">JAXB</a>-based webservices.
 * 
 * @author Matthias Müller
 * 
 */
public class SoapClient {

	private WebServiceTemplate webServiceTemplate;
	private Jaxb2Marshaller marshaller;
	private int connectTimeout = 0;
	private int readTimeout = 0;

	/**
	 * @param contextPath
	 *            the contextpath (package-names) for the {@link JAXBContext} to be created. Multiple packages have to
	 *            be separated by a colon (':').
	 * @param uri
	 *            the endpoint uri of the webservice
	 * 
	 * @see JAXBContext#newInstance(String, ClassLoader)
	 */
	public SoapClient(String contextPath, String uri) {
		this(contextPath, uri, false);
	}

	/**
	 * @param contextPath
	 *            the contextpath (package-names) for the {@link JAXBContext} to be created. Multiple packages have to
	 *            be separated by a colon (':').
	 * @param uri
	 *            the endpoint uri of the webservice
	 * @param format
	 *            {@code true} if {@link Marshaller} should format output
	 */
	public SoapClient(String contextPath, String uri, boolean format) {
		this.marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(contextPath);
		Map<String, Object> properties = new HashMap<>();
		properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, format);
		marshaller.setMarshallerProperties(properties);
		this.webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setMessageSender(new HttpUrlConnectionMessageSender() {
			@Override
			protected void prepareConnection(HttpURLConnection connection) throws IOException {
				super.prepareConnection(connection);
				connection.setConnectTimeout(connectTimeout);
				connection.setReadTimeout(readTimeout);
			}
		});
		webServiceTemplate.setMarshaller(marshaller);
		webServiceTemplate.setUnmarshaller(marshaller);
		webServiceTemplate.setDefaultUri(uri);
	}

	/**
	 * marshals the given object to a String using {@link StringResult}
	 * 
	 * @see org.springframework.oxm.Marshaller#marshal(Object, Result)
	 */
	public String marshalToString(Object object) {
		StringResult result = new StringResult();
		marshalToResult(object, result);
		return result.toString();
	}

	/**
	 * marshals the given object to the given result
	 * 
	 * @see org.springframework.oxm.Marshaller#marshal(Object, Result)
	 */
	public void marshalToResult(Object object, Result result) {
		marshaller.marshal(object, result);
	}

	/**
	 * sends the given request to the uri which was passed to the constructor
	 * 
	 * @param request
	 *            the request, needs to be an {@link XmlRootElement}
	 * @return an instance of the desired type
	 */
	@SuppressWarnings("unchecked")
	public <T> T send(Object request) {
		return (T) webServiceTemplate.marshalSendAndReceive(request);
	}

	/**
	 * 
	 * @return
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * 
	 * @param connectTimeout
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 
	 * @return
	 */
	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * 
	 * @param readTimeout
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

}
