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
package org.appng.tools.os;

import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to conveniently execute command via {@link Runtime#exec(String)}.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class Command {

	public static final int ERROR = -1;
	public static final int WRONG_OS = -2;

	/**
	 * Executes the given command.
	 * 
	 * @param command
	 *            the command to execute
	 * @param outputConsumer
	 *            a {@link StreamConsumer} to consume {@link Process#getInputStream()}
	 * @param errorConsumer
	 *            a {@link StreamConsumer} to consume {@link Process#getErrorStream()}
	 * @return the value returned by {@link Process#waitFor()}, or {@value #ERROR} if an exception occurred while
	 *         executing the process.
	 */
	public static int execute(String command, StreamConsumer<?> outputConsumer, StreamConsumer<?> errorConsumer) {
		try {
			LOGGER.debug("executing: '{}'", command);
			Process process = Runtime.getRuntime().exec(command);
			if (null != outputConsumer) {
				outputConsumer.consume(process.getInputStream());
			}
			if (null != errorConsumer) {
				errorConsumer.consume(process.getErrorStream());
			}
			return process.waitFor();
		} catch (Exception e) {
			LOGGER.warn(String.format("error while executing: %s", command), e);
		}
		return ERROR;
	}

	/**
	 * Executes the given command, but only if the given {@link OperatingSystem} matches.
	 * 
	 * @param os
	 *            the target {@link OperatingSystem}
	 * @param command
	 *            the command to execute
	 * @param outputConsumer
	 *            a {@link StreamConsumer} to consume {@link Process#getInputStream()}
	 * @param errorConsumer
	 *            a {@link StreamConsumer} to consume {@link Process#getErrorStream()}
	 * @return the return value of {@link #execute(String, StreamConsumer, StreamConsumer)}, if the current
	 *         {@link OperatingSystem} matches the desired one, otherwise {@value #WRONG_OS}
	 * 
	 * @see #execute(String, StreamConsumer, StreamConsumer)
	 */
	public static int execute(OperatingSystem os, String command, StreamConsumer<?> outputConsumer,
			StreamConsumer<?> errorConsumer) {
		if (OperatingSystem.isOs(os)) {
			return execute(command, outputConsumer, errorConsumer);
		}
		return WRONG_OS;
	}

	/**
	 * Interface for a type consuming and {@link InputStream}.
	 * 
	 * @author Matthias Müller
	 * 
	 * @param <T>
	 *            the type this {@code StreamConsumer} produces as a result, see {@link #getResult()}
	 */
	public interface StreamConsumer<T> {

		/**
		 * Consumes the given {@link InputStream}.
		 * 
		 * @param is
		 *            the {@link InputStream} to consume
		 * @throws IOException
		 *             if an error occurs while consuming
		 */
		void consume(InputStream is) throws IOException;

		/**
		 * Returns the result for this {@code StreamConsumer}.
		 * 
		 * @return the result of type {@code <T>}
		 */
		T getResult();

	}
}
