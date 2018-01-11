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
package org.appng.cli.commands;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.appng.tools.os.Command;
import org.appng.tools.os.OperatingSystem;
import org.appng.tools.os.StringConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to set the owner and group for a given {@link File} using the {@code chown} system command.
 * 
 * @author Matthias Müller
 * 
 * @see OperatingSystem#isLinux()
 */
public class FileOwner {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileOwner.class);
	private File file;

	/**
	 * Create a new {@code FileOwner} for the given {@link File}.
	 * 
	 * @param file
	 *            the {@link File} to set the owner and group for
	 */
	public FileOwner(File file) {
		this.file = file;
	}

	/**
	 * Set the owner and group for the {@link File} which has been passed to the constructor, using the {@code chown}
	 * system command. Works only on linux operating systems (see {@link OperatingSystem#isLinux()}).
	 * 
	 * @param user
	 *            the owner (user) to set
	 * @param group
	 *            the group to set
	 * @return {@code true} if setting the user/group was successful, {@code false} otherwise
	 */
	public boolean own(String user, String group) {
		if (OperatingSystem.isLinux()) {
			String command = "chown -R " + user + ":" + group + " " + file.getAbsolutePath();
			StringConsumer errorConsumer = new StringConsumer();
			if (0 != Command.execute(command, null, errorConsumer)) {
				LOGGER.warn("'{}' returned '{}'", command, StringUtils.join(errorConsumer.getResult(), "\r\n"));
				return false;
			}
		}
		return true;
	}
}
