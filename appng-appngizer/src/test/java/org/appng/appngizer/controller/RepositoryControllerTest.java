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
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.appng.appngizer.model.xml.PackageType;
import org.appng.appngizer.model.xml.Repository;
import org.appng.appngizer.model.xml.RepositoryMode;
import org.appng.appngizer.model.xml.RepositoryType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RepositoryControllerTest extends ControllerTest {

	@Test
	public void testCreateRetrieveInstallAndUpdate() throws Exception {
		Repository repo = new Repository();
		repo.setName("local repo");
		repo.setEnabled(true);
		repo.setStrict(false);
		repo.setPublished(false);
		repo.setMode(RepositoryMode.ALL);
		repo.setType(RepositoryType.LOCAL);
		repo.setUri(getUri());

		differenceListener.ignoreDifference("/repository[1]/uri[1]/text()[1]");
		postAndVerify("/repository", "xml/repository-create.xml", repo, HttpStatus.CREATED);
		postAndVerify("/repository", null, repo, HttpStatus.CONFLICT);

		getAndVerify("/repository/local%20repo/demo-application", "xml/repository-show-demo-app.xml", HttpStatus.OK);

		org.appng.appngizer.model.xml.Package install = new org.appng.appngizer.model.xml.Package();
		install.setName("demo-application");
		install.setVersion("1.5.3");
		install.setTimestamp("2013-01-13-1303");
		install.setDisplayName("Demo Application");
		install.setType(PackageType.APPLICATION);

		putAndVerify("/repository/local%20repo/install", null, install, HttpStatus.OK);

		getAndVerify("/repository/local%20repo/demo-application", "xml/repository-install-demo-app.xml", HttpStatus.OK);

		repo.setName("local");
		repo.setDescription("a local repository");
		putAndVerify("/repository/local%20repo", null, repo, HttpStatus.SEE_OTHER);

		getAndVerify("/repository/local", "xml/repository-update.xml", HttpStatus.OK);

	}

	@Test
	public void testDelete() throws Exception {
		Repository repo = new Repository();
		repo.setName("deleteme");
		repo.setEnabled(true);
		repo.setStrict(false);
		repo.setPublished(false);
		repo.setMode(RepositoryMode.ALL);
		repo.setType(RepositoryType.LOCAL);
		repo.setUri(getUri());

		postAndVerify("/repository", null, repo, HttpStatus.CREATED);
		deleteAndVerify("/repository/deleteme", "", HttpStatus.NO_CONTENT);
	}

	@Test
	public void testList() throws Exception {
		differenceListener.ignoreDifference("/repositories[1]/repository[1]/uri[1]/text()[1]");
		getAndVerify("/repository", "xml/repository-list.xml", HttpStatus.OK);
	}

	@Test
	public void testUploadAndDeletePackage() throws Exception {
		String name = "demo-application";
		String version = "1.5.3";
		String timestamp = "2013-01-13-1303";
		String originalFilename = String.format("%s-%s-%s.zip", name, version, timestamp);
		File file = new File(new File("").getAbsolutePath(),
				"../appng-core/src/test/resources/zip/" + originalFilename);
		MockMultipartHttpServletRequestBuilder post = MockMvcRequestBuilders
				.multipart(new URI("/repository/local/upload"));
		post.file(new MockMultipartFile("file", originalFilename, null, FileUtils.readFileToByteArray(file)));
		sendBodyAndVerify(post, null, HttpStatus.OK, "xml/archive-upload.xml");

		MockHttpServletRequestBuilder delete = MockMvcRequestBuilders
				.delete(new URI("/repository/local/" + name + "/" + version + "/" + timestamp));
		sendBodyAndVerify(delete, null, HttpStatus.OK, "xml/archive-delete.xml");
	}

	@Test
	public void testRetrieveInvalid() throws Exception {
		Repository repo = new Repository();
		repo.setName("dummy repo");
		repo.setEnabled(true);
		repo.setStrict(false);
		repo.setPublished(false);
		repo.setMode(RepositoryMode.ALL);
		repo.setType(RepositoryType.LOCAL);
		repo.setUri(getUri());
		postAndVerify("/repository", null, repo, HttpStatus.CREATED);

		getAndVerify("/repository/doesNotExist", null, HttpStatus.NOT_FOUND);
		getAndVerify("/repository/dummy%20repo/dummy", null, HttpStatus.NOT_FOUND);
		getAndVerify("/repository/doesNotExist/dummy", null, HttpStatus.NOT_FOUND);
		getAndVerify("/repository/dummy%20repo/demo-application/1.2.3", null, HttpStatus.NOT_FOUND);
		getAndVerify("/repository/doesNotExist/demo-application/1.2.3", null, HttpStatus.NOT_FOUND);
		getAndVerify("/repository/dummy%20repo/demo-application/1.5.2-SNAPSHOT/2011-01-13-1303", null,
				HttpStatus.NOT_FOUND);
		deleteAndVerify("/repository/dummy%20repo/demo-application/1.5.2-SNAPSHOT/2011-01-13-1303", null,
				HttpStatus.NOT_FOUND);
	}

}