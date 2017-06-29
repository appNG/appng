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
package org.appng.cli.commands.site;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.appng.api.BusinessException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.NoSuchSiteException;
import org.appng.core.domain.SiteImpl;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Platform;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Checks whether a {@link Site} is running.<br/>
 * 
 * <pre>
 * Usage: appng check-site [options]
 *   Options:
 *   * -n
 *      The site name.
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Parameters(commandDescription = "Checks whether a site is running.")
public class CheckSiteRunning implements ExecutableCliCommand {

	static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static final int TIMEOUT = 10000;

	@Parameter(names = "-n", required = true, description = "The site name.")
	private String name;

	protected int responseCode = -1;
	private String version;
	private boolean running;

	public CheckSiteRunning() {

	}

	public CheckSiteRunning(String name) {
		this.name = name;
	}

	String getContent(String uri) {
		InputStream is = null;
		HttpURLConnection connection = null;
		try {
			URL url = new URL(uri);
			connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(TIMEOUT);
			this.responseCode = connection.getResponseCode();
			is = connection.getInputStream();
			return IOUtils.toString(is, Charset.defaultCharset());
		} catch (IOException e) {
			// ignore
		} finally {
			IOUtils.closeQuietly(is);
			if (null != connection) {
				connection.disconnect();
			}
		}
		return null;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		SiteImpl site = cle.getCoreService().getSiteByName(name);
		if (null == site) {
			throw new NoSuchSiteException(name);
		}
		String adminPath = site.getProperties().getString(SiteProperties.MANAGER_PATH);
		String domain = site.getDomain();
		StringBuilder sb = new StringBuilder();
		if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
			sb.append("http://");
		}
		sb.append(domain);
		sb.append(adminPath);
		sb.append("/");
		sb.append(site.getName());
		String siteUri = sb.toString();
		sb.append("?xsl=false");
		try {
			String uri = sb.toString();
			String content = getContent(uri);

			if (null != content && content.startsWith(XML_PREFIX)) {
				Platform platform = MarshallService.getMarshallService().unmarshall(content, Platform.class);
				this.version = platform.getVersion();
				this.running = true;
				CliEnvironment.out.println("site '" + name + "' is running at " + siteUri + " with appNG version "
						+ version);
			} else {
				String message = "site '" + name + "' is NOT running at " + siteUri;
				if (responseCode > 0) {
					message = message + " HTTP response code: " + responseCode;
				}
				CliEnvironment.out.println(message);
			}
		} catch (JAXBException e) {
			throw new BusinessException("failed to parse response from " + siteUri, e);
		}
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getVersion() {
		return version;
	}

	public boolean isRunning() {
		return running;
	}

}
