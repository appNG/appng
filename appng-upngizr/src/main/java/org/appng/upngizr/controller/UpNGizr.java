/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.upngizr.controller;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.springframework.web.servlet.DispatcherServlet;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link DispatcherServlet} that implements {@link ContainerServlet}.
 * 
 * @author Matthias Müller
 *
 */
@Slf4j
public class UpNGizr extends DispatcherServlet implements ContainerServlet, ServletContextListener {

	static final String HOST = "host";
	static String appNGHome;
	static String appNGizerHome;
	private Wrapper wrapper;

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext servletContext = sce.getServletContext();
		String contextPath = servletContext.getRealPath("");
		String webappsDir = new File(contextPath).getParent();
		appNGHome = new File(webappsDir, "ROOT").getAbsolutePath();
		appNGizerHome = new File(webappsDir, "appNGizer").getAbsolutePath();
	}

	public void contextDestroyed(ServletContextEvent sce) {
	}

	public Wrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(Wrapper wrapper) {
		if (wrapper != null) {
			this.wrapper = wrapper;
			Context context = (Context) wrapper.getParent();
			Host host = (Host) context.getParent();
			log.info("Host: {}", host);
			context.getServletContext().setAttribute(HOST, host);
		}
	}

}
