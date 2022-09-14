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
package org.appng.appngizer.controller;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.RequestUtil;
import org.appng.api.SiteProperties;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.appngizer.model.Link;
import org.appng.appngizer.model.Site;
import org.appng.appngizer.model.Sites;
import org.appng.core.controller.messaging.ReloadSiteEvent;
import org.appng.core.domain.SiteImpl;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RestController
public class SiteController extends ControllerBase {

	@GetMapping(value = "/site")
	public ResponseEntity<Sites> listSites() {
		List<Site> siteList = new ArrayList<>();
		for (SiteImpl site : getCoreService().getSites()) {
			Site fromDomain = Site.fromDomain(site);
			fromDomain.applyUriComponents(getUriBuilder());
			siteList.add(fromDomain);
		}
		Sites sites = new Sites(siteList);
		sites.applyUriComponents(getUriBuilder());
		return new ResponseEntity<Sites>(sites, HttpStatus.OK);
	}

	@GetMapping(value = "/site/{name}")
	public ResponseEntity<Site> getSite(@PathVariable("name") String name) {
		SiteImpl site = getSiteByName(name);
		if (null == site) {
			return notFound();
		}
		Site fromDomain = Site.fromDomain(site);
		if (null != getSender(DefaultEnvironment.getGlobal()) || supportsReloadFile(site)) {
			fromDomain.addLink(new Link("reload", "/site/" + name + "/reload"));
		}
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	public static class ReloadSiteFromAppNGizer extends ReloadSiteEvent implements Serializable {

		public ReloadSiteFromAppNGizer(String siteName) {
			super(siteName);
		}

		@Override
		protected void setNodeId(String nodeId) {
			super.setNodeId(nodeId + "_appNGizer");
		}
	}

	@PutMapping(value = "/site/{name}/reload")
	public ResponseEntity<Void> reloadSite(@PathVariable("name") String name) throws BusinessException {
		SiteImpl site = getSiteByName(name);
		if (null == site) {
			return notFound();
		}
		Sender sender = getSender(DefaultEnvironment.getGlobal());
		if (null != sender) {
			LOGGER.debug("messaging is active, sending ReloadSiteEvent");
			sender.send(new ReloadSiteFromAppNGizer(name));
		} else if (supportsReloadFile(site)) {
			String rootDir = site.getProperties().getString(SiteProperties.SITE_ROOT_DIR);
			File reloadFile = new File(rootDir, ".reload");
			try {
				LOGGER.debug("Created reload marker {}", reloadFile.getAbsolutePath());
				FileUtils.touch(reloadFile);
			} catch (IOException e) {
				throw new BusinessException(e);
			}
		} else {
			return reply(HttpStatus.METHOD_NOT_ALLOWED);
		}
		return ok(null);
	}

	private Sender getSender(Environment env) {
		return Messaging.getMessageSender(env);
	}

	private boolean supportsReloadFile(org.appng.api.model.Site site) {
		return Boolean.TRUE.equals(site.getProperties().getBoolean(SiteProperties.SUPPORT_RELOAD_FILE));
	}

	@PostMapping(value = "/site")
	public ResponseEntity<Site> createSite(@RequestBody org.appng.appngizer.model.xml.Site site) {
		SiteImpl currentSite = getSiteByName(site.getName());
		if (null != currentSite) {
			return conflict();
		}
		getCoreService().createSite(Site.toDomain(site));
		return created(getSite(site.getName()).getBody());
	}

	@PutMapping(value = "/site/{name}")
	public ResponseEntity<Site> updateSite(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Site site) {
		SiteImpl siteByName = getSiteByName(name);
		if (null == siteByName) {
			return notFound();
		}
		siteByName.setHost(site.getHost());
		siteByName.setDomain(site.getDomain());
		siteByName.setDescription(site.getDescription());
		siteByName.setActive(site.isActive());
		getCoreService().saveSite(siteByName);
		return ok(getSite(name).getBody());
	}

	@DeleteMapping(value = "/site/{name}")
	public ResponseEntity<Void> deleteSite(@PathVariable("name") String name)
			throws BusinessException {
		SiteImpl currentSite = getSiteByName(name);
		if (null == currentSite) {
			return notFound();
		}
		Environment environment = DefaultEnvironment.getGlobal();
		org.appng.api.model.Site site = RequestUtil.getSiteByName(environment, name);
		if (site != null && (site.getState() == SiteState.STARTING || site.getState() == SiteState.STARTED
				|| site.getState() == SiteState.STOPPING)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		getCoreService().deleteSite(name, new FieldProcessorImpl("delete-site"));
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(getUriBuilder().path("/site").build().toUri());
		return noContent(headers);
	}

	Logger logger() {
		return LOGGER;
	}

}
