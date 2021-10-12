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
package org.appng.tools.os;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum type for different operating systems.
 * 
 * @author Matthias Müller
 */
public enum OperatingSystem {

	LINUX("linux", "nix"), WINDOWS("windows"), MACOSX("mac"), OTHER("");

	private String[] searchString;
	private static final List<OperatingSystem> operatingSystems = Arrays.asList(values());

	private OperatingSystem(String... searchString) {
		this.searchString = searchString;
	}

	/**
	 * Detects the current operating system.
	 *
	 * @return The enum depicting the current operating system.
	 */
	public static OperatingSystem detect() {
		return detect(System.getProperty("os.name"));
	}

	static OperatingSystem detect(String osName) {
		String name = null != osName ? osName.toLowerCase() : "";
		return operatingSystems.stream().filter(os -> StringUtils.containsAny(name, os.searchString)).findFirst()
				.orElse(OTHER);
	}

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that contains {@code "linux"} or {@code "nix"}
	 * (ignoring case).
	 * 
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isLinux() {
		return LINUX.equals(detect());
	}

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that contains {@code "mac"} (ignoring case).
	 * 
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isMac() {
		return MACOSX.equals(detect());
	}

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that contains {@code "windows"} (ignoring case).
	 * 
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isWindows() {
		return WINDOWS.equals(detect());
	}

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that does not match the other operating system
	 * name patterns.
	 *
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isOther() {
		return OTHER.equals(detect());
	}

	static boolean isOs(OperatingSystem os) {
		return os.equals(detect());
	}

}
