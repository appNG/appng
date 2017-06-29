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

import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.springframework.web.servlet.DispatcherServlet;

public class AppNGizerServlet extends DispatcherServlet implements ContainerServlet {

	public static final String HOST = "host";
	private Wrapper wrapper;

	public Wrapper getWrapper() {
		return wrapper;
	}

	@Override
	public void setWrapper(Wrapper wrapper) {
		if (wrapper != null) {
			this.wrapper = wrapper;
			Context context = (Context) wrapper.getParent();
			Host host = (Host) context.getParent();
			context.getServletContext().setAttribute(HOST, host);
		}
	}

}
