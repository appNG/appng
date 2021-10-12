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

import java.util.Date;

import org.appng.api.model.Identifier;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.Datasources;
import org.appng.xml.application.PackageInfo;
import org.appng.xml.application.Permissions;
import org.appng.xml.application.Properties;
import org.appng.xml.application.Roles;

/**
 * Delegate for {@link ApplicationInfo }.
 * 
 * @author Matthias Herlitzius
 */
public class PackageVersion implements Identifier, Comparable<PackageVersion> {

	private final PackageInfo packageInfo;
	private final boolean installed;
	private final boolean deletable;
	private boolean isApplication = false;

	public PackageVersion(PackageInfo packageInfo, boolean isInstalled, boolean isDeletable) {
		this.packageInfo = packageInfo;
		this.installed = isInstalled;
		this.deletable = isDeletable;
		this.isApplication = (packageInfo instanceof ApplicationInfo);
	}

	public boolean isInstalled() {
		return installed;
	}

	public boolean isDeletable() {
		return deletable;
	}

	public PackageInfo getPackageInfo() {
		return packageInfo;
	}

	public Integer getId() {
		return null;
	}

	public String getPackageVersion() {
		return packageInfo.getVersion();
	}

	public String getName() {
		return packageInfo.getName();
	}

	public String getDescription() {
		return packageInfo.getDescription();
	}

	public String getTimestamp() {
		return packageInfo.getTimestamp();
	}

	public String getDisplayName() {
		return packageInfo.getDisplayName();
	}

	public Date getVersion() {
		return RepositoryUtils.getDate(packageInfo);
	}

	public String getLongDescription() {
		return isApplication ? ((ApplicationInfo) packageInfo).getLongDescription() : null;
	}

	public String getAppNGVersion() {
		return packageInfo.getAppngVersion();
	}

	public Roles getRoles() {
		return isApplication ? ((ApplicationInfo) packageInfo).getRoles() : null;
	}

	public Permissions getPermissions() {
		return isApplication ? ((ApplicationInfo) packageInfo).getPermissions() : null;
	}

	public Properties getProperties() {
		return isApplication ? ((ApplicationInfo) packageInfo).getProperties() : null;
	}

	public Datasources getDatasources() {
		return isApplication ? ((ApplicationInfo) packageInfo).getDatasources() : null;
	}

	public int compareTo(PackageVersion o) {
		return RepositoryUtils.getVersionComparator().compare(o.packageInfo, packageInfo);
	}

	public boolean isSnapshot() {
		return RepositoryUtils.isSnapshot(getPackageVersion());
	}

}
