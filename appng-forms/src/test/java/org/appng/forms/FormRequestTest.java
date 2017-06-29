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
package org.appng.forms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.appng.forms.impl.RequestBean;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class FormRequestTest {

	private static final String SESSION_ID = "5F2ACDC183D81BEFC73E7B7482ECEAA4";

	@Test
	public void testUploadFileByContentType() throws Exception {
		testMultipart("image/jpeg");
	}

	@Test
	public void testUploadFileByExtension() throws Exception {
		testMultipart("GIF", "JPG");
	}

	private void testMultipart(String... fileTypes) throws IOException, FileNotFoundException {
		String bar = "bar";
		String foo = "foo";
		String file = "file";
		String filename = "anonymous.jpg";
		String filenameComplete = "c:\\foo\bar\\" + filename;
		String contentType = "image/jpeg";

		Request formRequest = new RequestBean();
		HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(httpRequest.getCharacterEncoding()).thenReturn("UTF-8");
		Mockito.when(httpRequest.getHeader("Content-Encoding")).thenReturn("UTF-8");
		String boundary = "foobar";
		Mockito.when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary + "");
		Mockito.when(httpRequest.getMethod()).thenReturn("POST");

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getId()).thenReturn(SESSION_ID);
		Mockito.when(httpRequest.getSession()).thenReturn(session);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		StringBuilder request = new StringBuilder();
		request.append("--" + boundary + "\r\n");
		request.append("Content-Disposition: form-data; name=\"" + foo + "\"\r\n");
		request.append("\r\n");
		request.append(bar + "\r\n");

		request.append("--" + boundary + "\r\n");
		request.append("Content-Disposition: form-data; name=\"" + file + "\"; filename=\"" + filenameComplete
				+ "\"\r\n");
		request.append("Content-Type: " + contentType + "\r\n");
		request.append("\r\n");

		out.write(request.toString().getBytes());

		ClassLoader classLoader = FormRequestTest.class.getClassLoader();
		File originalFile = new File(classLoader.getResource("deathstar.jpg").getFile());
		InputStream is = new FileInputStream(originalFile);
		int read = -1;
		while ((read = is.read()) != -1) {
			out.write(read);
		}
		is.close();

		out.write(("\r\n--" + boundary + "--\r\n").getBytes());
		int size = out.size();

		byte[] byteArray = out.toByteArray();
		final InputStream in = new ByteArrayInputStream(byteArray);

		Mockito.when(httpRequest.getContentLength()).thenReturn(size);
		Mockito.when(httpRequest.getInputStream()).thenReturn(new ServletInputStream() {
			public int read() throws IOException {
				return in.read();
			}

			public boolean isFinished() {
				return true;
			}

			public boolean isReady() {
				return true;
			}

			public void setReadListener(ReadListener listener) {
			}
		});

		formRequest.setMaxSize(10551);

		formRequest.setAcceptedTypes(file, fileTypes);
		formRequest.process(httpRequest);

		Assert.assertEquals(true, formRequest.isMultiPart());

		Assert.assertEquals(bar, formRequest.getParametersList().get(foo).get(0));
		Assert.assertEquals(bar, formRequest.getParameter(foo));

		Map<String, List<FormUpload>> formUploads = formRequest.getFormUploads();
		Assert.assertEquals(1, formUploads.size());
		FormUpload formUpload = formRequest.getFormUploads(file).get(0);
		Assert.assertEquals(formUpload, formUploads.get(file).get(0));
		Assert.assertEquals(filename, formUpload.getOriginalFilename());
		Assert.assertEquals(true, formUpload.isValid());
		Assert.assertEquals(true, formUpload.isValidSize());

		Assert.assertEquals(true, FileUtils.contentEquals(originalFile, formUpload.getFile()));
		Assert.assertEquals(contentType, formUpload.getContentType());
	}

	@Test
	public void testGet() throws Exception {
		String foo = "foo";
		String bar = "bar";
		String answer = "42";

		final Map<String, List<String>> requestParameters = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add(bar);
		values.add(answer);
		requestParameters.put(foo, values);

		Request formRequest = new RequestBean();
		HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(httpRequest.getCharacterEncoding()).thenReturn("UTF-8");
		Mockito.when(httpRequest.getHeader("Content-Encoding")).thenReturn("UTF-8");
		Mockito.when(httpRequest.getMethod()).thenReturn("GET");

		Mockito.when(httpRequest.getParameterValues(Mockito.anyString())).thenAnswer(new Answer<String[]>() {
			public String[] answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				List<String> list = requestParameters.get(name);
				return list.toArray(new String[list.size()]);
			}
		});
		final Iterator<String> it = requestParameters.keySet().iterator();
		Mockito.when(httpRequest.getParameterNames()).thenReturn(new Enumeration<String>() {
			public String nextElement() {
				return it.next();
			}

			public boolean hasMoreElements() {
				return it.hasNext();
			}
		});

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getId()).thenReturn(SESSION_ID);
		Mockito.when(httpRequest.getSession()).thenReturn(session);

		formRequest.process(httpRequest);

		Assert.assertEquals(false, formRequest.isMultiPart());
		Assert.assertEquals(0, formRequest.getFormUploads().size());
		Assert.assertEquals(bar, formRequest.getParameter(foo));
		Assert.assertEquals(values, formRequest.getParameterList(foo));
	}
}
