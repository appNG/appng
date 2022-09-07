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

import javax.servlet.http.HttpServletRequest;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.support.environment.DefaultEnvironment;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class Home extends ControllerBase {

	static final String AUTHORIZED = "authorized";
	static final String ROOT = "/";

	@PostMapping(value = ROOT)
	public ResponseEntity<org.appng.appngizer.model.xml.Home> login(@RequestBody String sharedSecret,
			HttpServletRequest request) {
		String platformSecret = getSharedSecret();
		if (!platformSecret.equals(sharedSecret)) {
			LOGGER.info("invalid shared secret for session {}", session.getId());
			return reply(HttpStatus.FORBIDDEN);
		}
		session.setAttribute(AUTHORIZED, true);
		LOGGER.info("session {} has been authorized (user-agent: {})", session.getId(),
				request.getHeader(HttpHeaders.USER_AGENT));
		return welcome();
	}

	@GetMapping(value = ROOT)
	public ResponseEntity<org.appng.appngizer.model.xml.Home> welcome() {
		String appngVersion = DefaultEnvironment.getGlobal().getAttribute(Scope.PLATFORM,
				Platform.Environment.APPNG_VERSION);
		if (null == appngVersion) {
			return reply(HttpStatus.SERVICE_UNAVAILABLE);
		}
		boolean dbInitialized = getDatabaseStatus() != null;
		org.appng.appngizer.model.Home entity = new org.appng.appngizer.model.Home(appngVersion, dbInitialized);
		entity.applyUriComponents(getUriBuilder());
		return ok(entity);
	}

	Logger logger() {
		return LOGGER;
	}

}
