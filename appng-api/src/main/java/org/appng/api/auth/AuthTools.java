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
package org.appng.api.auth;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to get different types of digests.
 * 
 * @author Matthias Herlitzius
 */
@Slf4j
public final class AuthTools {

	private static final String STRING_FORMAT = "%1$032X";
	private static final String SHA1PRNG = "SHA1PRNG";

	private AuthTools() {

	}

	/**
	 * From a base 64 representation, returns the corresponding byte[]
	 * 
	 * @param data
	 *            The base 64 data to be converted to byte[].
	 * @return The base 64 data converted to byte[].
	 * @throws IOException
	 */
	public static byte[] base64ToByte(String data) throws IOException {
		return Base64.decodeBase64(data.getBytes());
	}

	/**
	 * From a byte[] returns a base 64 representation.
	 * 
	 * @param data
	 *            The byte[] to be converted to base 64.
	 * @return The byte[] converted to base 64.
	 */
	public static String byteToBase64(byte[] data) {
		return new String(Base64.encodeBase64(data));
	}

	/**
	 * Returns a random salt with a given length.
	 * 
	 * @param length
	 *            The length of the salt in byte.
	 * @return A random salt, represented as base 64. The value of the length parameter is only relevant for the length
	 *         of byte[], which is internally used to generate the salt. The length of the returned base 64
	 *         representation of the salt may be different than the value of the length parameter.
	 */
	public static String getRandomSalt(int length) {
		return byteToBase64(getRandomSaltBytes(length));
	}

	private static byte[] getRandomSaltBytes(int length) {
		byte[] salt = null;
		try {
			Random random = SecureRandom.getInstance(SHA1PRNG);
			salt = new byte[length];
			random.nextBytes(salt);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("error while generting random string.", e);
		}
		return salt;
	}

	private static String getDigest(String input, String algorithm) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			md.update(input.getBytes());
			BigInteger hash = new BigInteger(1, md.digest());
			return String.format(STRING_FORMAT, hash);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae);
		}
	}

	/**
	 * Returns a MD5 digest. Although this convenience method is provided, it is recommended to use the stronger
	 * {@link #getSha512Digest(String)} method or at least the {@link #getSha1Digest(String)} method.
	 * 
	 * @param input
	 *            the input string
	 * @return the MD5 hash of the input string
	 */
	public static String getMd5Digest(String input) {
		return getDigest(input, MessageDigestAlgorithms.MD5);
	}

	/**
	 * Returns a SHA-1 digest. It is recommended to use the stronger {@link #getSha512Digest(String)} method.
	 * 
	 * @param input
	 *            the input string
	 * @return the SHA-1 hash of the input string
	 */
	public static String getSha1Digest(String input) {
		return getDigest(input, MessageDigestAlgorithms.SHA_1);
	}

	/**
	 * Returns a SHA-512 digest.
	 * 
	 * @param input
	 *            the input string
	 * @return the SHA-512 hash of the input string
	 */
	public static String getSha512Digest(String input) {
		return getDigest(input, MessageDigestAlgorithms.SHA_512);
	}

}
