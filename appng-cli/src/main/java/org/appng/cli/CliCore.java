/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.cli.commands.CommandBatch;
import org.appng.cli.commands.CommandMain;
import org.appng.cli.commands.application.ActivateApplication;
import org.appng.cli.commands.application.DeactivateApplication;
import org.appng.cli.commands.application.DeleteApplication;
import org.appng.cli.commands.application.InstallApplication;
import org.appng.cli.commands.application.ListApplications;
import org.appng.cli.commands.applicationrole.AddRole;
import org.appng.cli.commands.applicationrole.ListRoles;
import org.appng.cli.commands.group.AddGroup;
import org.appng.cli.commands.group.CreateGroup;
import org.appng.cli.commands.group.DeleteGroup;
import org.appng.cli.commands.group.ListGroups;
import org.appng.cli.commands.heartbeat.HeartBeat;
import org.appng.cli.commands.permission.AddPermission;
import org.appng.cli.commands.permission.ListPermissions;
import org.appng.cli.commands.permission.RemovePermission;
import org.appng.cli.commands.platform.ExtractData;
import org.appng.cli.commands.property.CreateProperty;
import org.appng.cli.commands.property.DeleteProperty;
import org.appng.cli.commands.property.ListProperties;
import org.appng.cli.commands.property.UpdateProperty;
import org.appng.cli.commands.repository.CreateRepository;
import org.appng.cli.commands.repository.DeleteRepository;
import org.appng.cli.commands.repository.ListRepositories;
import org.appng.cli.commands.site.CheckSiteRunning;
import org.appng.cli.commands.site.CreateSite;
import org.appng.cli.commands.site.DeleteSite;
import org.appng.cli.commands.site.ListSites;
import org.appng.cli.commands.site.ReloadSite;
import org.appng.cli.commands.site.SetSiteActive;
import org.appng.cli.commands.subject.CreateSubject;
import org.appng.cli.commands.subject.DeleteSubject;
import org.appng.cli.commands.subject.HashPassword;
import org.appng.cli.commands.subject.ListSubjects;
import org.appng.cli.commands.template.DeleteTemplate;
import org.appng.cli.commands.template.InstallTemplate;
import org.appng.core.service.DatabaseService;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.context.ApplicationContext;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;

import lombok.extern.slf4j.Slf4j;

/**
 * The core of the command line interface, aggregating the available {@link ExecutableCliCommand}s to a
 * {@link CliCommands}-object.
 * 
 * @author Matthias Herlitzius
 */
@Slf4j
public class CliCore {

	private static final String PROGRAM_NAME = "appng";
	private static final String COMMAND_BATCH = "batch";
	private ApplicationContext platformContext;
	public static final int STATUS_OK = 0;
	public static final int DATABASE_ERROR = 9;
	public static final int COMMAND_EXECUTION_ERROR = 10;
	public static final int COMMAND_INVALID = 11;
	public static final int OPTION_INVALID = 12;
	public static final int OPTION_MISSING = 13;

	private int status = STATUS_OK;
	private JCommander jc;
	private CommandMain cm;
	protected CliCommands commands;

	void addCommands() {

		commands.add("list-groups", new ListGroups());
		commands.add("create-group", new CreateGroup());
		commands.add("add-group", new AddGroup());
		commands.add("delete-group", new DeleteGroup());

		commands.add("list-applications", new ListApplications());
		commands.add("install-application", new InstallApplication());
		commands.add("activate-application", new ActivateApplication());
		commands.add("deactivate-application", new DeactivateApplication());
		commands.add("delete-application", new DeleteApplication());

		commands.add("list-roles", new ListRoles());
		commands.add("add-role", new AddRole());
		commands.add("add-permission", new AddPermission());
		commands.add("remove-permission", new RemovePermission());
		commands.add("list-permissions", new ListPermissions());

		commands.add("list-properties", new ListProperties());
		commands.add("create-property", new CreateProperty());
		commands.add("update-property", new UpdateProperty());
		commands.add("delete-property", new DeleteProperty());

		commands.add("list-repositories", new ListRepositories());
		commands.add("create-repository", new CreateRepository());
		commands.add("delete-repository", new DeleteRepository());

		commands.add("list-sites", new ListSites());
		commands.add("create-site", new CreateSite());
		commands.add("delete-site", new DeleteSite());
		commands.add("check-site", new CheckSiteRunning());
		commands.add("site-setactive", new SetSiteActive());
		commands.add("reload-site", new ReloadSite());

		commands.add("list-subjects", new ListSubjects());
		commands.add("create-subject", new CreateSubject());
		commands.add("hash-pw", new HashPassword());
		commands.add("delete-subject", new DeleteSubject());

		commands.add("install-template", new InstallTemplate());
		commands.add("delete-template", new DeleteTemplate());

		commands.add("heartbeat", new HeartBeat());
		commands.add("extract-data", new ExtractData());

		commands.add(COMMAND_BATCH, new CommandBatch());

	}

