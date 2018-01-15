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
package org.appng.core.security.signing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.appng.core.security.signing.SigningException.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates a certificate chain against a truststore.
 * 
 * @author Matthias Müller
 *
 */
public class CertChainValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(CertChainValidator.class);
	private static final String DEFAULT_PASS = "changeit";

	private List<X509Certificate> trustedCerts;

	CertChainValidator(InputStream is, char[] storepass) throws SigningException {
		init(is, storepass);
	}

	/**
	 * Creates a new validator using the default truststore located at {@code $java.home/lib/security/cacerts}
	 * 
	 * @throws SigningException
	 *             if an error occurred while reading from the truststore
	 * @throws FileNotFoundException
	 *             if the truststore does not exist
	 */
	CertChainValidator() throws SigningException, FileNotFoundException {
		File dir = new File(
				System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security");
		File trusted = new File(dir, "cacerts");
		LOGGER.info("using truststore {}", trusted.getAbsolutePath());
		init(new FileInputStream(trusted), DEFAULT_PASS.toCharArray());
	}

	CertChainValidator(KeyStore keyStore) throws SigningException {
		try {
			init(keyStore);
		} catch (KeyStoreException e) {
			throw new SigningException(ErrorType.VERIFY, "error while loading keystore", e);
		}
	}

	protected void init(InputStream is, char[] storepass) throws SigningException {
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(is, storepass);
			init(keyStore);
		} catch (GeneralSecurityException | IOException e) {
			throw new SigningException(ErrorType.VERIFY, "error while loading keystore", e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	protected void init(KeyStore keyStore) throws KeyStoreException {
		trustedCerts = new ArrayList<>(keyStore.size());
		Enumeration<String> alias = keyStore.aliases();
		while (alias.hasMoreElements()) {
			trustedCerts.add((X509Certificate) keyStore.getCertificate(alias.nextElement()));
		}
		LOGGER.debug("found {} certificates in truststore", trustedCerts.size());
	}

	boolean validateKeyChain(InputStream clientCert) {
		try {
			List<X509Certificate> certificates = new ArrayList<>();
			CertTools.addCerts(clientCert, certificates);

			int i = 0;
			while (i < certificates.size() - 1) {
				X509Certificate cert = certificates.get(i);
				X509Certificate issuser = certificates.get(i + 1);
				if (!cert.getIssuerX500Principal().equals(issuser.getSubjectX500Principal())) {
					LOGGER.error("'{}' should be signed by '{}', but is signed by '{}'", cert.getSubjectX500Principal(),
							issuser.getSubjectX500Principal(), cert.getIssuerX500Principal());
					return false;
				}
				i++;
			}

			for (Certificate c : certificates) {
				X509Certificate currentCert = (X509Certificate) c;
				if (validateKeyChain(currentCert, trustedCerts)) {
					return true;
				} else {
					LOGGER.info("'{}' is not trusted, trying with issuer '{}'", currentCert.getSubjectX500Principal(),
							currentCert.getIssuerX500Principal());
				}
			}
			LOGGER.info("can not trust {}", ((X509Certificate) certificates.get(0)).getSubjectX500Principal());
		} catch (GeneralSecurityException e) {
			LOGGER.warn("error while validating keychain", e);
		}
		return false;
	}

	private boolean validateKeyChain(X509Certificate client, List<X509Certificate> trustedCerts)
			throws GeneralSecurityException {
		LOGGER.debug("validating '{}' against truststore", client.getSubjectX500Principal());

		CertificateFactory cf = CertTools.getX509CertFactory();
		CertPathValidator validator = CertPathValidator.getInstance("PKIX");

		boolean found = false;
		int i = trustedCerts.size();
		while (!found && i > 0) {
			Set<TrustAnchor> anchors = Collections.singleton(new TrustAnchor(trustedCerts.get(--i), null));
			CertPath path = cf.generateCertPath(Collections.singletonList(client));

			PKIXParameters params = new PKIXParameters(anchors);
			params.setRevocationEnabled(false);

			X509Certificate trusted = trustedCerts.get(i);

			if (client.getIssuerDN().equals(trusted.getSubjectDN())) {
				try {
					validator.validate(path, params);
					if (isSelfSigned(trusted)) {
						found = true;
						LOGGER.debug("'{}' can be trusted (expires: {})", trusted.getSubjectX500Principal(),
								trusted.getNotAfter());
					} else if (!client.equals(trusted)) {
						LOGGER.debug("validating '{}' via '{}'", client.getSubjectX500Principal(),
								trusted.getSubjectX500Principal());
						found = validateKeyChain(trusted, trustedCerts);
					}
				} catch (CertPathValidatorException e) {
					LOGGER.warn("error while validating certification path", e);
				}
			}
		}
		return found;
	}

	private boolean isSelfSigned(X509Certificate cert) {
		try {
			cert.verify(cert.getPublicKey());
			return true;
		} catch (GeneralSecurityException sigEx) {
			return false;
		}
	}

}