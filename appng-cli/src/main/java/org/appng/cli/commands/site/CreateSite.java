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
package org.appng.cli.commands.site;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.SiteImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Creates a site.<br/>
 * 
 * <pre>
 * Usage: appng create-site [options]
 *   Options:
 *   * -d
 *        The site domain, as it should appear in URLs (incl. protocol and
 *        optionally port).
 *     -e
 *        Enable site.
 *        Default: false
 *   * -h
 *        The site host.
 *   * -n
 *        The site name.
 *     -r
 *        create a repository for the site.
 *        Default: false
 *     -t
 *        The site description.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Creates a site.")
public class CreateSite implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The site name.")
	private String name;

	@Parameter(names = "-h", required = true, description = "The site host.")
	private String host;

	@Parameter(names = "-d", required = true, description = "The site domain, as it should appear in URLs (incl. protocol and optionally port).")
	private String domain;

	@Parameter(names = "-t", required = false, description = "The site description.")
	private String description;

	@Parameter(names = "-e", description = "Enable site.")
	private boolean isEnabled = false;

	@Parameter(names = "-r", description = "create a repository for the site.")
	private boolean createRepo = false;

	public CreateSite() {

	}

	CreateSite(String name, String host, String domain, String description, boolean isEnabled, boolean createRepo) {
		this.name = name;
		this.host = host;
		this.domain = domain;
		this.description = description;
		this.isEnabled = isEnabled;
		this.createRepo = createRepo;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		SiteImpl site = new SiteImpl();
		site.setName(name);
		site.setHost(host);
		site.setDomain(domain);
		site.setDescription(description);
		site.setActive(isEnabled);
		site.setCreateRepository(createRepo);
		cle.getCoreService().createSite(site);
	}

}
