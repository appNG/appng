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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/***
 * Utility class for getting {@link X509Certificate}s
 * 
 * @author Matthias Müller
 */
public class CertTools {

	private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
	private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

	public static X509Certificate getCert(byte[] data) throws CertificateException {
		return (X509Certificate) getX509CertFactory().generateCertificate(new ByteArrayInputStream(data));
	}

	public static Collection<X509Certificate> getCerts(byte[] data) throws CertificateException {
		return addCerts(data, new ArrayList<X509Certificate>());
	}

	public static Collection<X509Certificate> addCerts(byte[] data, Collection<X509Certificate> certs)
			throws CertificateException {
		return addCerts(new ByteArrayInputStream(data), certs);
	}

	public static Collection<X509Certificate> addCerts(InputStream data, Collection<X509Certificate> certs)
			throws CertificateException {
		Collection<? extends Certificate> certificates = getX509CertFactory().generateCertificates(data);
		certificates.forEach(c -> certs.add((X509Certificate) c));
		return Collections.unmodifiableCollection(certs);
	}

	public static CertificateFactory getX509CertFactory() throws CertificateException {
		return CertificateFactory.getInstance("X.509");
	}

	public static byte[] writeCerts(Collection<X509Certificate> certs)
			throws IOException, CertificateEncodingException {
		try (StringWriter out = new StringWriter()) {
			for (X509Certificate c : certs) {
				out.write(BEGIN_CERTIFICATE);
				out.write(StringUtils.LF);
				out.write(Base64.encodeBase64String(c.getEncoded()));
				out.write(StringUtils.LF);
				out.write(END_CERTIFICATE);
				out.write(StringUtils.LF);
			}
			out.flush();
			return out.toString().getBytes();
		}
	}

}
