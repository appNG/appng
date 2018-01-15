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
package org.appng.taglib;

import static org.appng.api.Scope.PLATFORM;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.appng.api.Environment;
import org.appng.api.GlobalTaglet;
import org.appng.api.GlobalXMLTaglet;
import org.appng.api.Platform;
import org.appng.api.Taglet;
import org.appng.api.XMLTaglet;
import org.appng.api.model.Application;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationProvider;
import org.appng.forms.Request;
import org.appng.forms.impl.RequestBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * This class wraps an appNG taglet ( {@link Taglet}/{@link GlobalTaglet}/{@link XMLTaglet}/{@link GlobalXMLTaglet} )
 * into an {@link javax.servlet.jsp.tagext.Tag}.
 * <p/>
 * <b>Attributes:</b>
 * <ul>
 * <li>application - the name of the {@link Application} that contains the taglet</li>
 * <li>method - the bean name of the taglet</li>
 * <li>type - the type of the taglet, one of {@code text} or {@code xml}</li>
 * </ul>
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:taglet application="acme" method="tagletname">
 *   &lt;appNG:param name="foo">bar&lt;/appNG:param>
 *   &lt;appNG:param name="jin">fizzbar&lt;/appNG:param>
 * &lt;/appNG:taglet>
 * </pre>
 * 
 * @see Taglet
 * @see GlobalTaglet
 * @see XMLTaglet
 * @see GlobalXMLTaglet
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
public class TagletAdapter extends BodyTagSupport implements ParameterOwner {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(TagletAdapter.class);
	private static final String TAGLET_PROCESSOR = "tagletProcessor";
	private String application;
	private String method;
	private String type;
	private Map<String, String> tagletAttributes;

	/**
	 * set the application name
	 * 
	 * @param application
	 */
	public void setApplication(String application) {
		this.application = application;
	}

	/**
	 * set the method name
	 * 
	 * @param method
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * set the taglet type
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void setPageContext(PageContext pageContext) {
		super.setPageContext(pageContext);
	}

	public static Request getRequest(PageContext pageContext) {
		Request request = (Request) pageContext.getRequest().getAttribute(Request.REQUEST_PARSED);
		if (null == request) {
			request = new RequestBean();
			request.process((HttpServletRequest) pageContext.getRequest());
		}
		return request;
	}

	@Override
	public int doStartTag() throws javax.servlet.jsp.JspException {
		tagletAttributes = new HashMap<String, String>();
		return super.doStartTag();
	}

	@Override
	public final int doEndTag() {
		boolean processPage = true;
		HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();

		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		ApplicationProvider applicationProvider = null;
		try {
			Environment environment = getEnvironment();
			ApplicationContext ctx = environment.getAttribute(PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);

			if (ctx.containsBean(TAGLET_PROCESSOR)) {
				MultiSiteSupport multiSiteSupport = getMultiSiteSupport(servletRequest, environment);
				SiteImpl callingSite = multiSiteSupport.getCallingSite();
				SiteImpl executingSite = multiSiteSupport.getExecutingSite();
				applicationProvider = multiSiteSupport.getApplicationProvider();
				HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
				ApplicationRequest applicationRequest = applicationProvider.getApplicationRequest(servletRequest,
						servletResponse, true);
				applicationProvider.setPlatformScope();
				if (null == type) {
					type = "text";
				}
				Thread.currentThread().setContextClassLoader(executingSite.getSiteClassLoader());
				TagletProcessor processor = ctx.getBean(TAGLET_PROCESSOR, TagletProcessor.class);
				StringWriter out = new StringWriter();
				processPage = processor.perform(callingSite, executingSite, applicationProvider, tagletAttributes,
						applicationRequest, method, type, out);
				pageContext.getOut().write(out.toString());
			} else {
				log.debug("No such bean: {}", TAGLET_PROCESSOR);
			}
		} catch (Exception ex) {
			log.error("Unable to load Taglet '" + method + "' in application '" + application + "' (path: "
					+ servletRequest.getRequestURI() + " )", ex);
		} finally {
			if (null != applicationProvider) {
				applicationProvider.setPlatformScope(true);
			}
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
		tagletAttributes.clear();
		application = null;
		method = null;
		type = null;
		tagletAttributes = null;
		return processPage ? EVAL_PAGE : SKIP_PAGE;
	}

	protected MultiSiteSupport getMultiSiteSupport(HttpServletRequest servletRequest, Environment environment)
			throws JspException {
		MultiSiteSupport multiSiteSupport = new MultiSiteSupport();
		multiSiteSupport.process(environment, application, method, servletRequest);
		return multiSiteSupport;
	}

	protected Environment getEnvironment() {
		return DefaultEnvironment.get(pageContext);
	}

	public void addParameter(String name, String value) {
		tagletAttributes.put(name, value);
	}

}
