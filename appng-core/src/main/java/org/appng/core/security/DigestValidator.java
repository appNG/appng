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
package org.appng.core.security;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.auth.AuthTools;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to validate a digest.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 *
 * @see DigestUtil
 */
@Slf4j
public class DigestValidator {

	private final int maxOffsetMinutes;
	private boolean errors = false;
	private Calendar clientDate;
	private final String username;
	private final String timestamp;
	private final String utcOffset;
	private final String hashedPart;

	static final String PIPE = "|";
	static final FastDateFormat DATEFORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss|XXX");

	/**
	 * Validate the digest, assuming a maximum age of 3 minutes.
	 * 
	 * @param digest
	 *            the digest to validate
	 */
	public DigestValidator(String digest) {
		this(digest, 3);
	}

	/**
	 * Validate the digest.
	 * 
	 * @param digest
	 *            the digest to validate
	 * @param maxOffsetMinutes
	 *            the maximum age of the digest in minutes
	 */
	public DigestValidator(String digest, int maxOffsetMinutes) {
		this.maxOffsetMinutes = maxOffsetMinutes;
		String[] digestParts = digest.split(Pattern.quote(PIPE));
		if (digestParts.length == 4) {
			username = checkDigestPart(digestParts[0]);
			timestamp = checkDigestPart(digestParts[1]);
			utcOffset = checkDigestPart(digestParts[2]);
			hashedPart = checkDigestPart(digestParts[3]);
		} else {
			errors = true;
			LOGGER.error("Digest is invalid. It must have 4 segments, but {} segments are detected: {}",
					digestParts.length, digest);
			username = "";
			timestamp = "";
			utcOffset = "";
			hashedPart = "";
		}
	}

	private String checkDigestPart(String dp) {
		if (StringUtils.isNotBlank(dp)) {
			return dp;
		} else {
			errors = true;
			return StringUtils.EMPTY;
		}
	}

	private boolean setClientDate() {
		try {
			Date parsed = DATEFORMAT.parse(timestamp + PIPE + utcOffset);
			clientDate = Calendar.getInstance();
			clientDate.setTime(parsed);
			return true;
		} catch (ParseException e) {
			LOGGER.error("Invalid date format: {}", timestamp);
			return false;
		}
	}

	private boolean validateTimestamp() {
		long serverTime = Calendar.getInstance().getTimeInMillis();
		long clientTime = clientDate.getTimeInMillis();
		int serverOffset = TimeZone.getDefault().getOffset(serverTime);
		int clientOffset = clientDate.getTimeZone().getOffset(clientTime);
		long diff = Math.abs((serverTime + serverOffset) - (clientTime + clientOffset));
		int maxOffset = 1000 * 60 * maxOffsetMinutes;
		if (diff <= maxOffset) {
			return true;
		} else {
			LOGGER.error("Invalid date offset [millis]: {}, maximum is {}", diff, maxOffset);
			return false;
		}
	}

	private boolean validateHashedPart(String sharedSecret) {
		String unhashedDigest = username + PIPE + timestamp + PIPE + utcOffset + PIPE + sharedSecret;
		String hashedDigest = AuthTools.getMd5Digest(unhashedDigest);
		if (hashedDigest.equals(hashedPart)) {
			return true;
		} else {
			LOGGER.error("Encrypted part does not match. Encrypted part is {}, but should be {}", hashedPart,
					hashedDigest);
			return false;
		}
	}

	/**
	 * Validates the digest using the given shared secret.
	 * 
	 * @param sharedSecret
	 *            the shared secret
	 * @return
	 *         <ul>
	 *         <li>{@code true} if the digest is syntactically and semantically correct, i.e. if it not exceeds the
	 *         maximum age and the hash value matches the expected one
	 *         <li>{@code false} otherwise
	 *         </ul>
	 */
	public boolean validate(String sharedSecret) {
		if (!errors && setClientDate() && validateTimestamp() && validateHashedPart(sharedSecret)) {
			LOGGER.info("Digest successfully validated.");
			return true;
		}
		LOGGER.error("Digest validation failed.");
		return false;
	}

	/**
	 * Returns the username that was extracted from the digest.
	 * 
	 * @return the username, if the digest was syntactically correct, an empty String otherwise
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the timestamp that was extracted from the digest.
	 * 
	 * @return the timestamp, if the digest was syntactically correct, an empty String otherwise
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the UTC offset that was extracted from the digest.
	 * 
	 * @return the UTC offset, if the digest was syntactically correct, an empty String otherwise
	 */
	public String getUtcOffset() {
		return utcOffset;
	}

}
