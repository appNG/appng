/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.tools.locator;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Retrieves coordinates from Google Maps via its
 * <a href="https://developers.google.com/maps/documentation/geocoding/#JSON">JSON API</a>.
 * 
 * @author mueller.matthias
 * 
 */

@Slf4j
public class GMapGeoLocator implements GeoLocator {

	public static final String GOOGLE_URI = "googleUri";
	public static final String GOOGLE_SUFFIX = "googleSuffix";
	public static final String GOOGLE_CLIENT_ID = "googleClientId";
	public static final String GOOGLE_SIGNING_KEY = "googleSigningKey";
	public static final String SLEEP_TIME = "sleepTime";

	private static final String ENCODING = "UTF-8";
	private static final String DEFAULT_URI = "https://maps.google.com/maps/api/geocode/json";
	private static final String DEFAULT_SUFFIX = "&sensor=false";
	private static final String DEFAULT_SLEEP_TIME = "100";

	private static final String STATUS = "status";
	private static final String OK = "OK";
	private static final String RESULTS = "results";
	private static final String GEOMETRY = "geometry";
	private static final String LOCATION = "location";
	private static final String FORMATTED_ADDRESS = "formatted_address";
	private static final String LAT = "lat";
	private static final String LNG = "lng";

	private String uri = DEFAULT_URI;
	private String suffix = DEFAULT_SUFFIX;
	private String clientId;
	private String signingKey;
	private Long sleepTime = 100L;

	public void configure(Properties properties) {
		if (null != properties) {
			this.uri = properties.getProperty(GOOGLE_URI, DEFAULT_URI);
			this.suffix = properties.getProperty(GOOGLE_SUFFIX, DEFAULT_SUFFIX);
			this.clientId = properties.getProperty(GOOGLE_CLIENT_ID);
			this.signingKey = properties.getProperty(GOOGLE_SIGNING_KEY);
			this.sleepTime = Long.valueOf(properties.getProperty(SLEEP_TIME, DEFAULT_SLEEP_TIME));
		}
	}

	public Coordinate locate(String zip, String city, String street) {
		return locate(zip, city, street, null);
	}

	public Coordinate locate(String zip, String city, String street, String country) {
		try {
			StringBuilder query = new StringBuilder();
			boolean space = append(query, street, false);
			space = append(query, zip, space);
			space = append(query, city, space);
			append(query, country, space);

			String requestUrl = uri + "?address=" + URLEncoder.encode(query.toString(), ENCODING) + suffix;
			if (StringUtils.isNotBlank(clientId)) {
				requestUrl += "&client=" + clientId;
			}
			JsonNode response = getJsonResponse(buildUrl(requestUrl));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(requestUrl);
				LOGGER.trace(response.toString());
			}

			JsonNode status = response.get(STATUS);
			if (OK.equalsIgnoreCase(status.textValue())) {
				JsonNode result = response.get(RESULTS).get(0);
				JsonNode location = result.get(GEOMETRY).get(LOCATION);
				String address = result.get(FORMATTED_ADDRESS).textValue();
				double lat = location.get(LAT).doubleValue();
				double lng = location.get(LNG).doubleValue();
				Coordinate coordinate = new Coordinate(lat, lng);
				LOGGER.debug("found coordinates for address '{}': {}", address, coordinate);
				return coordinate;
			} else {
				LOGGER.debug("return-code was '{}' for request '{}', no coordinates retrieved", status.textValue(),
						requestUrl);
			}
		} catch (Exception e) {
			LOGGER.warn("error while retrieving coordinates", e);
		}
		return null;
	}

	protected boolean append(StringBuilder query, String street, boolean addSpace) {
		boolean hasValue = StringUtils.isNotBlank(street);
		if (hasValue) {
			if (addSpace) {
				query.append(StringUtils.SPACE);
			}
			query.append(street);
		}
		return hasValue;
	}

	protected JsonNode getJsonResponse(URL url) throws IOException, InterruptedException, GeneralSecurityException {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			Thread.sleep(sleepTime);
			connection.connect();
			InputStream is = connection.getInputStream();
			return new ObjectMapper().reader().readTree(is);
		} finally {
			if (null != connection) {
				connection.disconnect();
			}
		}
	}

	protected URL buildUrl(String requestUrl) throws MalformedURLException, GeneralSecurityException, IOException {
		URL url = new URL(requestUrl);
		if (StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(signingKey)) {
			String signedRequest = new UrlSigner(signingKey).signRequest(url.getPath(), url.getQuery());
			url = new URL(url.getProtocol() + "://" + url.getHost() + signedRequest);
		}
		return url;
	}

	/**
	 * https://developers.google.com/maps/documentation/business/image/auth
	 * https://developers.google.com/maps/documentation/business/webservices/auth#signature_examples
	 **/
	class UrlSigner {

		private byte[] key;

		public UrlSigner(String keyString) throws IOException {
			keyString = keyString.replace('-', '+');
			keyString = keyString.replace('_', '/');
			this.key = Base64.decodeBase64(keyString);
		}

		public String signRequest(String path, String query) throws GeneralSecurityException {
			String resource = path + '?' + query;
			byte[] sigBytes = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_1, key).doFinal(resource.getBytes());
			String signature = Base64.encodeBase64URLSafeString(sigBytes);
			return resource + "&signature=" + signature;
		}
	}

}
