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

import java.net.URI;

import org.appng.appngizer.model.xml.Site;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SiteControllerTest extends ControllerTest {

	@Test
	public void test01_SiteCreate() throws Exception {
		// Null where xsd allows it.
		Site siteNoOptionals = getAppNGizerSite("naked1", "nakedhost", null, "http://nakedhost:8081", null, false,
				true);
		postAndVerify("/site", "xml/site-create-nakedhost.xml", siteNoOptionals, HttpStatus.CREATED);

		// A regular site (not active yet).
		Site siteRegular = getAppNGizerSite("regular1", "regularhost",
				new String[] { "KillEmAllHost", "RidetheLightningHost", "MasterofPuppetsHost" },
				"http://regularhost:8081", "36:38 regular fit", false, true);
		postAndVerify("/site", "xml/site-create-regularhost.xml", siteRegular, HttpStatus.CREATED);
	}

	@Test
	public void test02_SiteConflicts() throws Exception {
		// Post the same site again. Should trigger conflict tests.
		Site siteNameConflict = getAppNGizerSite("regular1", "regularhost", null, "http://regularhost:8081", null, true,
				true);
		sendAndVerify(MockMvcRequestBuilders.post(new URI("/site")), siteNameConflict, HttpStatus.CONFLICT,
				"xml/site-test-sitename-conflict.xml");
		// New Site with conflict between aliases. (Only one combination of
		// Alias/Host<-conflict->Alias/Host. Rest is tested in Manager.)
		Site siteAliasConflict = getAppNGizerSite("newnamehost", "newnamehost",
				new String[] { "RidetheLightningHost", "MasterofPuppetsHost" }, "http://newnamehost:8081", null, true,
				true);
		sendAndVerify(MockMvcRequestBuilders.post(new URI("/site")), siteAliasConflict, HttpStatus.CONFLICT,
				"xml/site-test-alias-conflict.xml");
	}

	@Test
	public void test03_SiteUpdate() throws Exception {
		Site siteUpdate = getAppNGizerSite("naked1", "NotSoNakedAnyMoreHost", new String[] { "CoverMeInAliasesHost" },
				"http://nakedhost:8081", "Descriptive clothing", true, true);
		putAndVerify("/site/naked1", "xml/site-update-nakedhost.xml", siteUpdate, HttpStatus.OK);
	}

	@Test
	public void test04_SiteDelete() throws Exception {
		Site siteDeleteDummy = getAppNGizerSite("deleteme", "deleteme", null, "http://deleteme:8080", "deleteme", false,
				true);
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