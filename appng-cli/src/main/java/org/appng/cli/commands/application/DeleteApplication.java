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
package org.appng.cli.commands.application;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.FieldProcessor;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Deletes a application.<br/>
 * 
 * <pre>
 * Usage: appng delete-application [options]
 *   Options:
 *   * -a
 *        The application name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Deletes a application.")
public class DeleteApplication implements ExecutableCliCommand {

	private static final String NL = "\n";
	@Parameter(names = "-a", required = true, description = "The application name.")
	private String applicationName;

	public void execute(CliEnvironment cle) throws BusinessException {

		FieldProcessor fp = new FieldProcessorImpl(null);

		try {
			cle.getCoreService().deleteApplication(applicationName, fp);
		} catch (BusinessException e) {
			throw new BusinessException(e);
		} finally {
			StringBuilder result = new StringBuilder();
			List<Message> messages = fp.getMessages().getMessageList();
			for (Message message : messages) {
				MessageType clazz = message.getClazz();
				String content = message.getContent();
				String str = clazz.name() + ": " + content + NL;
				result = result.append(str);
			}
			cle.setResult(result.toString());
		}
	}
}
