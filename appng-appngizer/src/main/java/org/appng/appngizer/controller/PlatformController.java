/*
 * Copyright 2011-2023 the original author or authors.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.appng.appngizer.model.Platform;
import org.appng.appngizer.model.Properties;
import org.slf4j.Logger;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PlatformController extends ControllerBase {

	@Override
	Logger logger() {
		return LOGGER;
	}

	@GetMapping(value = "/platform")
	public ResponseEntity<Platform> showPlatform() {
		boolean dbInitialized = getDatabaseStatus() != null;
		boolean platformReloadAvailable = getAppNGContext() != null;
		Platform platform = new Platform(dbInitialized, platformReloadAvailable);
		platform.applyUriComponents(getUriBuilder());
		return ok(platform);
	}

	@GetMapping(value = "/platform/system")
	public ResponseEntity<Properties> listSystemProperties() {
		Properties response = mapProps(System.getProperties());
		response.setSelf("/platform/system");
		return ok(response);
	}

	@GetMapping(value = "/platform/environment")
	public ResponseEntity<Properties> listEnvironment() {
		Properties response = mapProps(System.getenv());
		response.setSelf("/platform/environment");
		return ok(response);
	}

	@PostMapping(value = "/platform/reload")
	public ResponseEntity<Void> reloadPlatform() {
		Context appNGContext = getAppNGContext();
		logger().info("reloading {}", appNGContext);
		ExecutorService executor = Executors.newFixedThreadPool(1);
		FutureTask<Void> futureTask = new FutureTask<Void>(() -> {
			appNGContext.reload();
			return null;
		});
		executor.execute(futureTask);
		executor.shutdown();
		return seeOther(getUriBuilder().path("/platform").build().toUri());
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
