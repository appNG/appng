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
package org.appng.tools.os;

/**
 * Enum type for different operating systems.
 * 
 * @author Matthias Müller
 * 
 */
public enum OperatingSystem {

	LINUX, MAC, WINDOWS;

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that contains {@code "linux"} (ignoring case).
	 * 
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isLinux() {
		return isOs(LINUX);
	}

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that contains {@code "mac"} (ignoring case).
	 * 
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isMac() {
		return isOs(MAC);
	}

	/**
	 * Checks if {@code System.getProperty("os.name")} returns a value that contains {@code "windows"} (ignoring case).
	 * 
	 * @return {@code true} if this is the case, {@code false} otherwise
	 */
	public static boolean isWindows() {
		return isOs(WINDOWS);
	}

	static boolean isOs(OperatingSystem os) {
		return System.getProperty("os.name").toLowerCase().contains(os.name().toLowerCase());
	}

}
