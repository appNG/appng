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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Principal;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.codec.binary.Hex;
import org.appng.core.security.signing.SigningException.ErrorType;

import lombok.extern.slf4j.Slf4j;

/**
 * Central class for signing and validating repositories.
 * 
 * @author Dirk Heuvels
 * @author Matthias Müller
 */
@Slf4j
public class Signer {

	private ValidatorConfig validatorConfig;

	private Signer(ValidatorConfig config) {
		this.validatorConfig = config;
	}

	public static Signer getRepoValidator(ValidatorConfig config, byte[] indexFileData, byte[] signatureData)
			throws SigningException {
		return getRepoValidator(config, indexFileData, signatureData, null);
	}

	public static Signer getRepoValidator(ValidatorConfig config, byte[] indexFileData, byte[] signatureData,
			Collection<X509Certificate> trustedCerts) throws SigningException {
		X509Certificate signingCert = config.getSigningCert();
		boolean validateChainAndValidity = true;
		Principal signingSubject = signingCert.getSubjectDN();
		if (null != trustedCerts) {
			X509Certificate trustedCert = trustedCerts.iterator().next();
			RSAPublicKey trustedPublicKey = (RSAPublicKey) trustedCert.getPublicKey();
			RSAPublicKey signedPublicKey = (RSAPublicKey) config.getSigningCert().getPublicKey();
			if (signedPublicKey.getModulus().equals(trustedPublicKey.getModulus())
					&& signedPublicKey.getPublicExponent().equals(trustedPublicKey.getPublicExponent())) {
				validateChainAndValidity = false;
			} else {
				Principal trustedSubject = trustedCert.getSubjectDN();
				String message = String.format("the trusted certificate does not match! Expected %s, got %s",
						trustedSubject.getName(), signingSubject.getName());
				throw new SigningException(ErrorType.VERIFY, message, null, signingCert);
			}
		}

		if (validateChainAndValidity) {

			CertChainValidator certChainValidator = config.getCertChainValidator();
			if (null != certChainValidator) {
				boolean isChainValid = certChainValidator
						.validateKeyChain(new ByteArrayInputStream(config.getSigningCertsRaw()));
				if (!isChainValid) {
					throw new SigningException(ErrorType.VERIFY,
							String.format("The chain for certificate '%s' is invalid!", signingSubject.getName()),
							signingCert);
				}
			}

			// Check certificate expiration
			try {
				signingCert.checkValidity();
			} catch (CertificateException ce) {
				throw new SigningException(ErrorType.VERIFY,
						String.format("The certificate '%s' is invalid (expires: %s).", signingSubject.getName(), ce,
								signingCert.getNotAfter()),
						ce, signingCert);
			}
		}
		// Verify the signature
		try {
			LOGGER.info("Validating the release file against signature/certificate '{}'.", signingSubject.getName());
			Signature sig = config.getSignature();
			sig.update(indexFileData);
			if (!sig.verify(signatureData)) {
				throw new SigningException(ErrorType.VERIFY, "Release signature did not validate. Cannot continue.",
						config.getSigningCert());
			}
			LOGGER.info("Successfully validated release file.");
		} catch (SignatureException se) {
			throw new SigningException(ErrorType.VERIFY, "Failed to validate the release signature.", se,
					config.getSigningCert());
		}

		verifyIndex(config, indexFileData);

		String missingKey = config.hasMissingKey();
		if (missingKey != null)
			throw new SigningException(ErrorType.VERIFY,
					String.format("Missing configuration key '%s' in repository.", missingKey),
					config.getSigningCert());

		return new Signer(config);
	}

