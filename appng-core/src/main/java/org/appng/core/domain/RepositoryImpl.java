/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.core.domain;

import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.appng.api.BusinessException;
import org.appng.api.ValidationMessages;
import org.appng.api.model.Identifier;
import org.appng.core.model.InstallablePackage;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.PackageVersion;
import org.appng.core.model.PackageWrapper;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryCache;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.core.model.RepositoryUtils;
import org.appng.core.security.signing.CertTools;
import org.appng.core.security.signing.SigningException;
import org.appng.core.security.signing.SigningException.ErrorType;
import org.appng.core.xml.repository.Certification;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.core.xml.repository.Packages;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.PackageInfo;

/**
 * {@link Repository} implementation.
 * 
 * @author Matthias Herlitzius
 * 
 */
@Entity
@Table(name = "repository")
public class RepositoryImpl implements Repository {

	private static final String UNDEFINED = "undefined";
	private Integer id;
	private String name;
	private String description;
	private Date version;
	private RepositoryType repositoryType;
	private URI uri;
	private boolean isActive;
	private boolean isStrict;
	private RepositoryMode repositoryMode;
	private boolean isPublished;
	private String digest;
	private String remoteRepositoryName;
	private byte[] acceptedCerts;

	public RepositoryImpl() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(unique = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Size(max = ValidationPatterns.LENGTH_8192, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(length = ValidationPatterns.LENGTH_8192)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Version
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	public RepositoryType getRepositoryType() {
		return repositoryType;
	}

	public void setRepositoryType(RepositoryType repositoryType) {
		this.repositoryType = repositoryType;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isPublished() {
		return isPublished;
	}

	public void setPublished(boolean isPublished) {
		this.isPublished = isPublished;
	}

	public boolean isStrict() {
		return isStrict;
	}

	public void setStrict(boolean isStrict) {
		this.isStrict = isStrict;
	}

	@Column(name = "mode")
	@Enumerated(EnumType.STRING)
	public RepositoryMode getRepositoryMode() {
		return repositoryMode;
	}

	public void setRepositoryMode(RepositoryMode repositoryMode) {
		this.repositoryMode = repositoryMode;
	}

	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(name = "remote_repository_name", length = ValidationPatterns.LENGTH_64)
	public String getRemoteRepositoryName() {
		return remoteRepositoryName;
	}

	public void setRemoteRepositoryName(String remoteRepositoryName) {
		this.remoteRepositoryName = remoteRepositoryName;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	@Transient
	public List<InstallablePackage> getInstallablePackages(List<Identifier> provisionedPackages)
			throws BusinessException {
		Map<String, Identifier> installedPackages = getInstalledPackagesMap(provisionedPackages);
		RepositoryCache cache = getRepositoryCache();
		List<PackageWrapper> publishedApplications = cache.getApplications();
		List<InstallablePackage> provisionableApplications = new ArrayList<InstallablePackage>();
		for (PackageWrapper publishedApplicationWrapper : publishedApplications) {
			InstallablePackage provisionableApplication;
			String packageName = publishedApplicationWrapper.getName();
			if (installedPackages.containsKey(packageName)) {
				String applicationVersion = installedPackages.get(packageName).getPackageVersion();
				provisionableApplication = new InstallablePackage(cache.getPublishedApplicationWrapper(packageName),
						applicationVersion);
			} else {
				provisionableApplication = new InstallablePackage(cache.getPublishedApplicationWrapper(packageName));
			}
			provisionableApplications.add(provisionableApplication);
		}
		return provisionableApplications;
	}

	@Transient
	private Map<String, Identifier> getInstalledPackagesMap(List<? extends Identifier> installedPackages) {
		Map<String, Identifier> installedPackagesMap = new HashMap<String, Identifier>();
		for (Identifier pckg : installedPackages) {
			installedPackagesMap.put(pckg.getName(), pckg);
		}
		return installedPackagesMap;
	}

	@Transient
	public List<PackageVersion> getPackageVersions(List<Identifier> provisionedApplicationsList, String name)
			throws BusinessException {
		Map<String, Identifier> provisionedApplications = getInstalledPackagesMap(provisionedApplicationsList);
		RepositoryCache cache = getRepositoryCache();
		List<PackageInfo> publishedApplicationVersions = cache.getVersions(name);
		List<PackageVersion> provisionableApplicationVersions = new ArrayList<PackageVersion>();
		for (PackageInfo applicationInfo : publishedApplicationVersions) {
			PackageVersion provisionableApplicationVersion;
			String applicationName = applicationInfo.getName();
			String applicationVersion = applicationInfo.getVersion();
			String applicationTimestamp = applicationInfo.getTimestamp();
			Identifier provisionedApplication = provisionedApplications.get(applicationName);
			boolean isDeletable = repositoryType.equals(RepositoryType.LOCAL);
			boolean isProvisioned = (null != provisionedApplication)
					&& provisionedApplication.getPackageVersion().equals(applicationVersion)
					&& provisionedApplication.getTimestamp().equals(applicationTimestamp);
			provisionableApplicationVersion = new PackageVersion(applicationInfo, isProvisioned, isDeletable);
			provisionableApplicationVersions.add(provisionableApplicationVersion);
		}
		Collections.sort(provisionableApplicationVersions);
		return provisionableApplicationVersions;
	}

	@Transient
	public Packages getPackages() throws BusinessException {
		RepositoryCache cache = getRepositoryCache();
		Packages packages = new Packages();
		packages.setCertification(cache.getCertification());
		for (PackageWrapper packageWrapper : cache.getApplications()) {
			org.appng.core.xml.repository.Package publishedPackage = packageWrapper.getPackage();
			packages.getPackage().add(publishedPackage);
		}
		return packages;
	}

	@Transient
	public Certification getCertification() throws BusinessException {
		return getRepositoryCache().getCertification();
	}

	@Transient
	public PackageVersions getPackageVersions(String name) throws BusinessException {
		RepositoryCache cache = getRepositoryCache();
		PackageVersions packageVersions = new PackageVersions();
		for (PackageInfo packageInfo : cache.getVersions(name)) {
			packageVersions.getPackage().add(packageInfo);
		}
		return packageVersions;
	}

	@Transient
	public PackageArchive getPackageArchive(String applicationName, String applicationVersion,
			String applicationTimestamp) throws BusinessException {
		if (isActive && StringUtils.isNotEmpty(applicationName)) {
			RepositoryCache repositoryCache = getRepositoryCache();
			return repositoryCache.getApplicationArchive(applicationName, applicationVersion, applicationTimestamp);
		} else if (!isActive) {
			throw new BusinessException("Repository is not active: " + name);
		}
		return null;
	}

	@Transient
	public void deletePackageVersion(String applicationName, String applicationVersion, String applicationTimestamp)
			throws Exception {
		if (isActive && StringUtils.isNotEmpty(applicationName)) {
			RepositoryCache repositoryCache = getRepositoryCache();
			repositoryCache.deleteApplicationVersion(applicationName, applicationVersion, applicationTimestamp);
		} else {
			throw new Exception("Application repository is not active! ID: " + id);
		}
	}

	@Transient
	private RepositoryCache getRepositoryCache() throws BusinessException {
		return RepositoryCacheFactory.instance().getCache(this);
	}

	public static ApplicationImpl getApplication(ApplicationInfo applicationInfo) {
		return getApplication(new ApplicationImpl(), applicationInfo);
	}

	public static ApplicationImpl getApplication(ApplicationImpl application, ApplicationInfo applicationInfo) {
		application.setDescription(applicationInfo.getDescription());
		application.setLongDescription(applicationInfo.getLongDescription());
		String appNGVersion = applicationInfo.getAppngVersion();
		if (StringUtils.isBlank(appNGVersion)) {
			appNGVersion = UNDEFINED;
		}
		application.setAppNGVersion(appNGVersion);
		application.setTimestamp(applicationInfo.getTimestamp());
		application.setName(applicationInfo.getName());
		application.setApplicationVersion(applicationInfo.getVersion());
		if (null == application.getId() || StringUtils.isBlank(application.getDisplayName())) {
			application.setDisplayName(applicationInfo.getDisplayName());
		}
		application.setSnapshot(RepositoryUtils.isSnapshot(applicationInfo.getVersion()));
		return application;
	}

	/**
	 * Reloads the application repository.
	 * 
	 * @throws BusinessException
	 */
	public void reload() throws BusinessException {
		getRepositoryCache().reload();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Lob
	@Column(name = "accepted_certs")
	public byte[] getAcceptedCerts() {
		return acceptedCerts;
	}

	public void setAcceptedCerts(byte[] acceptedCerts) {
		this.acceptedCerts = acceptedCerts;
	}

	/**
	 * Returns the certificate chain, if this is a signed remote repository.
	 * 
	 * @return the immutable certificate chain
	 * @throws SigningException
	 *             if an error occurs while retrieving the certificate chain
	 * @throws BusinessException
	 *             if an the remote repository could not be reached
	 */
	@Transient
	public Collection<X509Certificate> getRemoteCerts() throws SigningException, BusinessException {
		Certification certification = getRepositoryCache().getCertification();
		try {
			return CertTools.addCerts(Base64.decodeBase64(certification.getCert()), new ArrayList<X509Certificate>());
		} catch (CertificateException e) {
			throw new SigningException(ErrorType.VERIFY, "error decoding cert", e);
		}
	}

	/**
	 * Sets the certificate chain to trust for this repository
	 * 
	 * @param trustedCerts
	 *            the certificate chain
	 * @throws SigningException
	 *             if an error occurs while encoding the certificates to their binary form
	 * @see #getTrustedCertChain()
	 */
	public void setTrustedCertChain(Collection<X509Certificate> trustedCerts) throws SigningException {
		try {
			setAcceptedCerts(CertTools.writeCerts(trustedCerts));
		} catch (IOException | CertificateEncodingException e) {
			throw new SigningException(ErrorType.VERIFY, "error encoding cert(s)", e);
		}
	}

	@Transient
	public Collection<X509Certificate> getTrustedCertChain() throws SigningException {
		if (null == getAcceptedCerts()) {
			return null;
		}
		try {
			return CertTools.getCerts(getAcceptedCerts());
		} catch (CertificateException e) {
			throw new SigningException(ErrorType.VERIFY, "error reading certificate", e);
		}
	}

}
