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
package org.appng.core.controller.filter;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.appng.core.model.ResponseType;

class ResponseWrapper extends HttpServletResponseWrapper implements HttpServletResponse {

	private CharArrayWriter buffer;
	private ResponseType responseType;
	private PrintWriter printWriter;

	public ResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (null == responseType) {
			responseType = ResponseType.BINARY;
		}
		return super.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (null == printWriter) {
			responseType = ResponseType.CHARACTER;
			buffer = new CharArrayWriter();
			printWriter = new PrintWriter(buffer);
		}
		return printWriter;
	}

	public String getContent() {
		buffer.flush();
		return buffer.toString();
	}

	public ResponseType getResponseType() {
		return responseType;
	}

	public boolean hasResponse() {
		return (null != responseType);
	}

}
