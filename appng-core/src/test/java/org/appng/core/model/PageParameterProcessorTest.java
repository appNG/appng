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
package org.appng.core.model;

import static org.appng.api.Scope.SESSION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appng.api.Environment;
import org.appng.api.Request;
import org.appng.core.controller.TestSupport;
import org.appng.xml.platform.GetParams;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.ParamType;
import org.appng.xml.platform.PostParams;
import org.appng.xml.platform.UrlParams;
import org.appng.xml.platform.UrlSchema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PageParameterProcessorTest extends TestSupport {

	private static final String PARAM2 = "param2";
	private static final String HASH = "hash";
	private static final String FOO_ACTION = "foo";
	private static final String ACTION = "action";

	@Mock
	private Environment env;

	@Mock
	private Request request;

	private PageParameterProcessor ppp;

	private Set<String> sessionParamNames = new HashSet<>();

	private List<String> applicationUrlParameter = new ArrayList<>();

	private Map<String, String> sessionParams = new HashMap<>();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(request.getParameter(ACTION)).thenReturn(FOO_ACTION);
		ppp = new PageParameterProcessor("key", sessionParamNames, env, request, "");
		sessionParamNames.add(ACTION);
		Mockito.when(env.getAttribute(SESSION, "key")).thenReturn(sessionParams);
	}

	@Test
	public void testApplicationUrlParameter() throws Exception {
		Mockito.when(request.getParameter(ACTION)).thenReturn(null);
		Mockito.when(request.getParameter(PARAM2)).thenReturn("asasd");
		Mockito.when(request.isGet()).thenReturn(true);
		applicationUrlParameter.add("actionFromUrl");
		UrlSchema urlSchema = getUrlSchema();
		Param user = new Param();
		user.setName("user");
		urlSchema.getGetParams().getParamList().add(user);
		Param hash = new Param();
		hash.setName(HASH);
		urlSchema.getGetParams().getParamList().add(hash);
		sessionParams.put(HASH, "");
		sessionParamNames.add(HASH);
		boolean urlParamAdded = ppp.processPageParams(applicationUrlParameter, urlSchema);
		Mockito.verify(env, Mockito.atLeast(1)).getAttribute(SESSION, "key");
		Assert.assertFalse(urlParamAdded);
		validateXml(urlSchema, getClass().getSimpleName() + "-");
	}

	@Test
	public void testApplicationUrlParameterFromSession() throws Exception {
		Mockito.when(request.getParameter(ACTION)).thenReturn(null);
		Mockito.when(request.getParameter(PARAM2)).thenReturn("47");
		Mockito.when(request.isGet()).thenReturn(true);
		UrlSchema urlSchema = getUrlSchema();
		Param user = new Param();
		user.setName("user");
		urlSchema.getGetParams().getParamList().add(user);
		Param hash = new Param();
		hash.setName(HASH);
		urlSchema.getUrlParams().getParamList().add(hash);
		sessionParams.put(HASH, "hashFromSession");
		sessionParamNames.add(HASH);
		boolean urlParamAdded = ppp.processPageParams(applicationUrlParameter, urlSchema);
		Mockito.verify(env, Mockito.atLeast(1)).getAttribute(SESSION, "key");
		Assert.assertTrue(urlParamAdded);
		validateXml(urlSchema, getClass().getSimpleName() + "-");
	}

	@Test
	public void testGetBeforeUrl() throws Exception {
		Mockito.when(request.isGet()).thenReturn(true);
		UrlSchema urlSchema = getUrlSchema();
		Param user = new Param();
		user.setName("user");
		urlSchema.getGetParams().getParamList().add(user);
		Param hash = new Param();
		hash.setName(HASH);
		urlSchema.getGetParams().getParamList().add(hash);
		sessionParams.put(HASH, "");
		sessionParamNames.add(HASH);
		boolean urlParamAdded = ppp.processPageParams(applicationUrlParameter, urlSchema);
		Mockito.verify(env, Mockito.atLeast(1)).getAttribute(SESSION, "key");
		Assert.assertFalse(urlParamAdded);
		validateXml(urlSchema, getClass().getSimpleName() + "-");
	}

	@Test
	public void testPostOverGet() throws Exception {
		Mockito.when(request.isPost()).thenReturn(true);
		UrlSchema urlSchema = getUrlSchema();
		boolean urlParamAdded = ppp.processPageParams(applicationUrlParameter, urlSchema);
		Mockito.verify(env, Mockito.atLeast(1)).getAttribute(SESSION, "key");
		Assert.assertFalse(urlParamAdded);
		validateXml(urlSchema, getClass().getSimpleName() + "-");
	}

	@Test
	public void testSession() throws Exception {
		sessionParams.put(ACTION, "fooAction");
		sessionParamNames.add(ACTION);
		Mockito.when(request.getParameter(ACTION)).thenReturn(null);
		Mockito.when(request.isGet()).thenReturn(true);
		UrlSchema urlSchema = getUrlSchema();
		boolean urlParamAdded = ppp.processPageParams(applicationUrlParameter, urlSchema);
		Mockito.verify(env, Mockito.atLeast(1)).getAttribute(SESSION, "key");
		Assert.assertFalse(urlParamAdded);
		validateXml(urlSchema, getClass().getSimpleName() + "-");
	}

	private UrlSchema getUrlSchema() {
		UrlSchema urlSchema = new UrlSchema();

		GetParams getParams = new GetParams();
		Param getParam = new Param();
		getParam.setName(ACTION);
		getParams.getParamList().add(getParam);

		Param getParam2 = new Param();
		getParam2.setName(PARAM2);
		getParam2.setDefault("5");
		getParam2.setType(ParamType.INT);
		getParams.getParamList().add(getParam2);

		urlSchema.setGetParams(getParams);

		PostParams postParams = new PostParams();
		Param postParam = new Param();
		postParam.setName(ACTION);
		postParams.getParamList().add(postParam);
		urlSchema.setPostParams(postParams);

		UrlParams urlParams = new UrlParams();
		Param urlParam = new Param();
		urlParam.setName(ACTION);
		urlParam.setDefault("login");
		urlParams.getParamList().add(urlParam);
		urlSchema.setUrlParams(urlParams);
		return urlSchema;
	}
}
