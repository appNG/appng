/*
 * Copyright 2011-2020 the original author or authors.
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.appng.core.controller.HttpHeaders;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * This {@link Filter} is a workaround for a bug in Microsoft Office which causes the http-session to get lost if a link
 * is being opened from inside a Microsoft Office document.<br/>
 * See <a href="http://support.microsoft.com/kb/899927">KB 899927</a> for details.
 * <p>
 * The solution is to send a <a href="http://www.w3.org/TR/html-markup/meta.http-equiv.refresh.html">meta refresh</a> if
 * MS Office is detected as user-agent.
 * </p>
 * To enable this filter, add this to the web.xml:
 * 
 * <pre>
 * &lt;filter>
 *  &lt;filter-name>MSOfficeUserAgentFilter&lt;/filter-name>
 *  &lt;filter-class>org.appng.core.controller.filter.MSOfficeUserAgentFilter&lt;/filter-class>
 * &lt;/filter>
 * &lt;filter-mapping>
 *  &lt;filter-name>MSOfficeUserAgentFilter&lt;/filter-name>
 *  &lt;servlet-name>controller&lt;/servlet-name>
 *  &lt;dispatcher>REQUEST&lt;/dispatcher>
 * &lt;/filter-mapping>
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class MSOfficeUserAgentFilter implements Filter {

	private static final String USER_AGENT_MS_OFFICE = "ms-office";
	private static final String HTML_META_REFRESH = "<html><head><meta http-equiv='refresh' content='0'/></head><body></body></html>";

	public void init(FilterConfig filterConfig) throws ServletException {

	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		String userAgent = ((HttpServletRequest) request).getHeader(HttpHeaders.USER_AGENT);
		if (userAgent.indexOf(USER_AGENT_MS_OFFICE) > 0) {
			LOGGER.info("{} was {}, sending meta-refresh", HttpHeaders.USER_AGENT, userAgent);
			response.getWriter().write(HTML_META_REFRESH);
			response.setContentType(HttpHeaders.CONTENT_TYPE_TEXT_XML);
			return;
		}
		chain.doFilter(request, response);
	}

	public void destroy() {

	}

}
