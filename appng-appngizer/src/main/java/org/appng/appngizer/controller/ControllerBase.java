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
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.model.Properties;
import org.appng.appngizer.model.xml.Error;
import org.appng.appngizer.model.xml.Errors;
import org.appng.appngizer.model.xml.Nameable;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.CoreService;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.TemplateService;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@RequestMapping(produces = { MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE })
public abstract class ControllerBase {

	@Autowired
	protected ServletContext context;

	@Autowired
	protected HttpSession session;

	@Autowired
	protected CoreService coreService;

	@Autowired
	protected TemplateService templateService;

	@Autowired
	protected DatabaseService databaseService;

	@Autowired
	protected AppNGizerConfigurer configurer;

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<Errors> onBusinessException(HttpServletRequest request, BusinessException e) {
		String message = String.format("[%s] error while processing [%s] on %s", request.getSession().getId(),
				request.getMethod(), request.getRequestURI());
		logger().error(message, e);
		Errors errors = new Errors();
		Error error = new Error();
		error.setPath(e.getClass().getName());
		error.setValue(e.getMessage());
		errors.getError().add(error);
		return new ResponseEntity<Errors>(errors, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Errors> onConstraintViolationException(HttpServletRequest request,
			ConstraintViolationException e) {
		String message = String.format("[%s] error while processing [%s] on %s", request.getSession().getId(),
				request.getMethod(), request.getRequestURI());
		logger().error(message, e);
		Errors errors = new Errors();
		Iterator<ConstraintViolation<?>> iterator = e.getConstraintViolations().iterator();
		while (iterator.hasNext()) {
			ConstraintViolation<?> cv = (ConstraintViolation<?>) iterator.next();
			Error error = new Error();
			error.setPath(cv.getPropertyPath().toString());
			error.setValue(cv.getMessage());
			errors.getError().add(error);
		}
		return new ResponseEntity<Errors>(errors, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<Errors> onConflictException(HttpServletRequest request, ConflictException e) {
		String message = String.format("[%s] error while processing [%s] on %s", request.getSession().getId(),
				request.getMethod(), request.getRequestURI());
		logger().error(message, e);
		Errors errors = new Errors();
		for (String cn : e.getConflicts()) {
			Error error = new Error();
			error.setValue(cn);
			errors.getError().add(error);
		}
		return new ResponseEntity<Errors>(errors, HttpStatus.CONFLICT);
	}

	abstract Logger logger();

	CoreService getCoreService() {
		return coreService;
	}

	TemplateService getTemplateService() {
		return templateService;
	}

	ApplicationImpl getApplicationByName(String name) {
		return (ApplicationImpl) getCoreService().findApplicationByName(name);
	}

	SiteImpl getSiteByName(String name) {
		return getCoreService().getSiteByName(name);
	}

	SiteApplication getSiteApplication(String site, String application) {
		return getCoreService().getSiteApplicationWithGrantedSites(site, application);
	}

	boolean nameChanged(Nameable nameable, String name) {
		return !nameable.getName().equals(name);
	}

	UriComponentsBuilder getUriBuilder() {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path("appNGizer");
	}

	<T> ResponseEntity<T> ok(T entity) {
		return new ResponseEntity<T>(entity, HttpStatus.OK);
	}

	<T> ResponseEntity<T> notFound() {
		return reply(HttpStatus.NOT_FOUND);
	}

	<T> ResponseEntity<T> seeOther(URI location) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(location);
		return reply(httpHeaders, HttpStatus.SEE_OTHER);
	}

	<T> ResponseEntity<T> movedPermanently(URI location) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(location);
		return reply(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
	}

	<T> ResponseEntity<T> created(T entity) {
		return reply(entity, HttpStatus.CREATED);
	}

	<T> ResponseEntity<T> conflict() {
		return reply(HttpStatus.CONFLICT);
	}

	<T> ResponseEntity<T> internalServerError() {
		return reply(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	<T> ResponseEntity<T> noContent(HttpHeaders headers) {
		return reply(headers, HttpStatus.NO_CONTENT);
	}

	<T> ResponseEntity<T> reply(HttpStatus status) {
		return new ResponseEntity<T>(status);
	}

	<T> ResponseEntity<T> reply(HttpHeaders headers, HttpStatus status) {
		return new ResponseEntity<T>(headers, status);
	}

	<T> ResponseEntity<T> reply(T entity, HttpStatus status) {
		return new ResponseEntity<T>(entity, status);
	}

	protected MigrationInfo getDatabaseStatus() {
		DatabaseConnection platformConnection = databaseService.getPlatformConnection(configurer.getProps());
		return platformConnection.getMigrationInfoService().current();
	}

	public String getSharedSecret() {
		Properties platformCfg = getCoreService().getPlatformProperties();
		return platformCfg.getString(Platform.Property.SHARED_SECRET);
	}

}
