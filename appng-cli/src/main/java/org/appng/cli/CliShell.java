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
package org.appng.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.tools.os.OperatingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Wraps the appNG CLI in a shell.
 * 
 * @author Matthias Müller
 *
 */
public class CliShell {

	/**
	 * Starts the shell
	 * 
	 * @param args
	 *            May contain the path to the appNG installation ({@code APPNG_HOME}) (optional). If empty, the system
	 *            property {@value CliBootstrap#APPNG_HOME} is used.
	 * @throws Exception
	 *             if an error occurs while initializing /executing the shell
	 */
	public static void main(String[] args) throws Exception {
		if (!OperatingSystem.isWindows()) {
			System.out.println("\033[31m");
		}

		ClassLoader classLoader = CliShell.class.getClassLoader();
		List<String> logo = IOUtils.readLines(classLoader.getResourceAsStream("cli-shell.txt"), StandardCharsets.UTF_8);
		logo.forEach(l -> System.out.println(l));

		CliBootstrapEnvironment env = new CliBootstrapEnvironment();
		File platformRootPath = null;
		String appngHome = null;

		if (args.length == 1) {
			appngHome = args[0];
		} else {
			appngHome = System.getProperty(CliBootstrap.APPNG_HOME);
		}
		if (null != appngHome) {
			platformRootPath = new File(appngHome).getAbsoluteFile();
		}
		if (null == platformRootPath || !platformRootPath.exists()) {
			platformRootPath = CliBootstrap.getPlatformRootPath(env);
		}

		Properties cliConfig = CliBootstrap.getCliConfig(env, true, platformRootPath);

		FutureTask<ApplicationContext> futureTask = new FutureTask<ApplicationContext>(
				new Callable<ApplicationContext>() {
					public ConfigurableApplicationContext call() throws Exception {
						return CliBootstrap.getContext(cliConfig, CliBootstrap.CLI_CONTEXT_XML);
					}
				});

		System.out.print("Loading appNG shell");
		Executors.newFixedThreadPool(1).execute(futureTask);
		long start = System.currentTimeMillis();
		while (!futureTask.isDone()) {
			System.out.print(".");
			Thread.sleep(500);
		}
		long stop = System.currentTimeMillis();
		System.out.println("done! duration: " + (stop - start) + "ms");
		ApplicationContext context = futureTask.get(1, TimeUnit.MINUTES);

		CliCore cliCore = new CliCore();
		cliCore.setContext(context);

		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.print("appng> ");
			String commandLine = console.readLine();
			if ("exit".equals(StringUtils.trimToEmpty(commandLine))) {
				System.out.println("bye");
				System.exit(0);
			}
			if (null != StringUtils.trimToNull(commandLine)
					&& cliCore.processCommand(commandLine.split(StringUtils.SPACE))) {
				cliCore.perform(cliConfig);
			}
		}

	}

}
