/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.core.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.servlets.DefaultServlet;
import org.apache.commons.io.output.NullOutputStream;

public class Tomcat7Support extends DefaultServlet implements Controller.Support {

	public void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String encoding)
			throws ServletException, IOException {
		super.serveResource(request, response, content);
	}

	public HttpServletResponse wrapResponseForHeadRequest(HttpServletResponse response) {
		return new HttpServletResponseWrapper(response) {
			@Override
			public ServletOutputStream getOutputStream() throws IOException {
				return new ServletOutputStream() {
					public void write(int b) throws IOException {
					}
				};
			}

			@Override
			public PrintWriter getWriter() throws IOException {
				return new PrintWriter(new NullOutputStream());
			}
		};
	}

}
