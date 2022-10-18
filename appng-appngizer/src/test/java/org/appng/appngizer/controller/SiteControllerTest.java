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

import org.appng.appngizer.model.xml.Site;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SiteControllerTest extends ControllerTest {

	@Test
	public void test01_SiteCreate() throws Exception {
		// Null where xsd allows it.
		Site siteNoOptionals = getAppNGizerSite("nakedhost", "nakedhost", null, "http://nakedhost:8081", null, false,
				true);
		postAndVerify("/site", "xml/site-create-nakedhost.xml", siteNoOptionals, HttpStatus.CREATED);

		// A regular site (not active yet).
		Site siteRegular = getAppNGizerSite("regularhost", "regularhost",
				new String[] { "KillEmAllHost", "RidetheLightningHost", "MasterofPuppetsHost" },
				"http://regularhost:8081", "36:38 regular fit", false, true);
		postAndVerify("/site", "xml/site-create-regularhost.xml", siteRegular, HttpStatus.CREATED);
	}

	@Test
	public void test02_SiteCollisions() throws Exception {
		// New Site with name collision.
		Site siteNameCollision = getAppNGizerSite("regularhost", "regularhost", null, "http://regularhost:8081", null,
				true, true);
		postAndVerify("/site", null, siteNameCollision, HttpStatus.CONFLICT);

		// New Stie with collision between new alias-name and existing site-name.
		/* Functionality not implemented yet.
		Site siteAlias2NameCollision = getAppNGizerSite("newnamehost", "newnamehost",
				new String[] { "regularhost" }, "http://newnamehost:8081", null, true, true);
		postAndVerify("/site", null, siteAlias2NameCollision, HttpStatus.CONFLICT);
		*/

		// New Site with collision between new alias-name and existing alias-name.
		/*
		Site siteAliasCollision = getAppNGizerSite("newnamehost", "newnamehost",
				new String[] { "RidetheLightningHost" }, "http://newnamehost:8081", null, true, true);
		postAndVerify("/site", null, siteAliasCollision, HttpStatus.CONFLICT);
		*/
	}

	/*
	@Test
	public void test03_SiteUpdate() throws Exception {
	  ToDo
	}
	*/

	@Test
	public void test04_SiteDelete() throws Exception {
		Site siteDeleteDummy = getAppNGizerSite("deleteme", "deleteme", null, "http://deleteme:8080", "deleteme",
				false, true);
		postAndVerify("/site", null, siteDeleteDummy, HttpStatus.CREATED);
		deleteAndVerify("/site/deleteme", "", HttpStatus.NO_CONTENT);
	}

	@Test
	public void test05_SiteList() throws Exception {
		getAndVerify("/site", "xml/site-list.xml", HttpStatus.OK);
	}

	private Site getAppNGizerSite(String name, String host, String[] aliasNames, String domain, String description,
			boolean active, boolean repoPath) {
		Site site = new Site();
		site.setName(name);
		site.setHost(host);
		if (null != aliasNames) {
			Site.HostAliases hostAliases = new Site.HostAliases();
			for (String aliasName : aliasNames) {
				hostAliases.getAlias().add(aliasName);
			}
			site.setHostAliases(hostAliases);
		}
		site.setDomain(domain);
		site.setDescription(description);
		site.setActive(active);
		site.setCreateRepositoryPath(repoPath);
		return site;
	}
}