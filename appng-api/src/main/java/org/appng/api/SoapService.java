/*
 * Copyright 2011-2020 the original author or authors.
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

import javax.xml.bind.JAXBContext;

import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * This interface has to be implemented for creating SOAP-webservices based on <a href="http://jaxb.java.net/">JAXB</a>
 * and <a href="http://www.springframework.org/spring-ws">Spring-WS</a>. The implementing class has to be annotated with
 * {@link org.springframework.ws.server.endpoint.annotation.Endpoint}.
 * 
 * @author Matthias Müller
 * 
 */
public interface SoapService {

	/**
	 * Returns the contextpath (package-names) for the {@link JAXBContext} to be created. Multiple packages have to be
	 * separated by a colon (':').
	 * 
	 * @return the contextpath
	 * @see JAXBContext#newInstance(String, ClassLoader)
	 */
	String getContextPath();

	/**
	 * Returns the location of the xsd-schema used by the webservice (classpath-relative)
	 * 
	 * @return the location
	 */
	String getSchemaLocation();

	/**
	 * Sets the {@link Application} to use when processing the request
	 * 
	 * @param application
	 *            the {@link Application}
	 */
	void setApplication(Application application);

	/**
	 * Sets the {@link Site} to use when processing the request
	 * 
	 * @param site
	 *            the {@link Site}
	 */
	void setSite(Site site);

	/**
	 * Sets the {@link Environment} to use when processing the request
	 * 
	 * @param environment
	 *            the {@link Environment}
	 */
	void setEnvironment(Environment environment);

}
