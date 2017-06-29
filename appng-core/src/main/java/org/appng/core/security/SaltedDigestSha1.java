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
package org.appng.core.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.appng.api.auth.AuthTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods required to create a salted hash of a given secret using the SHA-1 algorithm.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class SaltedDigestSha1 implements SaltedDigest {

	private static final Logger log = LoggerFactory.getLogger(SaltedDigestSha1.class);
	private static final String UTF_8 = "UTF-8";
	private static final int ITERATIONS = 763;
	private static final int SALT_LENGTH = 8;

	public String getDigest(String secret, String salt) {
		try {
			byte[] saltBytes = AuthTools.base64ToByte(salt);
			MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_1);
			digest.reset();
			digest.update(saltBytes);
			byte[] input = digest.digest(secret.getBytes(UTF_8));
			for (int i = 0; i < ITERATIONS; i++) {
				digest.reset();
				input = digest.digest(input);
			}
			return AuthTools.byteToBase64(input);
		} catch (IOException e) {
			log.error("An IO Error occured during the digest computation.", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("MessageDigest Algorithm not found.", e);
		}
		return null;
	}

	public String getSalt() {
		return AuthTools.getRandomSalt(SALT_LENGTH);
	}

}
