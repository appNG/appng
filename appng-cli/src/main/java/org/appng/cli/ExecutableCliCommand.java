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
package org.appng.cli;

import org.appng.api.BusinessException;

/**
 * Interface to be implemented by each command.
 * 
 * @author Matthias Herlitzius
 */
public interface ExecutableCliCommand {

	/**
	 * Executes the command.
	 * 
	 * @param cle
	 *            the {@link CliEnvironment} to use
	 * 
	 * @throws BusinessException
	 *                           if on error occurs while executing the command
	 */
	void execute(CliEnvironment cle) throws BusinessException;

}
