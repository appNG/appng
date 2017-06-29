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
package org.appng.appngizer.controller;

import java.util.ArrayList;
import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.appngizer.model.Site;
import org.appng.appngizer.model.Sites;
import org.appng.core.controller.messaging.ReloadSiteEvent;
import org.appng.core.domain.SiteImpl;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SiteController extends ControllerBase {

	@RequestMapping(value = "/site", method = RequestMethod.GET)
	public ResponseEntity<Sites> listSites() {
		List<Site> siteList = new ArrayList<Site>();
		for (SiteImpl site : getCoreService().getSites()) {
			Site fromDomain = Site.fromDomain(site);
			fromDomain.applyUriComponents(getUriBuilder());
			siteList.add(fromDomain);
		}
		Sites sites = new Sites(siteList);
		sites.applyUriComponents(getUriBuilder());
		return new ResponseEntity<Sites>(sites, HttpStatus.OK);
	}

	@RequestMapping(value = "/site/{name}", method = RequestMethod.GET)
	public ResponseEntity<Site> getSite(@PathVariable("name") String name) {
		SiteImpl site = getSiteByName(name);
		if (null == site) {
			return notFound();
		}
		Site fromDomain = Site.fromDomain(site);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/site/{name}/reload", method = RequestMethod.PUT)
	public ResponseEntity<Void> reloadSite(@PathVariable("name") String name) {
		Sender sender = Messaging.getMessageSender(DefaultEnvironment.get(context));
		if (null == sender) {
			return notFound();
		}
		sender.send(new ReloadSiteEvent(name));
		return ok(null);
	}

	@RequestMapping(value = "/site", method = RequestMethod.POST)
	public ResponseEntity<Site> createSite(@RequestBody org.appng.appngizer.model.xml.Site site) {
		SiteImpl currentSite = getSiteByName(site.getName());
		if (null != currentSite) {
			return conflict();
		}
		getCoreService().createSite(Site.toDomain(site));
		return created(getSite(site.getName()).getBody());
	}

	@RequestMapping(value = "/site/{name}", method = RequestMethod.PUT)
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

	@RequestMapping(value = "/site/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteSite(@PathVariable("name") String name) throws BusinessException {
		SiteImpl currentSite = getSiteByName(name);
		if (null == currentSite) {
			return notFound();
		}
		getCoreService().deleteSite(name, new FieldProcessorImpl("delete-site"));
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(getUriBuilder().path("/site").build().toUri());
		return noContent(headers);
	}

	Logger logger() {
		return log;
	}

}
