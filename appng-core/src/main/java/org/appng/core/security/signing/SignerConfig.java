/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.core.security.signing;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.appng.core.security.signing.SigningException.ErrorType;

/**
 * The configuration used for signing a repository
 * 
 * @author Dirk Heuvels
 * @author Matthias Müller
 */
public class SignerConfig extends BaseConfig {

	protected RSAPrivateKey signingKey;
	protected Signature signature;

	// Constructor with reasonable defaults
	public SignerConfig(String repoCodeName, String repoDescription, String repoVersion, byte[] privateKey,
			byte[] signCert, SigningAlgorithm sigAlgorithm, PrivateKeyFormat keyFormat) throws SigningException {
		repoAttributes.put("repoCodeName", repoCodeName);
		repoAttributes.put("repoDescription", repoDescription);
		repoAttributes.put("repoVersion", repoVersion);

		setSignKey(privateKey, sigAlgorithm, keyFormat);
		setSigningCerts(signCert, ErrorType.SIGN);
		setMsgDigest(DigestAlgorithm.SHA256);

		// Make sure our signing key matches the private key in the certificate.
		if (!this.getSigningKey().getModulus().equals(this.getCertPublicKey().getModulus()))
			throw new SigningException(ErrorType.SIGN,
					"Signing key and cerfiticate were successfully loaded, but the key does not match the certificate. You may want to verify that the keys have the same modulus using the '-modulus' switch of openssl.");
	}

	// --------------------

	protected String hasMissingKey() {
		String missing;
		if ((missing = super.hasMissingKey()) != null)
			return missing;
		if (signingKey == null)
			return "signingKey";
		if (signature == null)
			return "signature";
		return null;
	}

	// signingKey + signature
	private void setSignKey(byte[] privateKey, SigningAlgorithm sigAlgorithm, PrivateKeyFormat keyFormat)
			throws SigningException {
		// Load the signing key (PKCS8 has proven to be the easiest way, when not using extra libraries like bouncy
		// castle).
		PKCS8EncodedKeySpec signingKeyP8;
		try {
			KeyFactory keyfactory = KeyFactory.getInstance("RSA");
			switch (keyFormat) {
			case PEM:
				String pkcs8KeyPem = new String(privateKey);
				// Keys in PEM format (at least when generated by OpenSSL) have a header and trailer, we must remove.
				String pkcs8KeyData = pkcs8KeyPem.replaceAll("(\\r)?(\\n)?-----(.*)(\\r)?\\n", "");
				byte[] parseBase64Binary = Base64.decodeBase64(pkcs8KeyData);
				signingKeyP8 = new PKCS8EncodedKeySpec(parseBase64Binary);
				this.signingKey = (RSAPrivateKey) keyfactory.generatePrivate(signingKeyP8);
				break;
			case DER:
				// In DER format the key can be loaded instantaneously.
				signingKeyP8 = new PKCS8EncodedKeySpec(privateKey);
				this.signingKey = (RSAPrivateKey) keyfactory.generatePrivate(signingKeyP8);
				break;
			}
		} catch (NoSuchAlgorithmException nsae) {
			throw new SigningException(ErrorType.SIGN, String.format(
					"Got NoSuchAlgorithmException while loading private key as 'RSA' key. This should not happen with one of the tested Java versions (1.7+)."),
					nsae);
		} catch (InvalidKeySpecException ikse) {
			if (keyFormat == PrivateKeyFormat.DER) {
				throw new SigningException(ErrorType.SIGN, String.format(
						"Error while loading private key. You may want to check if the key is valid with OpenSSL: 'openssl pkcs8 -in %s -inform DER -nocrypt'",
						"<cert>.der"), ikse);
			} else {
				throw new SigningException(ErrorType.SIGN, String.format(
						"Error while loading private key. You may want to check if the key is valid with OpenSSL: 'openssl rsa -in %s -inform PEM -text'",
						"<cert>.pem"), ikse);
			}
		}

		// Get a Signature instance with our signing key
		try {
			this.signature = Signature.getInstance(sigAlgorithm.toString());
			this.signature.initSign(this.signingKey);
		} catch (InvalidKeyException ike) {
			throw new SigningException(ErrorType.SIGN, String.format(
					"Private key was successfully loaded, but failed to instantiate at Signature(%s).initSign().",
					sigAlgorithm), ike);
		} catch (NoSuchAlgorithmException nsae) {
			throw new SigningException(ErrorType.SIGN, String.format(
					"Signing algorithm '%s' could not be loaded, but it should. This should not happen with one of the tested Java versions (1.7+).",
					sigAlgorithm), nsae);
		}
	}

	private RSAPrivateKey getSigningKey() {
		return this.signingKey;
	}

	Signature getSignature() {
		return this.signature;
	}

}
