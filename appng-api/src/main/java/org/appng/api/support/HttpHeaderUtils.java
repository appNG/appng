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
package org.appng.api.support;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Utility-class that helps dealing with several HTTP-Headers
 * 
 * @author Matthias Müller
 *
 */
public class HttpHeaderUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpHeaderUtils.class);

	private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	protected static final FastDateFormat HTTP_DATE = FastDateFormat.getInstance(HTTP_DATE_FORMAT,
			TimeZone.getTimeZone("GMT"), Locale.ENGLISH);

	/**
	 * Handles the {@value org.springframework.http.HttpHeaders#LAST_MODIFIED} and
	 * {@value org.springframework.http.HttpHeaders#IF_MODIFIED_SINCE} headers for the give request/response pair. Sets
	 * the {@value org.springframework.http.HttpHeaders#LAST_MODIFIED}-header for the response and reads the
	 * {@value org.springframework.http.HttpHeaders#IF_MODIFIED_SINCE}-header from the request.
	 * 
	 * @param servletRequest
	 *            the current {@link HttpServletRequest}
	 * @param servletResponse
	 *            the current {@link HttpServletResponse}
	 * @param resource
	 *            the {@link HttpResource} to process
	 * @param output
	 *            whether or not to write the response data to the {@link OutputStream} of the
	 *            {@link HttpServletResponse}, also setting the content-type and content length
	 * @return the response-data, which may have a length of 0 in case
	 *         {@value org.springframework.http.HttpHeaders#IF_MODIFIED_SINCE} was set for the request and the
	 *         {@link HttpResource} has'nt been updated since then
	 * @throws IOException
	 *             if an error occurred while reading/updating the {@link HttpResource}
	 */
	public static byte[] handleModifiedHeaders(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			HttpResource resource, boolean output) throws IOException {
		byte[] result = new byte[0];

		String sessionId = servletRequest.getSession().getId();

		long lastModified = resource.update();

		Date ifModifiedSince = null;
		String ifModifiedSinceHeader = servletRequest.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
		boolean hasModifiedSince = StringUtils.isNotBlank(ifModifiedSinceHeader);
		if (hasModifiedSince) {
			try {
				logger.debug("[{}] received {}={} for {}?{}", sessionId, HttpHeaders.IF_MODIFIED_SINCE,
						ifModifiedSinceHeader, servletRequest.getServletPath(), servletRequest.getQueryString());
				ifModifiedSince = HTTP_DATE.parse(ifModifiedSinceHeader);
			} catch (ParseException e) {
				hasModifiedSince = false;
				logger.debug("[{}] error parsing header {}={} for {}?{} ({})", sessionId, HttpHeaders.IF_MODIFIED_SINCE,
						ifModifiedSinceHeader, servletRequest.getServletPath(), servletRequest.getQueryString(),
						e.getMessage());
			}
		}

		boolean isModifiedAfter = hasModifiedSince && lastModified > ifModifiedSince.getTime();
		if (resource.needsUpdate() || isModifiedAfter || !hasModifiedSince) {
			result = resource.getData();
		}
		if (hasModifiedSince && !isModifiedAfter) {
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			logger.debug("[{}] setting status {} for {}?{}", sessionId, HttpServletResponse.SC_NOT_MODIFIED,
					servletRequest.getServletPath(), servletRequest.getQueryString());
		} else {
			String lastModifiedHeader = HTTP_DATE.format(new Date(lastModified));
			servletResponse.setHeader(HttpHeaders.LAST_MODIFIED, lastModifiedHeader);
			logger.debug("[{}] setting {}={} for {}?{}", sessionId, HttpHeaders.LAST_MODIFIED, lastModifiedHeader,
					servletRequest.getServletPath(), servletRequest.getQueryString());
			if (output) {
				logger.debug("[{}] setting content type {} ({}B) for {}?{}", sessionId, resource.getContentType(),
						result.length, servletRequest.getServletPath(), servletRequest.getQueryString());
				servletResponse.setContentType(resource.getContentType());
				servletResponse.setContentLength(result.length);
				servletResponse.getOutputStream().write(result);
			}
		}

		return result;
	}

	/**
	 * A resource that has been requested by an {@link HttpServletRequest} and eventually needs to be updated
	 * 
	 * @see HttpHeaderUtils#handleModifiedHeaders(HttpServletRequest, HttpServletResponse, HttpResource, boolean)
	 */
	public interface HttpResource {

		/**
		 * updates this resource if necessary and returns the latest version
		 * 
		 * @return the latest version of the resource
		 * @throws IOException
		 *             if an error occurs while updating
		 */
		public long update() throws IOException;

		/**
		 * Returns the content-type for this resource
		 * 
		 * @return the content-type
		 */
		public String getContentType();

		/**
		 * whether or not this resource has been updated during {@link #update()}
		 * 
		 * @return {@code true} if this resource has been updated
		 * 
		 * @see #update()
		 */
		public boolean needsUpdate();

		/**
		 * retrieves the data of this resource
		 * 
		 * @return the data
		 * @throws IOException
		 *             if an error occurs while retrieving the data
		 */
		public byte[] getData() throws IOException;
	}

}
