/*
 * Copyright 2011-2020 the original author or authors.
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

import org.appng.core.xml.repository.PackageType;

/**
 * A application shown in the {@link Repository} overview list.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class InstallablePackage {

	/**
	 * The version of the currently provisioned application. Empty if this application has not yet been provisioned.
	 */
	private final String installedVersion;
	private final PackageWrapper packageWrapper;

	public InstallablePackage(PackageWrapper packageWrapper) {
		this(packageWrapper, "");
	}

	public InstallablePackage(PackageWrapper packageWrapper, String installedVersion) {
		this.installedVersion = installedVersion;
		this.packageWrapper = packageWrapper;
	}

	public String getInstalledVersion() {
		return installedVersion;
	}

	public PackageWrapper getPackageWrapper() {
		return packageWrapper;
	}

	public String getName() {
		return packageWrapper.getName();
	}

	public String getDisplayName() {
		return packageWrapper.getDisplayName();
	}

	public String getLongDescription() {
		return packageWrapper.getLongDescription();
	}

	public String getLatestRelease() {
		return packageWrapper.getLatestRelease();
	}

	public String getLatestSnapshot() {
		return packageWrapper.getLatestSnapshot();
	}

	public PackageType getType() {
		return packageWrapper.getType();
	}

	public boolean isInstalled() {
		return packageWrapper.getPackage().getVersion().equals(installedVersion);
	}
}
