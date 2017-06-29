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

import java.util.Collections;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.appng.appngizer.model.Platform;
import org.appng.appngizer.model.Properties;
import org.slf4j.Logger;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PlatformController extends ControllerBase {

	@Override
	Logger logger() {
		return log;
	}

	@RequestMapping(value = "/platform", method = RequestMethod.GET)
	public ResponseEntity<Platform> showPlatform() {
		boolean dbInitialized = getDatabaseStatus() != null;
		boolean platformReloadAvailable = getAppNGContext() != null;
		Platform platform = new Platform(dbInitialized, platformReloadAvailable);
		platform.applyUriComponents(getUriBuilder());
		return ok(platform);
	}

	@RequestMapping(value = "/platform/system", method = RequestMethod.GET)
	public ResponseEntity<Properties> listSystemProperties() {
		Properties response = mapProps(System.getProperties());
		response.setSelf("/platform/system");
		return ok(response);
	}

	@RequestMapping(value = "/platform/environment", method = RequestMethod.GET)
	public ResponseEntity<Properties> listEnvironment() {
		Properties response = mapProps(System.getenv());
		response.setSelf("/platform/environment");
		return ok(response);
	}

	@RequestMapping(value = "/platform/reload", method = RequestMethod.POST)
	public ResponseEntity<Platform> reloadPlatform() {
		Context appNGContext = getAppNGContext();
		if (null == appNGContext) {
			logger().info("no appNG context found!");
			return notFound();
		} else {
			logger().info("reloading {}", appNGContext);
			appNGContext.reload();
			if (LifecycleState.STARTED.equals(appNGContext.getState())) {
				return showPlatform();
			} else {
				return internalServerError();
			}
		}
	}

	protected Context getAppNGContext() {
		Host host = (Host) context.getAttribute(AppNGizerServlet.HOST);
		return (Context) host.findChild("");
	}

	protected Properties mapProps(Map<?, ?> properties) {
		Properties response = new Properties();
		for (Object key : properties.keySet()) {
			org.appng.appngizer.model.Property prop = new org.appng.appngizer.model.Property();
			prop.setName(key.toString());
			prop.setValue(properties.get((String) key).toString());
			response.getProperty().add(prop);
		}
		Collections.sort(response.getProperty(), new PropertyComparator<>("name", true, true));
		return response;
	}

}
