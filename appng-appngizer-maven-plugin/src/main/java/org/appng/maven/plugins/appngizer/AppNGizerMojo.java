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
package org.appng.maven.plugins.appngizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.appng.appngizer.model.xml.Home;
import org.appng.appngizer.model.xml.Package;
import org.appng.appngizer.model.xml.Repository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

abstract class AppNGizerMojo extends AbstractMojo {

	@Parameter(property = "sharedSecret", defaultValue = "loreipsum", required = true)
	protected String sharedSecret;

	@Parameter(property = "repository", defaultValue = "Local", required = true)
	protected String repository;

	@Parameter(property = "endpoint", defaultValue = "http://localhost:8080/appNGizer/", required = true)
	protected URL endpoint;

	protected File file;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	protected File targetFolder;

	@Parameter(defaultValue = "${project.artifactId}-${project.version}", readonly = true)
	protected String targetFile;

	@Parameter(property = "baseAuthUser")
	protected String baseAuthUser;

	@Parameter(property = "baseAuthPassword")
	protected String baseAuthPassword;

	protected RestTemplate restTemplate;

	private Jaxb2RootElementHttpMessageConverter jaxbConverter;

	private String cookie;

	protected AppNGizerMojo() {
		restTemplate = new RestTemplate();
		this.jaxbConverter = new Jaxb2RootElementHttpMessageConverter();
		restTemplate.getMessageConverters().add(jaxbConverter);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			protected boolean hasError(HttpStatus statusCode) {
				return statusCode.is5xxServerError();
			}
		});
	}

	protected <T> ResponseEntity<T> send(Object requestObject, HttpHeaders header, HttpMethod method, String path,
			Class<T> resultType) throws URISyntaxException {
		return send(requestObject, header, method, new URI(endpoint + path), resultType);
	}

	private <T> ResponseEntity<T> send(Object requestObject, HttpHeaders headers, HttpMethod method, URI path,
			Class<T> resultType) throws URISyntaxException {
		RequestEntity<?> req = new RequestEntity<>(requestObject, headers, method, path);
		getLog().debug("out: " + req);
		if (null != requestObject) {
			debugBody(requestObject, headers.getContentType());
		}
		ResponseEntity<T> response = restTemplate.exchange(req.getUrl(), req.getMethod(), req, resultType);
		getLog().debug("in: " + response);
		if (null != response.getBody()) {
			debugBody(response.getBody(), response.getHeaders().getContentType());
		}
		List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
		if (null != cookies) {
			cookie = cookies.stream().collect(Collectors.joining(";"));
		}
		return response;
	}

	protected void debugBody(Object o, MediaType mediaType) {
		if (getLog().isDebugEnabled() && MediaType.APPLICATION_XML.equals(mediaType)) {
			try {
				final OutputStream out = new ByteArrayOutputStream();
				jaxbConverter.write(o, mediaType, new HttpOutputMessage() {

					public HttpHeaders getHeaders() {
						return new HttpHeaders();
					}

					public OutputStream getBody() throws IOException {
						return out;
					}

				});
				getLog().debug(out.toString());
			} catch (IOException e) {
				getLog().warn("error writing body", e);
			}
		}
	}

	protected HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		if (StringUtils.isNotBlank(cookie)) {
			headers.set(HttpHeaders.COOKIE, cookie);
		}
		if (StringUtils.isNoneBlank(baseAuthUser, baseAuthPassword)) {
			String userAndPass = baseAuthUser + ":" + baseAuthPassword;
			byte[] encodedAuth = Base64.getEncoder().encode(userAndPass.getBytes(StandardCharsets.UTF_8));
			headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
		}
		return headers;
	}

	protected void login() throws URISyntaxException {
		HttpHeaders loginHeader = getHeader();
		loginHeader.setContentType(MediaType.TEXT_PLAIN);
		getLog().info("Connecting to " + endpoint);
		send(sharedSecret, loginHeader, HttpMethod.POST, endpoint.toURI(), Home.class);
	}

	protected ResponseEntity<Repository> getRepository() throws URISyntaxException {
		ResponseEntity<Repository> repo = send(null, getHeader(), HttpMethod.GET, "repository/" + repository,
				Repository.class);
		getLog().info("Retrieved repo " + repo.getBody().getName() + " at " + repo.getBody().getSelf());
		return repo;
	}

	protected ResponseEntity<Package> upload() throws URISyntaxException {
		MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
		multipartRequest.add("file", new FileSystemResource(file));
		HttpHeaders uploadHeader = getHeader();
		uploadHeader.setContentType(MediaType.MULTIPART_FORM_DATA);

		getLog().info("Uploading file " + file);

		return send(multipartRequest, uploadHeader, HttpMethod.POST, "repository/" + repository + "/upload",
				Package.class);
	}

	protected ResponseEntity<Void> install(Package uploadPackage, String cookie) throws URISyntaxException {
		getLog().info(String.format("Installing %s %s %s", uploadPackage.getName(), uploadPackage.getVersion(),
				uploadPackage.getTimestamp()));
		HttpHeaders installHeader = getHeader();
		installHeader.setContentType(MediaType.APPLICATION_XML);
		return send(uploadPackage, installHeader, HttpMethod.PUT, "repository/" + repository + "/install", Void.class);
	}

	protected void determineFile() throws MojoExecutionException {
		String[] files = targetFolder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(targetFile) && name.endsWith(".zip");
			}
		});
		if (null == files) {
			throw new MojoExecutionException(String.format("No archive file(s) starting with %s found in %s",
					targetFile, targetFolder.getAbsolutePath()));
		}
		List<String> sortedFiles = new ArrayList<String>(Arrays.asList(files));
		Collections.sort(sortedFiles);
		file = new File(targetFolder, sortedFiles.get(sortedFiles.size() - 1));

		getLog().info("Found archive " + file.getAbsolutePath());
	}

}