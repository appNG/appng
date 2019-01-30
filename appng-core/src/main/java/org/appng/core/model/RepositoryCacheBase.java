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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.core.security.signing.SignatureWrapper;
import org.appng.xml.application.PackageInfo;

/**
 * Base-class for {@link RepositoryCache} implementations
 * 
 * @author Matthias Herlitzius
 * 
 */
abstract class RepositoryCacheBase implements RepositoryCache {

	protected final Map<String, PackageWrapper> applicationWrapperMap = new HashMap<String, PackageWrapper>();
	protected final Repository repository;
	protected byte[] cert;
	protected SignatureWrapper signatureWrapper;

	protected RepositoryCacheBase(Repository repository, byte[] cert, boolean reload) throws BusinessException {
		this.repository = repository;
		if (null != cert) {
			this.cert = ArrayUtils.clone(cert);
		}
		if (reload) {
			reload();
		}
	}

	public void reload() throws BusinessException {
		applicationWrapperMap.clear();
		init();
	}

	/**
	 * Deletes the cache before indexing the repository.
	 * 
	 * @throws BusinessException
	 */
	abstract void init() throws BusinessException;

	/**
	 * Updates the repository incrementally by detecting the changes.
	 * 
	 * @throws BusinessException
	 */
	abstract void update() throws BusinessException;

	/**
	 * Updates only the mentioned application.
	 * 
	 * @throws BusinessException
	 */
	abstract void update(String packageName) throws BusinessException;

	public List<PackageWrapper> getApplications() throws BusinessException {
		return getApplications(null);
	}

	public List<PackageWrapper> getApplications(String packageName) throws BusinessException {
		update();
		List<PackageWrapper> applications = new ArrayList<>(applicationWrapperMap.values());
		if (StringUtils.isNotBlank(packageName)) {
			Predicate<? super PackageWrapper> filter;
			String wildcard = "*";
			if (packageName.contains(wildcard)) {
				filter = p -> p.getName().matches(packageName.replace(wildcard, ".*?"));
			} else {
				filter = p -> p.getName().startsWith(packageName);
			}
			applications = applications.parallelStream().filter(filter).collect(Collectors.toList());
		}
		Collections.sort(applications);
		return applications;
	}

	public List<PackageInfo> getVersions(String name) throws BusinessException {
		update(name);
		PackageWrapper publishedApplication = getPublishedApplicationWrapper(name);
		if (null == publishedApplication) {
			throw new BusinessException("application not found: " + name);
		}
		List<PackageInfo> versions = new ArrayList<>(publishedApplication.getVersions().values());
		Collections.sort(versions, new Comparator<PackageInfo>() {
			public int compare(PackageInfo applicationA, PackageInfo applicationB) {
				return applicationA.getName().compareTo(applicationB.getName());
			}
		});
		return versions;
	}

	public PackageWrapper getPublishedApplicationWrapper(String name) {
		return applicationWrapperMap.get(name);
	}

	PackageInfo getSnapshot(String name) {
		return getPublishedApplication(name).getSnapshot();
	}

	PackageInfo getSnapshot(String name, String version) {
		return getPublishedApplication(name).getSnapshot(version);
	}

	PackageInfo getSnapshot(String name, String version, String timestamp) {
		return getPublishedApplication(name).getVersion(version, timestamp);
	}

	PackageInfo getRelease(String name) {
		return getPublishedApplication(name).getRelease();
	}

	PackageInfo getRelease(String name, String version) throws BusinessException {
		PackageWrapper publishedApplication = getPublishedApplicationWrapper(name);
		if (null == publishedApplication) {
			throw new BusinessException("application not found: " + name + " " + version);
		}
		return publishedApplication.getVersion(version, null);
	}

	private PackageWrapper getPublishedApplication(String name) {
		PackageWrapper publishedApplication = getPublishedApplicationWrapper(name);
		if (null == publishedApplication) {
			throw new IllegalArgumentException("application not found: " + name);
		}
		return publishedApplication;
	}

	static String getApplicationVersionSignature(String packageName, String packageVersion, String packageTimestamp) {
		return packageName + ", Version: " + packageVersion + ", Timestamp: " + packageTimestamp;
	}

	protected Repository getRepository() {
		return repository;
	}

}
