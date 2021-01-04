/*
 * Copyright 2011-2021 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.appng.core.security.signing.SigningException.ErrorType;

/**
 * The configuration used for validating a repository
 * 
 * @author Dirk Heuvels
 * @author Matthias Müller
 */
public class ValidatorConfig extends BaseConfig {

	public ValidatorConfig() throws SigningException {
		setMsgDigest(DigestAlgorithm.SHA256);
	}

	protected Map<String, String> pkgDigests = new HashMap<>();

	protected Signature signature;
	private byte[] signingCertsRaw;
	private byte[] trustStore;
	private char[] trustStorePassword;

	public void setSigningCert(byte[] signCert, SigningAlgorithm sigAlgorithm) throws SigningException {
		this.signingCertsRaw = ArrayUtils.clone(signCert);
		setSigningCerts(signingCertsRaw, ErrorType.VERIFY);
		try {
			// Get a Signature instance with the repos certificate
			this.signature = Signature.getInstance(sigAlgorithm.toString());
			this.signature.initVerify(getSigningCert());
		} catch (InvalidKeyException ike) {
			throw new SigningException(ErrorType.VERIFY,
					String.format(
							"Certificate key was successfully loaded, but failed to instantiate at Signature(%s).initVerify().",
							sigAlgorithm),
					ike);
		} catch (NoSuchAlgorithmException nsae) {
			throw new SigningException(ErrorType.VERIFY,
					String.format(
							"Signing algorithm '%s' could not be loaded, but it should. This should not happen with one of the tested Java versions (1.7+).",
							sigAlgorithm),
					nsae);
		}
	}

	Signature getSignature() {
		return this.signature;
	}

	public void setTrustStore(byte[] trustStore) {
		this.trustStore = trustStore;
	}

	public void setTrustStorePassword(char[] trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	byte[] getSigningCertsRaw() {
		return signingCertsRaw;
	}

	public void setupDefaultTruststore() throws SigningException {
		File dir = new File(
				System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security");
		try {
			this.trustStore = FileUtils.readFileToByteArray(new File(dir, "cacerts"));
			this.trustStorePassword = "changeit".toCharArray();
		} catch (IOException e) {
			throw new SigningException(ErrorType.VERIFY,
					String.format("error reading cacerts from %s", dir.getAbsolutePath()), e);
		}

	}

	CertChainValidator getCertChainValidator() throws SigningException {
		if (null == trustStore && null == trustStorePassword) {
			return null;
		}
		return new CertChainValidator(new ByteArrayInputStream(trustStore), trustStorePassword);
	}

}