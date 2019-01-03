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
package org.appng.core.controller.filter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.controller.filter.RedirectFilter.RedirectRule;
import org.appng.core.model.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Filter} that performs a search-and-replace on the given content of the {@link ServletResponse}.<br/>
 * It searches for paths (relative or absolute paths that match the {@link Site}'s domain) that end with ".jsp"
 * (respectively the configured file ending for JSPs) and removes the ".jsp" ending from that path.<br/>
 * Before:
 * 
 * <pre>
 * &lt;a href="/en/contact.jsp">Contact&lt;/a>
 * &lt;a href="http://foobar.org/en/index.jsp">Index&lt;/a>
 * &lt;a href="http://example.com/index.jsp">Example&lt;/a>
 * </pre>
 * 
 * </pre>
 * 
 * After, assuming the site's domain is 'http://foobar.org':
 * 
 * <pre>
 * &lt;a href="/en/contact">Contact&lt;/a>
 * &lt;a href="http://foobar.org/en/index">Index&lt;/a>
 * &lt;a href="http://example.com/index.jsp">Example&lt;/a>
 * </pre>
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 * 
 */
public class JspExtensionFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(JspExtensionFilter.class);
	private static final String PLATFORM_JSP_FILTER_SERVICE_CONTENT_TYPES = "jspFilterServiceContentTypes";
	private static final String PLATFORM_JSP_FILTER_SKIPPED_SERVICE_NAMES = "jspFilterSkippedServiceNames";
	private static final ConcurrentMap<String, Pattern> PATTERNS = new ConcurrentHashMap<String, Pattern>();
	private static final String DELIMITER = ",";
	private FilterConfig filterConfig;
	private String defaultServiceFilterTypes;
	// if prefix is '<site-domain>/' -> replace
	// if prefix is quote (") or singlequote (') -> replace
	// if prefix is not 'http://' -> replace
	private static final String DOMAIN_PATTERN = "(%s/|(?:\"|')(?!http://))(\\S+)\\%s";

	public JspExtensionFilter() {
		StringBuilder sb = new StringBuilder();
		sb.append(HttpHeaders.CONTENT_TYPE_TEXT_HTML);
		sb.append(DELIMITER);
		sb.append(HttpHeaders.CONTENT_TYPE_TEXT_PLAIN);
		sb.append(DELIMITER);
		sb.append(HttpHeaders.CONTENT_TYPE_TEXT_XML);
		sb.append(DELIMITER);
		sb.append(HttpHeaders.CONTENT_TYPE_APPLICATION_JSON);
		defaultServiceFilterTypes = sb.toString();
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ServletContext servletContext = filterConfig.getServletContext();
		Environment env = DefaultEnvironment.get(servletContext);
		String hostIdentifier = RequestUtil.getHostIdentifier(request, env);
		Site site = RequestUtil.getSiteByHost(env, hostIdentifier);
		String servletPath = ((HttpServletRequest) request).getServletPath();
		if (null != site) {
			Path pathInfo = RequestUtil.getPathInfo(env, site, servletPath);
			boolean isDocument = pathInfo.isDocument();

			if (!(isDocument || pathInfo.isService())) {
				chain.doFilter(request, response);
				return;
			}

			// Respect the client-specified character encoding
			// (see HTTP specification section 3.4.1)
			String encoding = site.getProperties().getString(Platform.Property.ENCODING);
			if (request.getCharacterEncoding() == null) {
				request.setCharacterEncoding(encoding);
			}
			response.setCharacterEncoding(encoding);

			ResponseWrapper wrapper = new ResponseWrapper((HttpServletResponse) response);
			chain.doFilter(request, wrapper);

			if (!response.isCommitted() && wrapper.hasResponse()) {
				Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
				if (wrapper.getResponseType().equals(ResponseType.CHARACTER)) {
					try (Writer writer = response.getWriter()) {
						String output = wrapper.getContent();
						if (isDocument || isFilterService(response, pathInfo, platformProperties)) {
							output = replaceUrls(site, platformProperties, servletPath, output);
							output = replaceOtherStuff(output);
						}
						writer.write(output);
					}
				}
			}
		}
	}

	private boolean isFilterService(ServletResponse response, Path pathInfo, Properties platformProperties) {
		boolean filterService = false;
		String contentType = response.getContentType();
		if (null != contentType && pathInfo.isService()) {
			contentType = contentType.split(";")[0];
			if (log.isTraceEnabled()) {
				log.trace("content type for '{}' is '{}'", pathInfo.getServletPath(), contentType);
			}
			List<String> filterServiceTypes = platformProperties.getList(PLATFORM_JSP_FILTER_SERVICE_CONTENT_TYPES,
					defaultServiceFilterTypes, DELIMITER);
			List<String> skippedServices = platformProperties.getList(PLATFORM_JSP_FILTER_SKIPPED_SERVICE_NAMES,
					"logViewer,threadViewer", DELIMITER);
			filterService = !skippedServices.contains(pathInfo.getService())
					&& filterServiceTypes.contains(contentType);
		}
		return filterService;
	}

	private String replaceUrls(Site site, Properties platformProperties, String servletPath, String content) {
		String jspType = platformProperties.getString(Platform.Property.JSP_FILE_TYPE);
		String jspExtension = "." + jspType;
		List<RedirectRule> redirectRules = RedirectFilter.getRedirectRules(site.getName());
		return doReplace(redirectRules, servletPath, site.getDomain(), jspExtension, content);
	}

	protected String doReplace(List<RedirectRule> redirectRules, String sourcePath, String domain, String jspExtension,
			String content) {
		long startTime = System.currentTimeMillis();
		int numRules = 0;
		if (null != redirectRules) {
			numRules = redirectRules.size();
			for (RedirectRule rule : redirectRules) {
				content = rule.apply(content);
				if (log.isTraceEnabled()) {
					log.trace("{} has been applied", rule);
				}
			}
		}

		if (content.contains(jspExtension)) {
			Pattern domainPattern = getDomainPattern(domain, jspExtension);
			content = domainPattern.matcher(content).replaceAll("$1$2");
			if (log.isTraceEnabled()) {
				log.trace("replace with pattern {} has been applied", domainPattern);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("handling JSP extensions for source '{}' took {}ms ({} redirect-rules processed)", sourcePath,
					System.currentTimeMillis() - startTime, numRules);
		}
		return content;
	}

	private Pattern getDomainPattern(String domain, String jspExtension) {
		String key = domain + jspExtension;
		Pattern pattern = PATTERNS.get(key);
		if (null == pattern) {
			pattern = Pattern.compile(String.format(DOMAIN_PATTERN, domain, jspExtension));
			PATTERNS.put(key, pattern);
		}
		return pattern;
	}

	private String replaceOtherStuff(String content) {
		content = content.replace("alt=\"__Visual__\"", "alt=\"Visual\"");
		content = content.replace("alt=\"__Visual__", "alt=\"");
		return content;
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void destroy() {
		PATTERNS.clear();
	}

}