	/**
	 * Performs a cli command
	 * 
	 * @param cliConfig
	 *                  the properties read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 **/
	public int perform(final Properties cliConfig) {

		Map<String, String> hibernateParams = cm.getHibernateParams();
		if (null != hibernateParams) {
			cliConfig.putAll(hibernateParams);
		}
		CliEnvironment cle = new CliEnvironment(platformContext, cliConfig);
		DatabaseService databaseService = platformContext.getBean(DatabaseService.class);
		String parsedCommand = jc.getParsedCommand();
		if (!COMMAND_BATCH.equals(parsedCommand)) {
			MigrationInfo migrationInfo;
			if (cm.isInitDatabase()) {
				migrationInfo = cm.doInitDatabase(databaseService, cliConfig);
				if (null == migrationInfo) {
					logError("Database could not be initialized, see logs for details.");
				}
			} else {
				migrationInfo = databaseService.status(cliConfig);
				if (null == migrationInfo) {
					logError("Database is not initialized. Use option -i|-initdatabase to initialize database.");
				}
			}
			if (null == migrationInfo || migrationInfo.getState().isFailed()) {
				logError("Database is in an erroneous state, see logs for details.");
				return DATABASE_ERROR;
			}
			String stateName = migrationInfo.getState().name();
			Date installedOn = migrationInfo.getInstalledOn();
			String logMessage = "Database is at version " + migrationInfo.getVersion() + ", state: " + stateName
					+ ", installed on " + installedOn;
			LOGGER.info(logMessage);
			cle.initPlatform(cliConfig);
		}

		if (null != parsedCommand || cm.isSchemaExport()) {
			try {
				commands.getCommand(parsedCommand).execute(cle);
			} catch (Exception e) {
				LOGGER.error("An error occured.", e);
				String message = e.getMessage();
				if (null != message) {
					logError(message);
				} else {
					logError("Unknown error. Consult the log file for more details.");
				}
				status = COMMAND_EXECUTION_ERROR;
			} finally {
				print(cle.getResult());
			}
		}

		return status;
	}

	public void setContext(ApplicationContext platformContext) {
		this.platformContext = platformContext;
	}

	void usage(JCommander jc) {
		if (null == jc) {
			jc = createJCommanderInstance(null);
		}
		StringBuilder out = new StringBuilder();
		jc.usage(out);
		print(out.toString());
	}

	private JCommander createJCommanderInstance(Object o) {
		JCommander jc;
		if (null == o) {
			jc = new JCommander();
		} else {
			jc = new JCommander(o);
		}
		jc.setProgramName(PROGRAM_NAME);
		return jc;
	}

	private void print(String string) {
		if (StringUtils.isNotEmpty(string)) {
			CliEnvironment.out.println(string);
		}
	}

	/**
	 * Parses the given command line arguments and sets the state for this {@link CliCore}. Must be called before
	 * {@link #perform(Properties)}. Only if this method returns {@code true}, it is reasonable to call
	 * {@link #perform(Properties)}.
	 * 
	 * @param args
	 *             the command line arguments
	 * 
	 * @return {@code true} if the given arguments have been parsed to an {@link ExecutableCliCommand} and
	 *         {@link #perform(Properties)} should be called, {@code false} otherwise
	 * 
	 * @see #getStatus()
	 */
	public boolean processCommand(String[] args) throws ParameterException {
		this.status = STATUS_OK;
		this.cm = new CommandMain();
		this.jc = createJCommanderInstance(cm);
		this.commands = new CliCommands(jc);
		addCommands();
		if (null == args || args.length == 0) {
			usage(jc);
			print(PROGRAM_NAME + " -h for help.");
			return false;
		}
		try {
			jc.parse(args);
			if (cm.isUsage()) {
				usage(jc);
				return false;
			}
			return true;
		} catch (ParameterException ex) {
			String message = ex.getMessage();
			String[] splittedArgs = args[0].split(" ");
			String commandName = splittedArgs[0];
			JCommander failedJc = jc.getCommands().get(commandName);
			if (failedJc != null) {
				if (ex instanceof MissingCommandException) {
					String[] unknownArgs = ArrayUtils.subarray(splittedArgs, 1, splittedArgs.length);
					logError("Invalid options: " + StringUtils.join(unknownArgs, " "));
					status = OPTION_INVALID;
				} else {
					logError(message);
					status = OPTION_MISSING;
				}
				failedJc.setProgramName(PROGRAM_NAME + " " + commandName);
				usage(failedJc);
			} else {
				logError("Invalid command: " + commandName);
				usage(jc);
				status = COMMAND_INVALID;
			}
		}
		return false;
	}

	public int getStatus() {
		return status;
	}

	void logError(String message) {
		CliEnvironment.out.println(LogCategory.ERROR.name() + ": " + message);
	}

	/**
	 * Enum type for logging events.
	 * 
	 * @author Matthias Herlitzius
	 */
	private enum LogCategory {
		INFO, WARN, ERROR;
	}
}
