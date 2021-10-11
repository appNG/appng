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
package org.appng.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.core.xml.repository.Package;
import org.appng.core.xml.repository.PackageType;
import org.appng.xml.application.PackageInfo;

/**
 * A application shown in the {@link Repository} overview list.
 * 
 * @author Matthias Herlitzius
 */
public class PackageWrapper implements Comparable<PackageWrapper> {

	private static final String MINUS = "-";
	private Map<String, PackageInfo> versions = new HashMap<>();
	private Map<String, PackageInfo> latestSnapshotForVersion = new HashMap<>();
	private Package publishedPackage;
	private PackageInfo latestRelease;
	private PackageInfo latestSnapshot;

	public PackageWrapper(Package publishedPackage) {
		this.publishedPackage = publishedPackage;
	}

	public PackageWrapper(PackageInfo packageInfo, PackageType type) {
		this(new Package());
		publishedPackage.setType(type);
		addPackage(packageInfo);
	}

	void init() {
		findLatest();
		if (null != latestSnapshot) {
			publishedPackage.setLatestSnapshot(latestSnapshot.getVersion());
		}
		if (null != latestRelease) {
			publishedPackage.setLatestRelease(latestRelease.getVersion());
		}
		PackageInfo latest = null == latestRelease ? latestSnapshot : latestRelease;
		publishedPackage.setName(latest.getName());
		publishedPackage.setDisplayName(latest.getDisplayName());
		publishedPackage.setLongDescription(latest.getLongDescription());
	}

	void addPackage(PackageInfo packageInfo) {
		String vkey = getVersionKey(packageInfo.getVersion(), packageInfo.getTimestamp());
		versions.put(vkey, packageInfo);
	}

	private String getVersionKey(String version, String timestamp) {
		if (StringUtils.isBlank(timestamp)) {
			return version;
		} else {
			return RepositoryUtils.isSnapshot(version) ? version + MINUS + timestamp : version;
		}
	}

	Map<String, PackageInfo> getVersions() {
		return Collections.unmodifiableMap(versions);
	}

	PackageInfo getVersion(String version, String timestamp) {
		String vkey = getVersionKey(version, timestamp);
		return versions.get(vkey);
	}

	PackageInfo getSnapshot(String version) {
		return latestSnapshotForVersion.get(version);
	}

	PackageInfo getSnapshot() {
		return latestSnapshot;
	}

	PackageInfo getRelease() {
		return latestRelease;
	}

	void removePackageVersion(String version, String timestamp) throws BusinessException {
		String versionKey = getVersionKey(version, timestamp);
		if (versions.containsKey(versionKey)) {
			versions.remove(versionKey);
		} else {
			throw new BusinessException("Package version not found: " + version + ", timestamp: " + timestamp);
		}
	}

	private void findLatest() {
		latestRelease = null;
		latestSnapshot = null;
		for (Entry<String, PackageInfo> e : versions.entrySet()) {
			String currentVersion = e.getKey();
			PackageInfo currentPackage = e.getValue();
			if (RepositoryUtils.isSnapshot(currentVersion)) {
				if (RepositoryUtils.isNewer(currentPackage, latestSnapshot)) {
					latestSnapshot = currentPackage;
				}
				findLatestSnapshotForVersion(currentPackage);
			} else {
				if (RepositoryUtils.isNewer(currentPackage, latestRelease)) {
					latestRelease = currentPackage;
				}
			}
		}
	}

	private void findLatestSnapshotForVersion(PackageInfo current) {
		String currentVersion = current.getVersion();
		PackageInfo latest = latestSnapshotForVersion.get(currentVersion);
		if (RepositoryUtils.isNewer(current, latest)) {
			latestSnapshotForVersion.put(currentVersion, current);
		}
	}

	public Package getPackage() {
		return publishedPackage;
	}

	public String getName() {
		return publishedPackage.getName();
	}

	public String getDisplayName() {
		return publishedPackage.getDisplayName();
	}

	public String getLongDescription() {
		return publishedPackage.getLongDescription();
	}

	public String getLatestRelease() {
		return publishedPackage.getLatestRelease();
	}

	public String getLatestSnapshot() {
		return publishedPackage.getLatestSnapshot();
	}

	public int compareTo(PackageWrapper o) {
		return getName().compareTo(o.getName());
	}

	public PackageType getType() {
		return publishedPackage.getType();
	}

}
