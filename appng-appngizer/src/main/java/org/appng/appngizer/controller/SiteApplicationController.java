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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.appng.appngizer.model.Application;
import org.appng.appngizer.model.Applications;
import org.appng.appngizer.model.Link;
import org.appng.appngizer.model.xml.Grant;
import org.appng.appngizer.model.xml.Grants;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.AccessibleApplication;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
public class SiteApplicationController extends ControllerBase {

	@RequestMapping(value = "/site/{site}/application", method = RequestMethod.GET)
	public ResponseEntity<Applications> listApplications(@PathVariable("site") String site) {
		SiteImpl siteByName = getSiteByName(site);
		List<Application> applicationList = new ArrayList<Application>();
		List<String> sortedNames = new ArrayList<String>(siteByName.getApplicationMap().keySet());
		Collections.sort(sortedNames);
		for (String appName : sortedNames) {
			ApplicationImpl application = (ApplicationImpl) siteByName.getApplication(appName);
			Application fromDomain = Application.fromDomain(application, site);
			fromDomain.addLinks();
			applicationList.add(fromDomain);
		}
		Applications applications = new Applications(applicationList);
		applications.setSelf("/site/" + site + "/application");
		applications.applyUriComponents(getUriBuilder());
		return ok(applications);
	}

	@RequestMapping(value = "/site/{site}/application/{app}", method = RequestMethod.GET)
	public ResponseEntity<Application> getApplication(@PathVariable("site") String site,
			@PathVariable("app") String app) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		SiteApplication siteApplication = getSiteApplication(site, app);
		if (null == siteApplication) {
			return notFound();
		}
		ApplicationImpl application = (ApplicationImpl) siteApplication.getApplication();
		Application fromDomain = Application.fromDomain(application, site);
		fromDomain.addLinks();
		fromDomain.addLink(new Link("grants", fromDomain.getSelf() + "/grants"));

		DatabaseConnection dbc = getCoreService().getDatabaseConnection(siteByName, application);
		if (null != dbc) {
			fromDomain.addLink(new Link("database", fromDomain.getSelf() + "/database"));
		}
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/grants", method = RequestMethod.PUT)
	public ResponseEntity<Grants> grantApplicationForSites(@PathVariable("site") String site,
			@PathVariable("app") String appName, @RequestBody Grants grants) {
		SiteApplication application = getSiteApplication(site, appName);
		if (null == application) {
			return notFound();
		}
		List<String> grantedSites = new ArrayList<>();
		for (Grant grant : grants.getGrant()) {
			if (grant.isValue()) {
				grantedSites.add(grant.getSite());
			}
		}
		getCoreService().grantApplicationForSites(site, appName, grantedSites);
		return getGrantsForApplication(site, appName);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/grants", method = RequestMethod.GET)
	public ResponseEntity<Grants> getGrantsForApplication(@PathVariable("site") String site,
			@PathVariable("app") String app) {
		SiteApplication application = getSiteApplication(site, app);
		if (null == application) {
			return notFound();
		}
		Grants grants = new Grants();
		String self = String.format("/site/%s/application/%s/grants", site, app);
		grants.setSelf(getUriBuilder().path(self).build().toUriString());
		List<SiteImpl> sites = getCoreService().getSites();
		for (SiteImpl s : sites) {
			if (!s.getName().equals(site)) {
				Grant grant = new Grant();
				grant.setSite(s.getName());
				boolean isGranted = application.getGrantedSites().contains(s);
				grant.setValue(isGranted);
				grants.getGrant().add(grant);
			}
		}
		return ok(grants);
	}

	@RequestMapping(value = "/site/{site}/application/{app}", method = RequestMethod.POST)
	public ResponseEntity<Void> activateApplication(@PathVariable("site") String site,
			@PathVariable("app") String app) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		AccessibleApplication appByName = getApplicationByName(app);
		if (null == appByName) {
			return notFound();
		}
		boolean isAssigned = siteByName.getApplications().contains(appByName);
		if (isAssigned) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setAllow(new HashSet<HttpMethod>(Arrays.asList(HttpMethod.GET)));
			return reply(httpHeaders, HttpStatus.METHOD_NOT_ALLOWED);
		}
		getCoreService().assignApplicationToSite(siteByName, appByName, true);
		URI location = getUriBuilder().path("/site/{site}/application/{app}").buildAndExpand(site, app).toUri();
		return seeOther(location);
	}

	@RequestMapping(value = "/site/{site}/application/{app}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deactivateApplication(@PathVariable("site") String site,
			@PathVariable("app") String app) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		ApplicationImpl appByName = (ApplicationImpl) siteByName.getApplication(app);
		if (null == appByName) {
			return notFound();
		}
		getCoreService().unlinkApplicationFromSite(siteByName.getId(), appByName.getId());
		URI location = getUriBuilder().path("/site/{site}/application").buildAndExpand(site).toUri();
		return seeOther(location);
	}

	Logger logger() {
		return log;
	}
}
