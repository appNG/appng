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
package org.appng.core.security.signing;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.appng.core.security.signing.SigningException.ErrorType;

/**
 * Base configuration: Things needed for signing and verifying likewise.
 * 
 * @author Dirk Heuvels
 * @author Matthias Müller
 */
public abstract class BaseConfig {

	static final String RELEASE_FILE_DIGEST_SEPARATOR = "[package digests]";

	public enum PrivateKeyFormat {
		PEM, DER
	}

	enum DigestAlgorithm {
		SHA256, SHA512
	}

	public enum SigningAlgorithm {
		SHA256withRSA, SHA512withRSA
	}

	static final LinkedHashSet<String> validRepoAttributes = new LinkedHashSet<String>();

	static {
		validRepoAttributes.add("repoCodeName");
		validRepoAttributes.add("repoDescription");
		validRepoAttributes.add("repoVersion");
	}

	protected HashMap<String, String> repoAttributes = new HashMap<String, String>();
	protected Collection<X509Certificate> signingCertChain;
	protected MessageDigest digest;
	protected Charset charset = Charset.forName(StandardCharsets.UTF_8.name());

	protected String hasMissingKey() {
		for (String requiredKey : validRepoAttributes) {
			if (!repoAttributes.containsKey(requiredKey))
				return requiredKey;
		}
		if (signingCertChain == null)
			return "signingCert";
		if (digest == null)
			return "digest";
		if (charset == null)
			return "charset";
		return null;
	}

	protected void setSigningCerts(byte[] cert, ErrorType errorType) throws SigningException {
		try {
			signingCertChain = CertTools.addCerts(cert,  new ArrayList<>());
		} catch (CertificateException ce) {
			throw new SigningException(errorType,
					String.format(
							"Error while loading signing certificate. You may want to check it with OpenSSL: 'openssl x509 -in %s -text -noout'",
							"<cert>.pem"),
					ce);
		}
	}

	X509Certificate getSigningCert() {
		return this.signingCertChain.iterator().next();
	}

	protected RSAPublicKey getCertPublicKey() {
		return (RSAPublicKey) getSigningCert().getPublicKey();
	}

	protected void setMsgDigest(DigestAlgorithm msgDigest) {
		switch (msgDigest) {
		case SHA256:
			this.digest = DigestUtils.getSha256Digest();
			break;
		case SHA512:
			this.digest = DigestUtils.getSha512Digest();
			break;
		}
	}

	MessageDigest getDigest() {
		return this.digest;
	}

	Charset getCharset() {
		return this.charset;
	}

}
