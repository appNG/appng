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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;

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
	 * Handles the {@value org.springframework.http.HttpHeaders#LAST_MODIFIED} and
	 * {@value org.springframework.http.HttpHeaders#IF_MODIFIED_SINCE} headers for the give request/response pair. Sets
	 * the {@value org.springframework.http.HttpHeaders#LAST_MODIFIED}-header for the response and reads the
	 * {@value org.springframework.http.HttpHeaders#IF_MODIFIED_SINCE}-header from the request.
	 * 
	 * @param requestHeaders
	 *            the headers the current {@link HttpServletRequest}
	 * @param responseHeaders
	 *            the headers of the current {@link HttpServletResponse}
	 * @param resource
	 *            the {@link HttpResource} to process
	 * @return the response-data, which may have a length of 0 in case
	 *         {@value org.springframework.http.HttpHeaders#IF_MODIFIED_SINCE} was set for the request and the
	 *         {@link HttpResource} has'nt been updated since then
	 * @throws IOException
	 */
	public static byte[] handleModifiedHeaders(HttpHeaders requestHeaders, HttpHeaders responseHeaders,
			HttpResource resource) throws IOException {
		byte[] result = new byte[0];
		long lastModified = resource.update();
		long ifModifiedSince = requestHeaders.getIfModifiedSince();
		if (ifModifiedSince > 0) {
			logger.debug("received {}={}", HttpHeaders.IF_MODIFIED_SINCE, new Date(ifModifiedSince));
			if (ifModifiedSince > lastModified) {
				logger.debug("setting status {}", HttpServletResponse.SC_NOT_MODIFIED);
				resource.setStatus(HttpStatus.NOT_MODIFIED);
			}
		} else {
			result = resource.getData();
			responseHeaders.setLastModified(lastModified);
			logger.debug("setting {}={}", HttpHeaders.LAST_MODIFIED, new Date(lastModified));
			logger.debug("setting content type {} ({}B)", resource.getContentType(), result.length);
			responseHeaders.setContentType(MediaType.parseMediaType(resource.getContentType()));
			responseHeaders.setContentLength(result.length);
		}

		return result;
	}

	/**
	 * A resource that has been requested by an {@link HttpServletRequest} and eventually needs to be updated.
	 * 
	 * @see HttpHeaderUtils#handleModifiedHeaders(HttpServletRequest, HttpServletResponse, HttpResource, boolean)
	 */
	public interface HttpResource {

		/**
		 * Updates this resource if necessary and returns the latest version.
		 * 
		 * @return the latest version of the resource
		 * @throws IOException
		 *             if an error occurs while updating
		 */
		long update() throws IOException;

		/**
		 * Returns the content-type for this resource
		 * 
		 * @return the content-type
		 */
		String getContentType();

		/**
		 * Whether or not this resource has been updated during {@link #update()}.
		 * 
		 * @return {@code true} if this resource has been updated
		 * 
		 * @see #update()
		 */
		boolean needsUpdate();

		/**
		 * Retrieves the data of this resource.
		 * 
		 * @return the data
		 * @throws IOException
		 *             if an error occurs while retrieving the data
		 */
		byte[] getData() throws IOException;

		/**
		 * Set the status for this response.
		 * 
		 * @param status
		 *            the status
		 */
		default void setStatus(HttpStatus status) {
		}

		/**
		 * Returns the {@link HttpStatus} for this resource. Default: {@code HttpStatus#OK}.
		 * 
		 * @return the status
		 */
		default HttpStatus getStatus() {
			return HttpStatus.OK;
		}

		/**
		 * Returns the {@link HttpHeaders} for this resource. Default: An empty {@link HttpHeaders} object
		 * 
		 * @return the HttpHeaders
		 */
		default HttpHeaders getHeaders() {
			return new HttpHeaders();
		}
	}

	/**
	 * Parses the string-based HTTP headers of the given {@link HttpServletRequest} to an {@link HttpHeaders} object.
	 * 
	 * @param httpServletRequest
	 *            a {@link HttpServletRequest}
	 * @return The immutable {@link HttpHeaders}
	 */
	public static HttpHeaders parse(HttpServletRequest httpServletRequest) {
		BodyBuilder builder = RequestEntity.method(null, null);
		if (null != httpServletRequest) {
			Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String header = headerNames.nextElement();
				@SuppressWarnings("unchecked")
				List<String> headerValues = EnumerationUtils.toList(httpServletRequest.getHeaders(header));
				builder.header(header, headerValues.toArray(new String[headerValues.size()]));
			}
		}
		return HttpHeaders.readOnlyHttpHeaders(builder.build().getHeaders());
	}

	/**
	 * Applies the given {@link HttpHeaders} to the {@link HttpServletResponse}.
	 * 
	 * @param httpServletResponse
	 *            the response to set the headers for
	 * @param headers
	 *            the headers to apply
	 */
	public static void applyHeaders(HttpServletResponse httpServletResponse, HttpHeaders headers) {
		if (null != headers) {
			Set<String> headerNames = new HashSet<>(headers.keySet());
			if (headerNames.remove(HttpHeaders.CACHE_CONTROL)) {
				httpServletResponse.setHeader(HttpHeaders.CACHE_CONTROL, headers.getCacheControl());
			}
			if (headerNames.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
				httpServletResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
						headers.getAccessControlAllowOrigin());
			}
			headerNames.forEach(h -> httpServletResponse.setHeader(h, headers.getFirst(h)));
		}
	}

}
