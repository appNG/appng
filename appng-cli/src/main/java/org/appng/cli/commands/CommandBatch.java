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
package org.appng.cli.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.cli.CliCore;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.validators.FileExists;
import org.appng.el.ExpressionEvaluator;
import org.springframework.context.ApplicationContext;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * An {@link ExecutableCliCommand} processing a batch file.
 * 
 * <pre>
 * Usage: appng batch [options]
 *   Options:
 *     -d
 *        Print the commands that would be executed after the batch file has been
 *        parsed, but do not execute them (dry-run).
 *        Default: false
 *   * -f
 *        The name of the batch file.
 * </pre>
 * 
 * @author Matthias Müller
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Processes a batch file")
public class CommandBatch implements ExecutableCliCommand {

	private static final char DOUBLE_QUOTE = '"';
	private static final String COMMENT_PREFIX = "#";
	private static final String VAR_PREFIX = "$";
	private static final String VAR_DECLARATION = "def";
	private static final char VAR_ASSIGNMENT = '=';
	private Map<String, String> variables = new HashMap<>();

	@Parameter(names = "-f", required = true, description = "The name of the batch file.", validateWith = FileExists.class)
	private String fileName;

	@Parameter(names = "-d", required = false, description = "Print the commands that would be executed after the batch file has been parsed, but do not execute them (dry-run).")
	private boolean dryRun = false;

	private void execute(CliCore cliCore, Properties config, String command) throws IOException {
		String[] args = parseLine(command);
		if (args.length > 0) {
			StringBuilder builder = new StringBuilder();
			for (String chunk : args) {
				builder = builder.append(chunk + " ");
			}
			CliEnvironment.out.println(builder.toString().trim());
			if (!dryRun && cliCore.processCommand(args)) {
				cliCore.perform(config);
			}
		}
	}

	/**
	 * Reads each line from the provided file and performs the corresponding command.
	 * 
	 * @param cle
	 *            the current {@link CliEnvironment}
	 */
	public void execute(CliEnvironment cle) throws BusinessException {
		File file = new File(fileName).getAbsoluteFile();
		ApplicationContext platformContext = cle.getContext();
		CliCore cliCore = new CliCore();
		cliCore.setContext(platformContext);
		String actual = null;
		try {
			Properties cliConfig = cle.getCliConfig();
			try (FileInputStream fis = new FileInputStream(file)) {
				List<String> lines = IOUtils.readLines(fis, Charset.defaultCharset());
				for (String command : lines) {
					actual = command;
					execute(cliCore, cliConfig, command);
				}
			}
		} catch (Exception e) {
			if (null != actual) {
				throw new BusinessException("error while performing " + actual, e);
			}
			throw new BusinessException("error while batch-processing " + fileName, e);
		}
	}

	protected String[] parseLine(String command) {
		String[] args = new String[0];
		if (command.startsWith(COMMENT_PREFIX) || StringUtils.isBlank(command)) {
			return args;
		}

		Map<String, Object> params = new HashMap<>(variables);
		params.put("systemEnv", System.getenv());
		params.put("systemProp", System.getProperties());
		ExpressionEvaluator ee = new ExpressionEvaluator(params);

		String line = ee.evaluate(command, String.class);
		if (line.startsWith(VAR_DECLARATION)) {
			int eqIdx = line.indexOf(VAR_ASSIGNMENT);
			String name = line.substring(VAR_DECLARATION.length() + 1, eqIdx).trim();
			String value = line.substring(eqIdx + 1).trim();
			if (value.contains(StringUtils.SPACE) && value.charAt(0) != DOUBLE_QUOTE) {
				value = DOUBLE_QUOTE + value + DOUBLE_QUOTE;
			}
			variables.put(name, value);
		} else {
			args = parse(line).toArray();
			if (args.length == 0 && StringUtils.isNotBlank(line)) {
				args = line.split(StringUtils.SPACE);
			}
		}
		return args;
	}

	protected Line parse(String command) {
		Line line = new Line();
		String current = StringUtils.EMPTY;

		for (char c : command.toCharArray()) {

			switch (line.getCharType(c)) {

			case SPACE:
				line.addToken(current);
				current = StringUtils.EMPTY;
				break;

			case QUOTE:
				current += DOUBLE_QUOTE;
				if (line.hasOpened()) {
					line.addToken(current);
					current = StringUtils.EMPTY;
					line.setOpened(false);
					line.setSkipAdd(true);
				} else {
					line.setOpened(true);
				}
				break;

			case VARIABLE:
				if (current.length() > 0) {
					line.addToken(current);
					current = StringUtils.EMPTY;
				}
				line.setAssumeVariable(true);
				break;

			case REGULAR:
				current += c;
				line.setSkipAdd(false);
				break;

			default:

			}

		}
		if (current.length() > 0) {
			line.addToken(current);
		}
		return line;
	}

	protected Map<String, String> getVariables() {
		return variables;
	}

	private class Line {

		private static final char SINGLE_QUOTE = '\'';
		private List<String> tokens = new ArrayList<>();
		private boolean hasOpened = false;
		private boolean assumeVariable = false;
		private boolean skipAdd = false;

		private CharType getCharType(char c) {
			if (' ' == c && !hasOpened && !assumeVariable && !skipAdd) {
				return CharType.SPACE;
			} else if (DOUBLE_QUOTE == c || SINGLE_QUOTE == c) {
				return CharType.QUOTE;
			} else if (!tokens.isEmpty() && tokens.get(0).equals(VAR_DECLARATION) && VAR_ASSIGNMENT == c) {
				return CharType.VARIABLE;
			} else {
				return CharType.REGULAR;
			}
		}

		private void addToken(String current) {
			String token = current.trim();
			if (token.startsWith(VAR_PREFIX) && variables.containsKey(getVariableName(token))) {
				tokens.add(variables.get(getVariableName(token)));
			} else {
				tokens.add(token);
			}
		}

		private String getVariableName(String token) {
			if (token.length() > 1) {
				return token.substring(1);
			} else {
				return StringUtils.EMPTY;
			}
		}

		private String[] toArray() {
			return tokens.toArray(new String[tokens.size()]);
		}

		private boolean hasOpened() {
			return hasOpened;
		}

		private void setOpened(boolean opened) {
			this.hasOpened = opened;
		}

		private void setAssumeVariable(boolean assumeVariable) {
			this.assumeVariable = assumeVariable;
		}

		private void setSkipAdd(boolean skipAdd) {
			this.skipAdd = skipAdd;
		}

	}

	private enum CharType {
		QUOTE, REGULAR, SPACE, VARIABLE;
	}
}