	protected static void verifyIndex(ValidatorConfig config, byte[] indexFileData) throws SigningException {
		BufferedReader respReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(indexFileData), config.getCharset()));

		boolean reachedDigests = false;
		int lineNo = 0;
		try {
			for (String line; (line = respReader.readLine()) != null; ++lineNo) {
				String[] keyVal = line.split(":");
				if (keyVal.length != 2) {
					if (line.equals(SignerConfig.RELEASE_FILE_DIGEST_SEPARATOR)) {
						reachedDigests = true;
						continue;
					} else {
						throw new SigningException(ErrorType.VERIFY,
								String.format(
										"Release file has unexpected format on line %d. Expected 'key: value', but got '%s'.",
										lineNo, line));
					}
				}

				if (reachedDigests) {
					LOGGER.info("..importing package digest '{}: {}'", keyVal[0].trim(), keyVal[1].trim());
					config.pkgDigests.put(keyVal[0].trim(), keyVal[1].trim());
				} else {
					LOGGER.info("..importing repository attribute '{}: {}'", keyVal[0].trim(), keyVal[1].trim());
					config.repoAttributes.put(keyVal[0].trim(), keyVal[1].trim());
				}
			}
		} catch (IOException e) {
			throw new SigningException(ErrorType.VERIFY, "error while building index", e);
		}
	}

	public boolean validatePackage(byte[] bytes, String packageName) throws SigningException {
		if (!validatorConfig.pkgDigests.containsKey(packageName)) {
			throw new SigningException(ErrorType.VERIFY, String.format("Package '%s' not found.", packageName));
		}
		byte[] hashRaw = validatorConfig.getDigest().digest(bytes);
		String actualDigest = new String(Hex.encodeHex(hashRaw, true));
		String expectedDigest = validatorConfig.pkgDigests.get(packageName);
		if (actualDigest.equals(expectedDigest)) {
			LOGGER.debug("Package {} has the expected digest {}", packageName, expectedDigest);
			return true;
		} else {
			throw new SigningException(ErrorType.VERIFY, String.format("Digests missmatch for {}, expected {}, got {}",
					packageName, expectedDigest, actualDigest));
		}

	}

	public static SignatureWrapper signRepo(Path repoPath, SignerConfig config) throws SigningException {
		String missingKey = config.hasMissingKey();
		if (missingKey != null)
			throw new SigningException(ErrorType.SIGN,
					String.format("Missing configuration key '%s' in SignerConfig.", missingKey));

		LOGGER.info("Signing repository '{}'", repoPath);
		try {
			// Find the packages to sign
			Path[] pkgPaths = fileGlob(repoPath, "*.{jar,zip}");
			// Write the release file
			Path releaseFilePath = repoPath.resolve("index");
			LOGGER.info("Writing release file '{}'", releaseFilePath);
			try (
					BufferedWriter releaseFileOut = Files.newBufferedWriter(releaseFilePath, config.getCharset(),
							StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

				LOGGER.info("..adding repository attributes");
				for (String key : BaseConfig.validRepoAttributes) {
					releaseFileOut.append(String.format("%s: %s\n", key, config.repoAttributes.get(key)));
				}

				releaseFileOut.append(String.format("%s\n", SignerConfig.RELEASE_FILE_DIGEST_SEPARATOR));
				for (Path pkgPath : pkgPaths) {
					LOGGER.info("..adding message digest of package '{}'", pkgPath.getFileName());
					byte[] hashRaw = config.getDigest().digest(Files.readAllBytes(pkgPath));
					releaseFileOut
							.append(String.format("%s: %s\n", pkgPath.getFileName(), Hex.encodeHexString(hashRaw)));
				}
				releaseFileOut.close();

				Signature sig = config.getSignature();
				sig.update(Files.readAllBytes(releaseFilePath));
				byte[] signed = sig.sign();
				SignatureWrapper signatureWrapper = new SignatureWrapper();
				signatureWrapper.setSignature(signed);
				signatureWrapper.setIndex(Files.readAllBytes(releaseFilePath));
				return signatureWrapper;
			}
		} catch (IOException ioe) {
			throw new SigningException(ErrorType.SIGN,
					"IOException during repo signing. Please check the configured paths and masks.", ioe);
		} catch (SignatureException se) {
			throw new SigningException(ErrorType.SIGN,
					String.format(
							"SignatureException during repo signing. There is no plausible reason in this part of the code. You probably found a bug!"),
					se);
		}

	}

	// Is there an easier way to do globbing?
	static Path[] fileGlob(Path path, String pattern) throws IOException {
		final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		final ArrayList<Path> pathBuf = new ArrayList<>(128);
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				Path fileName = path.getFileName();
				if (pathMatcher.matches(fileName))
					pathBuf.add(path);
				return FileVisitResult.CONTINUE;
			}
		});
		Collections.sort(pathBuf);
		return pathBuf.toArray(new Path[pathBuf.size()]);
	}
}
