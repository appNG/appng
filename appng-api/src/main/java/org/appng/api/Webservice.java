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
package org.appng.api;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkmode;
import org.springframework.http.HttpStatus;

/**
 * A {@link Webservice} usually returns some text-based data like XML, JSON or even plain text, which can directly be
 * displayed/processed by the client. The implementing class needs to be defined in the application's {@code beans.xml}.
 * <p>
 * The resulting URI follows this schema:<br/>
 * {@code <site-host>/service/<site-name>/<application-name>/webservice/<bean-id>?<get-params>} <br/>
 * Example:<br/>
 * {@code  http://localhost:8080/service/appng/demoapplication/webservice/timeService?country=de}
 * <p>
 * Usually, a link to the {@link Webservice} is provided by defining a {@link Link} in mode {@link Linkmode#WEBSERVICE},
 * where the target is the bean name, optionally with some additional GET-Parameters.
 * 
 * <pre>
 * &lt;link mode="webservice" target="timeService?country=de" >
 *    &lt;label id="time" />
 *    &lt;icon>time&lt;/icon>
 * &lt;/link>
 * </pre>
 * 
 * For providing files to download, see {@link AttachmentWebservice}.<br/>
 * For 'real' SOAP-Webservices, use {@link SoapService} instead.
 * 
 * @author Matthias Müller
 * @see AttachmentWebservice
 * @see SoapService
 */
public interface Webservice {

	/**
	 * Processes the current {@link Request} and return some data.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param environment
	 *            the current {@link Environment}
	 * @param request
	 *            the current {@link Request}
	 * @return some bytes
	 * @throws BusinessException
	 *             if an error occures during retrieving the data
	 */
	byte[] processRequest(Site site, Application application, Environment environment, Request request)
			throws BusinessException;

	/**
	 * Returns the content-type for the data returned by
	 * {@link #processRequest(Site, Application, Environment, Request)} (optional)
	 * 
	 * @return the content-type
	 */
	String getContentType();

	/**
	 * returns the HTTP status code for this service
	 * 
	 * @return the HTTP status code
	 */
	default int getStatus() {
		return HttpStatus.OK.value();
	}

}
