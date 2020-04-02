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

import java.util.Date;

import org.appng.api.Platform;
import org.appng.api.auth.AuthTools;

/**
 * Utility class to generate a digest of the form {@code <user>|<yyyyMMddHHmmss>|<utc-offset>|<hash>}.<br/>
 * Example:<br/>
 * {@code admin|20160114120555|+01:00|1D87C8A5E738BD3015AC57F2D9B862A5}<br/>
 * The {@code <hash>} is a MD5 hash of {@code <user>|<timestamp>|<utc-offset>|<shared-secret>} , where
 * {@code <shared-secret>} comes from the platform property {@value Platform.Property#SHARED_SECRET}.
 * 
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 *
 * @see DigestValidator
 */
public class DigestUtil {

	/**
	 * Creates and returns a digest.
	 * 
	 * @param username
	 *            the username
	 * @param sharedSecret
	 *            the shared secret
	 * @return the digest
	 */
	public static String getDigest(String username, String sharedSecret) {
		return getDigest(username, sharedSecret, new Date());
	}

	static String getDigest(String username, String sharedSecret, Date date) {
		return getDigest(username, sharedSecret, DigestValidator.DATEFORMAT.format(date.getTime()));
	}

	static String getDigest(String username, String sharedSecret, String timestamp) {
		String digest = username + DigestValidator.PIPE + timestamp + DigestValidator.PIPE;
		digest = digest + AuthTools.getMd5Digest(digest + sharedSecret);
		return digest;
	}

}
