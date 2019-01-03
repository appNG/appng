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
package org.appng.core.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.model.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StopWatch;

/**
 * Returns instances of {@link RepositoryCache}.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 * 
 */
public class RepositoryCacheFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheFactory.class);
	private static Map<Integer, RepositoryCache> caches = new HashMap<Integer, RepositoryCache>();
	private byte[] cert;
	private byte[] privateKey;
	private byte[] trustStore;
	private char[] storePass;
	private boolean validateCertChain;
	private static RepositoryCacheFactory INSTANCE;
	private int connectTimeout = 0;
	private int readTimeout = 0;

	/**
	 * Initializes the factory.
	 * 
	 * @param cert
	 *            the certificate to use when verifying a signed remote repository (PEM format)
	 * @param privateKey
	 *            the private key to use when signing a local published repository (PEM format)
	 * @param trustStore
	 *            the truststore used when verifying a signed remote repository (to build a {@link java.security.KeyStore} from)
	 * @param storePass
	 *            the truststore's password
	 * @param validateCertChain
	 *            whether or not signed remote repositories are validated against the configured truststore
	 * @return the singleton instance of {@link RepositoryCacheFactory}
	 */
	public static RepositoryCacheFactory init(String cert, String privateKey, byte[] trustStore, String storePass,
			boolean validateCertChain) {
		RepositoryCacheFactory.INSTANCE = new RepositoryCacheFactory();
		if (StringUtils.isNoneBlank(cert, privateKey)) {
			INSTANCE.privateKey = privateKey.getBytes().clone();
			INSTANCE.cert = cert.getBytes().clone();
			if (null != trustStore) {
				INSTANCE.trustStore = trustStore;
			}
			if (null != storePass) {
				INSTANCE.storePass = storePass.toCharArray();
			}
			INSTANCE.validateCertChain = validateCertChain;
		}
		return INSTANCE;
	}

	/**
	 * Initializes the factory with properties from the platform's configuration
	 * 
	 * @param platformConfig
	 *            the platform's configuration
	 * @return the singleton instance of {@link RepositoryCacheFactory}
	 * 
	 * @see RepositoryCacheFactory#init
	 * 
	 * @see Platform.Property#REPOSITORY_CERT
	 * @see Platform.Property#REPOSITORY_SIGNATURE
	 * @see Platform.Property#REPOSITORY_TRUSTSTORE
	 * @see Platform.Property#REPOSITORY_TRUST_STORE_PASSWORD
	 * @see Platform.Property#REPOSITORY_VERIFY_SIGNATURE
	 */
	public static RepositoryCacheFactory init(Properties platformConfig) {
		String repoCert = platformConfig.getClob(Platform.Property.REPOSITORY_CERT);
		String repoSignature = platformConfig.getClob(Platform.Property.REPOSITORY_SIGNATURE);
		String repoTrustStorePath = platformConfig.getString(Platform.Property.REPOSITORY_TRUSTSTORE);
		String repoTrustStorePassword = platformConfig.getString(Platform.Property.REPOSITORY_TRUST_STORE_PASSWORD);
		boolean repoVerify = platformConfig.getBoolean(Platform.Property.REPOSITORY_VERIFY_SIGNATURE);
		byte[] repoTrustStore = null;
		if (StringUtils.isNotBlank(repoTrustStorePath)) {
			try {
				File trustStore = ResourceUtils.getFile(repoTrustStorePath);
				if (trustStore.exists()) {
					repoTrustStore = FileUtils.readFileToByteArray(trustStore);
				} else {
					LOGGER.error("configured truststore {} does not exist!", repoTrustStorePath);
				}
			} catch (IOException e) {
				LOGGER.error(String.format("error while loading truststore from %s", repoTrustStorePath), e);
			}
		}
		return init(repoCert, repoSignature, repoTrustStore, repoTrustStorePassword, repoVerify);
	}

	public static synchronized RepositoryCacheFactory instance() {
		return INSTANCE;
	}

	/**
	 * Returns a {@link RepositoryCache} for the given {@link Repository}
	 * 
	 * @param repository
	 *            the {@link Repository}
	 * @return the {@link RepositoryCache}
	 * @throws BusinessException
	 *             if an error occurred while retrieving the {@link RepositoryCache}
	 */
	public RepositoryCache getCache(Repository repository) throws BusinessException {
		StopWatch stopWatch = new StopWatch("ApplicationRepositoryCache");
		stopWatch.start();
		Integer id = repository.getId();
		RepositoryCache cacheInstance = null;
		if (caches.containsKey(id)) {
			cacheInstance = caches.get(id);
			Repository repo = ((RepositoryCacheBase) cacheInstance).getRepository();
			if (!repository.getUri().equals(repo.getUri()) || repository.getVersion().after(repo.getVersion())) {
				caches.remove(id);
			}
		}

		if (!caches.containsKey(id)) {
			cacheInstance = getRealInstance(repository);
			caches.put(id, cacheInstance);
		}
		stopWatch.stop();
		LOGGER.debug(stopWatch.shortSummary());
		return cacheInstance;
	}

	private RepositoryCache getRealInstance(Repository repository) throws BusinessException {
		RepositoryCache cacheInstance = null;
		validateRepositoryURI(repository);
		RepositoryType repositoryType = repository.getRepositoryType();
		switch (repositoryType) {
		case LOCAL:
			cacheInstance = new RepositoryCacheFilesystem(repository, cert, privateKey);
			break;
		case REMOTE:
			cacheInstance = new RepositoryCacheSoap(repository, connectTimeout, readTimeout, trustStore, storePass,
					validateCertChain);
			break;
		}
		return cacheInstance;
	}

	/**
	 * Checks whether the {@link Repository} has a valid {@link URI}.
	 * 
	 * @param repository
	 *            the {@link Repository} to check
	 * @throws BusinessException
	 *             if the {@link URI} defined by the {@link Repository} is not valid
	 * @see RepositoryScheme
	 * @see Repository#getUri()
	 */
	public static void validateRepositoryURI(Repository repository) throws BusinessException {
		try {
			URI uri = repository.getUri();
			RepositoryScheme scheme = RepositoryScheme.valueOf(uri.getScheme().toUpperCase());
			switch (scheme) {
			case FILE:
				File directory = new File(uri);
				if (!directory.isDirectory()) {
					String errorMessage = "The repository scheme is \"" + scheme
							+ "\", but does not point to a directory: " + directory.getAbsolutePath();
					throw new BusinessException(errorMessage);
				}
				break;

			default:
				break;
			}
		} catch (IllegalArgumentException iae) {
			throw new BusinessException("Error while validating URI. " + getSchemeInfo(repository), iae);
		} catch (NullPointerException npe) {
			throw new BusinessException(
					"Invalid URI. URI must conform to the pattern [scheme:][//authority][path][?query][#fragment] ; "
							+ getSchemeInfo(repository),
					npe);
		}
	}

	private static String getSchemeInfo(Repository repository) {
		RepositoryType type = repository.getRepositoryType();
		Set<RepositoryScheme> schemes = RepositoryScheme.getSchemes(type);
		return "Supported schemes for repositories of type \"" + type + "\": " + schemes;
	}

	private RepositoryCacheFactory() {
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

}
