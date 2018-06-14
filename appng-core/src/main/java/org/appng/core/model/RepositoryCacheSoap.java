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
package org.appng.core.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.SoapClient;
import org.appng.core.domain.PackageArchiveImpl;
import org.appng.core.security.signing.BaseConfig.SigningAlgorithm;
import org.appng.core.security.signing.SignatureWrapper;
import org.appng.core.security.signing.Signer;
import org.appng.core.security.signing.SigningException;
import org.appng.core.security.signing.ValidatorConfig;
import org.appng.core.xml.repository.Certification;
import org.appng.core.xml.repository.GetCertificationRequest;
import org.appng.core.xml.repository.GetCertificationResponse;
import org.appng.core.xml.repository.GetPackageRequest;
import org.appng.core.xml.repository.GetPackageResponse;
import org.appng.core.xml.repository.GetPackageVersionsRequest;
import org.appng.core.xml.repository.GetPackageVersionsResponse;
import org.appng.core.xml.repository.GetPackagesRequest;
import org.appng.core.xml.repository.GetPackagesResponse;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.core.xml.repository.Packages;
import org.appng.xml.application.PackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.WebServiceClientException;

/**
 * Implementation of {@link RepositoryCache} that retrieves the packages from a remote repository.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class RepositoryCacheSoap extends RepositoryCacheBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheSoap.class);
	private String url;
	private byte[] trustStore;
	private char[] storePass;
	private boolean validateCertChain;
	private int connectTimeout = 0;
	private int readTimeout = 0;

	RepositoryCacheSoap(Repository repository, int connectTimeout, int readTimeout, byte[] trustStore, char[] storePass,
			boolean validateCertChain) throws BusinessException {
		super(repository, null, true);
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.trustStore = trustStore;
		this.storePass = storePass;
		this.validateCertChain = validateCertChain;
	}

	void init() throws BusinessException {
		if (StringUtils.isBlank(repository.getRemoteRepositoryName())) {
			throw new BusinessException("The name of the remote repository must not be empty!");
		}
		try {
			url = repository.getUri().toURL().toExternalForm();
		} catch (MalformedURLException mue) {
			throw new BusinessException("invalid repository URL", mue);
		}
		update();
	}

	void update() throws BusinessException {
		List<org.appng.core.xml.repository.Package> publishedPackages = getPublishedApplications();
		for (org.appng.core.xml.repository.Package pkg : publishedPackages) {
			String packageName = pkg.getName();
			LOGGER.debug("Retrieving archive from remote repository \"{}\": {}", repository.getRemoteRepositoryName(),
					packageName);
			applicationWrapperMap.put(packageName, new PackageWrapper(pkg));
		}
	}

	void update(String packageName) throws BusinessException {
		List<PackageInfo> packages = getPublishedApplicationVersions(packageName);
		for (PackageInfo packageInfo : packages) {
			applicationWrapperMap.get(packageName).put(packageInfo);
		}
	}

	private List<org.appng.core.xml.repository.Package> getPublishedApplications() throws BusinessException {
		GetPackagesRequest request = new GetPackagesRequest();
		request.setRepositoryName(repository.getRemoteRepositoryName());
		request.setDigest(repository.getDigest());
		GetPackagesResponse response = sendRequest(request);
		Packages publishedPackages = response.getPackages();
		Certification certification = publishedPackages.getCertification();
		if (null != certification) {
			Decoder decoder = Base64.getDecoder();
			this.signatureWrapper = new SignatureWrapper();
			signatureWrapper.setCert(decoder.decode(certification.getCert()));
			signatureWrapper.setIndex(decoder.decode(certification.getIndex()));
			signatureWrapper.setSignature(decoder.decode(certification.getSignature()));
			try {
				ValidatorConfig validatorConfig = getValidatorConfig();
				validatorConfig.setSigningCert(signatureWrapper.getCert(), SigningAlgorithm.SHA512withRSA);
				Signer.getRepoValidator(validatorConfig, signatureWrapper.getIndex(), signatureWrapper.getSignature(),
						repository.getTrustedCertChain());
			} catch (SigningException e) {
				BusinessException businessException = new BusinessException(
						String.format("error while validating repository %s", repository.getName()), e,
						"repository.error." + e.getType().name(), repository.getName());
				LOGGER.error("validating repository failed", businessException);
				throw businessException;
			}
		}
		return publishedPackages.getPackage();
	}

	private List<PackageInfo> getPublishedApplicationVersions(String packageName) throws BusinessException {
		GetPackageVersionsRequest request = new GetPackageVersionsRequest();
		request.setRepositoryName(repository.getRemoteRepositoryName());
		request.setDigest(repository.getDigest());
		request.setPackageName(packageName);
		GetPackageVersionsResponse response = sendRequest(request);
		PackageVersions packageVersions = response.getPackageVersions();
		return packageVersions.getPackage();
	}

	@SuppressWarnings("unchecked")
	private <T> T sendRequest(Object request) throws BusinessException {
		try {
			SoapClient soapClient = new SoapClient(RepositoryUtils.getContextPath(), url);
			soapClient.setConnectTimeout(connectTimeout);
			soapClient.setReadTimeout(readTimeout);
			return (T) soapClient.send(request);
		} catch (WebServiceClientException e) {
			throw new BusinessException(e);
		}
	}

	public Certification getCertification() throws BusinessException {
		GetCertificationRequest request = new GetCertificationRequest();
		request.setRepositoryName(repository.getRemoteRepositoryName());
		request.setDigest(repository.getDigest());
		GetCertificationResponse response = sendRequest(request);
		return response.getCertification();
	}

	public PackageArchive getApplicationArchive(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException {
		GetPackageRequest request = new GetPackageRequest();
		request.setRepositoryName(repository.getRemoteRepositoryName());
		request.setDigest(repository.getDigest());
		request.setPackageName(packageName);
		request.setPackageVersion(packageVersion);
		request.setPackageTimestamp(packageTimestamp);
		GetPackageResponse response = sendRequest(request);
		byte[] bytes = response.getData();

		File folder = new File(System.getProperty("java.io.tmpdir"), "appNG");
		if (!folder.exists()) {
			folder.mkdir();
		}
		String archiveName = response.getFileName();
		File archiveFile = new File(folder, archiveName);
		LOGGER.debug("Writing temporary archive zip file: {}", archiveFile.getAbsolutePath());

		try (FileOutputStream fos = new FileOutputStream(archiveFile)) {
			fos.write(bytes);

			if (null != archiveFile && archiveFile.canRead()) {
				PackageArchive archive = new PackageArchiveImpl(archiveFile, repository.isStrict());
				String checksum = response.getChecksum();
				if (StringUtils.isNotBlank(checksum) && !archive.getChecksum().equals(checksum)) {
					throw new BusinessException(String.format("checksum missmatch for %s, expected %s, got %s!",
							archive, checksum, archive.getChecksum()));
				}
				if (null != signatureWrapper) {
					try {
						ValidatorConfig config = getValidatorConfig();
						config.setSigningCert(this.signatureWrapper.getCert(), SigningAlgorithm.SHA512withRSA);
						Signer repoValidator = Signer.getRepoValidator(config, signatureWrapper.getIndex(),
								signatureWrapper.getSignature(), repository.getTrustedCertChain());
						repoValidator.validatePackage(bytes, archiveName);
					} catch (SigningException e) {
						throw new BusinessException(String.format(
								"error validating repository signature for repository %s", repository.getName()), e);
					}
				}
				if (archive.isValid()) {
					return archive;
				} else {
					throw new BusinessException("Invalid archive: " + archiveFile.getAbsolutePath());
				}
			} else {
				throw new BusinessException(
						"No archive file oder archive file is not readable: " + archiveFile.getAbsolutePath());
			}
		} catch (IOException o) {
			throw new BusinessException("error while processing " + archiveFile.getAbsolutePath(), o);
		}
	}

	private ValidatorConfig getValidatorConfig() throws SigningException {
		ValidatorConfig config = new ValidatorConfig();
		if (validateCertChain) {
			if (null != trustStore && null != storePass) {
				config.setTrustStore(trustStore);
				config.setTrustStorePassword(storePass);
			} else {
				config.setupDefaultTruststore();
			}
		}
		return config;
	}

	public void deleteApplicationVersion(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException {
		throw new BusinessException("Deleting application versions is not supported for remote repositories!");
	}

}
