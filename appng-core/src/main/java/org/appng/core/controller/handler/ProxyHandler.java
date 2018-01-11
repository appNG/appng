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
package org.appng.core.controller.handler;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.Scope;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Matthias Herlitzius
 */
public class ProxyHandler implements RequestHandler {

	private static final String SET_COOKIE = "Set-Cookie";
	private static final String COOKIE = "cookie";
	private static final String LOCATION = "Location";
	private static final String JSESSIONID = "JSESSIONID=";
	private static final String PROXY_JSESSIONID = "proxyJSESSIONID";
	private static final List<String> HEADER_LIST = Arrays.asList(LOCATION, "Date", "Expires", "Last-Modified",
			"Content-Length", SET_COOKIE, "Content-Encoding", "lsrequestid", "Content-Type");
	private static final String PROXY_SESSION_MAP = "proxySessionMap";
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class);
	private static final char CR = '\r';
	private static final String GET = "GET";

	@Override
	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment environment,
			Site site, PathInfo pathInfo) throws ServletException, IOException {

		String path = pathInfo.getServletPath();
		LOGGER.debug("CLIENT REQUEST: Path : " + path);

		String proxyTarget = site.getProperties().getString("proxyTarget");
		String targetURL = proxyTarget + path;
		String urlParameters = "";
		HttpURLConnection connection = null;

		byte[] bytes = null;
		Map<String, List<String>> headers = null;

		try {
			// Create connection
			URL url = new URL(targetURL + getQueryString(servletRequest));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(GET);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			Enumeration<String> requestHeaders = servletRequest.getHeaderNames();
			while (requestHeaders.hasMoreElements()) {
				String headerName = requestHeaders.nextElement();
				String headerValue = servletRequest.getHeader(headerName);
				LOGGER.trace("CLIENT REQUEST: " + headerName + " : " + headerValue);

				String value;
				if (headerName.equals(COOKIE)) {
					String[] cookieValues = headerValue.split(";");
					StringBuilder cookieBuilder = new StringBuilder();
					for (String cookieValue : cookieValues) {
						cookieValue = cookieValue.trim();
						if (cookieValue.startsWith(JSESSIONID)) {
							String jSessionId = environment.getAttribute(Scope.SESSION, PROXY_JSESSIONID);
							cookieBuilder = cookieBuilder.append(jSessionId + ",");
						} else {
							cookieBuilder = cookieBuilder.append(cookieValue + ",");
						}
					}
					String cookieValue = cookieBuilder.toString();
					if (cookieValue.endsWith(",")) {
						cookieValue = cookieValue.substring(0, cookieValue.length() - 1);
					}
					value = cookieValue;
				} else {
					value = headerValue;
				}
				connection.setRequestProperty(headerName, value);
			}

			LOGGER.debug("SERVER REQUEST: Path : " + url);
			logHeaders("SERVER REQUEST: ", connection.getRequestProperties());

			// Send request
			DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
			outputStream.writeBytes(urlParameters);
			outputStream.flush();
			outputStream.close();
			headers = connection.getHeaderFields();
			logHeaders("SERVER RESPONSE: ", headers);

			// Get Response
			InputStream inputStream = connection.getInputStream();
			bytes = IOUtils.toByteArray(inputStream);
			inputStream.close();
		} catch (FileNotFoundException e) {
			// http 404
			InputStream inputStream = connection.getErrorStream();
			bytes = IOUtils.toByteArray(inputStream);
			inputStream.close();
		} catch (IOException e) {
			servletResponse.setStatus(502);
			LOGGER.error("Error while processing request.", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		if (null != bytes) {
			if (null != headers) {
				for (String header : HEADER_LIST) {
					if (headers.containsKey(header)) {
						StringBuilder headerBuilder = new StringBuilder();
						List<String> values = headers.get(header);

						if (header.equals(SET_COOKIE)) {
							for (String value : values) {
								value = value.trim();
								if (value.startsWith(JSESSIONID)) {
									environment.setAttribute(Scope.SESSION, PROXY_JSESSIONID, value);
								} else {
									servletResponse.addHeader(header, value);
								}
							}
						} else {
							values.forEach(value -> headerBuilder.append(value + ","));
							String headerValue = headerBuilder.toString();
							if (headerValue.endsWith(",")) {
								headerValue = headerValue.substring(0, headerValue.length() - 1);
							}
							// String proxyTargetNoProto = proxyTarget.replaceAll("https?://", "");
							// LOGGER.trace("proxyTargetNoProto: " + proxyTargetNoProto);
							// if (header.equals(LOCATION) && headerValue.contains(proxyTargetNoProto)) {
							// String domainNoProto = site.getDomain().replaceAll("https?://", "");
							// LOGGER.trace("domainNoProto: " + domainNoProto);
							// headerValue = headerValue.replace(proxyTargetNoProto, domainNoProto);
							// LOGGER.trace("headerValue after replace: " + headerValue);
							// }

							String bpcWeiredRedirectHost = "localhost80";
							if (header.equals(LOCATION) && headerValue.contains(bpcWeiredRedirectHost)) {
								String domainNoProto = site.getDomain().replaceAll("https?://", "");
								LOGGER.trace("domainNoProto: " + domainNoProto);
								headerValue = headerValue.replace(bpcWeiredRedirectHost, domainNoProto);
								LOGGER.trace("headerValue after replace: " + headerValue);
							}
							servletResponse.setHeader(header, headerValue);
						}

					}
				}
			}

			int responseCode = connection.getResponseCode();
			servletResponse.setStatus(responseCode);

			ServletOutputStream outputStream = servletResponse.getOutputStream();
			outputStream.write(bytes);
			outputStream.flush();
			outputStream.close();

			Collection<String> responseHeaders = servletResponse.getHeaderNames();
			for (String headerName : responseHeaders) {
				String value = servletResponse.getHeader(headerName);
				LOGGER.trace("CLIENT RESPONSE: " + headerName + " : " + value);
			}

		}
	}

	private String getQueryString(HttpServletRequest servletRequest) {
		String queryString = servletRequest.getQueryString();
		if (StringUtils.isNotBlank(queryString)) {
			return "?" + queryString;
		}
		return "";
	}

	private void logHeaders(String message, Map<String, List<String>> headers) {
		for (Entry<String, List<String>> e : headers.entrySet()) {
			String key = e.getKey();
			List<String> value = e.getValue();
			LOGGER.trace(message + key + " : " + value);
		}
	}

}
