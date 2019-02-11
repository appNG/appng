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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.appng.core.security.signing.BaseConfig.PrivateKeyFormat;
import org.appng.core.security.signing.BaseConfig.SigningAlgorithm;
import org.junit.Assert;
import org.junit.Test;

public class SignerTest {

	File certFile = getFile("cert/appng-dev.cert");
	File indexFile = getFile("zip/index-test.txt");
	File signatureFile = getFile("zip/index.sig");

	@Test
	public void testSign() throws IOException {
		byte[] cert = FileUtils.readFileToByteArray(certFile);

		byte[] key = FileUtils.readFileToByteArray(getFile("cert/appng-dev.pem"));

		Path repoPath = getFile("zip").toPath();
		SignatureWrapper signRepo = Signer.signRepo(repoPath, new SignerConfig("Local", "a local test repository", "",
				key, cert, SigningAlgorithm.SHA512withRSA, PrivateKeyFormat.PEM));

		StringWriter output = new StringWriter();
		IOUtils.write(signRepo.getIndex(), output, StandardCharsets.UTF_8);

		String expected = FileUtils.readFileToString(indexFile, StandardCharsets.UTF_8);
		Assert.assertEquals(expected, output.toString());

		byte[] expectedSig = FileUtils.readFileToByteArray(signatureFile);
		Assert.assertArrayEquals(expectedSig, signRepo.getSignature());
	}

	@Test
	public void testVerfiy() throws IOException {
		ValidatorConfig config = new ValidatorConfig();
		config.setSigningCert(FileUtils.readFileToByteArray(certFile), SigningAlgorithm.SHA512withRSA);
		Signer repoValidator = Signer.getRepoValidator(config, FileUtils.readFileToByteArray(indexFile),
				FileUtils.readFileToByteArray(signatureFile));

		File packageFile = getFile("zip/demo-application-1.5.3-2013-01-13-1303.zip");
		File packageFile2 = getFile("zip/demo-application-1.5.2-2012-11-27-1305.zip");

		boolean isValid = repoValidator.validatePackage(FileUtils.readFileToByteArray(packageFile),
				packageFile.getName());
		Assert.assertTrue("package must be valid", isValid);
		try {
			repoValidator.validatePackage(FileUtils.readFileToByteArray(packageFile2), packageFile.getName());
		} catch (IOException e) {
			return;
		}
		Assert.fail("package must be invalid");
	}

	@Test(expected = SigningException.class)
	public void testVerfiyInvalidCert() throws IOException {
		ValidatorConfig config = new ValidatorConfig();
		config.setSigningCert(FileUtils.readFileToByteArray(signatureFile), SigningAlgorithm.SHA512withRSA);
	}

	@Test(expected = SigningException.class)
	public void testVerfiyInvalidSignature() throws IOException {
		ValidatorConfig config = new ValidatorConfig();
		config.setSigningCert(FileUtils.readFileToByteArray(certFile), SigningAlgorithm.SHA512withRSA);
		Signer.getRepoValidator(config, FileUtils.readFileToByteArray(indexFile),
				FileUtils.readFileToByteArray(indexFile));
	}

	File getFile(String name) {
		try {
			URI uri = getClass().getClassLoader().getResource(name).toURI();
			return new File(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
