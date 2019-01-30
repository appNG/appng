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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.MultipartFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link WebListener} responsible for CSRF-protection.
 * 
 * @author Matthias Müller
 *
 * @see org.appng.api.Platform.Property#CSRF_FILTER_ENABLED
 * @see SiteProperties#CSRF_PROTECTION_ENABLED
 * @see SiteProperties#CSRF_PROTECTED_METHODS
 * @see SiteProperties#CSRF_PROTECTED_PATHS
 */
@Slf4j
@WebListener
public class CsrfSetupFilter implements ServletContextListener {

	public static final String CSRF_TOKEN = ".CSRF_TOKEN";
	private static final String CSRF_PARAM = "_csrf";
	private static final String SLASH_ALL = "/*";

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();

		Properties platformProps = DefaultEnvironment.get(context).getAttribute(Scope.PLATFORM,
				Platform.Environment.PLATFORM_CONFIG);

		if (platformProps.getBoolean(Platform.Property.CSRF_FILTER_ENABLED)) {
			try {
				LOGGER.info("initializing CSRF protection");
				String uploadDir = platformProps.getString(Platform.Property.UPLOAD_DIR);
				String uploadPath = context.getRealPath(uploadDir.startsWith("/") ? uploadDir : "/" + uploadDir);
				final MultipartResolver multipartResolver = new MultipartResolver(new FileSystemResource(uploadPath));

				MultipartFilter multipartFilter = new MultipartFilter() {
					protected MultipartResolver lookupMultipartResolver(HttpServletRequest request) {
						return multipartResolver;
					}
				};

				EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD);

				Dynamic dynMultipartFilter = context.addFilter("multipartFilter", multipartFilter);
				dynMultipartFilter.addMappingForUrlPatterns(dispatcherTypes, false, SLASH_ALL);

				CsrfFilter csrfFilter = new CsrfFilter(new TokenRepository());
				csrfFilter.setRequireCsrfProtectionMatcher(new SiteRequestMatcher());
				Dynamic dynCsrfFilter = context.addFilter("csrfFilter", csrfFilter);
				dynCsrfFilter.addMappingForUrlPatterns(dispatcherTypes, false, SLASH_ALL);

			} catch (IOException e) {
				throw new RuntimeException("error while initializing", e);
			}
		} else {
			LOGGER.info("'{}' is false, CSRF protection is disabled", Platform.Property.CSRF_FILTER_ENABLED);
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
	}

	class SiteRequestMatcher implements RequestMatcher {

		public boolean matches(HttpServletRequest request) {
			DefaultEnvironment env = DefaultEnvironment.get(request, null);
			Properties siteProps = RequestUtil.getSite(env, request).getProperties();
			if (siteProps.getBoolean(SiteProperties.CSRF_PROTECTION_ENABLED) && (siteProps
					.getList(SiteProperties.CSRF_PROTECTED_METHODS, ",").contains(request.getMethod().toUpperCase()))) {
				for (String protectedPath : siteProps.getList(SiteProperties.CSRF_PROTECTED_PATHS, ",")) {
					if (request.getServletPath().startsWith(protectedPath)) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("CSRF protection enabled for {} {}", request.getMethod(),
									request.getServletPath());
						}
						return true;
					}
				}
			}
			return false;
		}
	}

	class TokenRepository implements CsrfTokenRepository {

		public CsrfToken generateToken(HttpServletRequest request) {
			CsrfToken csrfToken = new DefaultCsrfToken(CSRF_TOKEN, CSRF_PARAM, UUID.randomUUID().toString());
			saveToken(csrfToken, request, null);
			return csrfToken;
		}

		public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
			(DefaultEnvironment.get(request.getSession())).setAttribute(Scope.SESSION, CSRF_TOKEN, token);
		}

		public CsrfToken loadToken(HttpServletRequest request) {
			return (DefaultEnvironment.get(request.getSession())).getAttribute(Scope.SESSION, CSRF_TOKEN);
		}
	}

	class MultipartResolver extends CommonsMultipartResolver {

		MultipartResolver(Resource uploadTempDir) throws IOException {
			super();
			setUploadTempDir(uploadTempDir);
		}

		@Override
		public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
			MultipartHttpServletRequest multipart = super.resolveMultipart(request);
			request.setAttribute(org.appng.forms.Request.REQUEST_PARSED, new MultipartRequest(multipart));
			return multipart;
		}
	}

	/**
	 * Implementation of {@link org.appng.forms.Request} that wraps a {@link MultipartHttpServletRequest}, which is the
	 * result of {@link org.springframework.web.multipart.MultipartResolver#resolveMultipart(HttpServletRequest)}.
	 */
	class MultipartRequest implements org.appng.forms.Request {

		private MultipartHttpServletRequest wrapped;
		private Map<String, String> additionalParams = new HashMap<String, String>();
		private String host;

		MultipartRequest(MultipartHttpServletRequest wrapped) {
			this.wrapped = wrapped;
			this.host = wrapped.getServerName();
		}

		public boolean hasParameter(String name) {
			return getParameter(name) != null;
		}

		public Map<String, List<String>> getParametersList() {
			Map<String, List<String>> parameters = new HashMap<String, List<String>>();
			for (String name : getParameterNames()) {
				parameters.put(name, getParameterList(name));
			}
			return Collections.unmodifiableMap(parameters);
		}

		public Map<String, String> getParameters() {
			Map<String, String> parameters = new HashMap<String, String>();
			for (String name : getParameterNames()) {
				parameters.put(name, getParameter(name));
			}
			return Collections.unmodifiableMap(parameters);
		}

		public Set<String> getParameterNames() {
			Set<String> names = new HashSet<>();
			Enumeration<String> parameterNames = wrapped.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				names.add(parameterNames.nextElement());
			}
			names.addAll(additionalParams.keySet());
			return Collections.unmodifiableSet(names);
		}

		public List<String> getParameterList(String name) {
			if (additionalParams.containsKey(name)) {
				return Arrays.asList(additionalParams.get(name));
			}
			String[] parameterValues = wrapped.getParameterValues(name);
			if (null != parameterValues) {
				return Arrays.asList(parameterValues);
			}
			return Collections.emptyList();
		}

		public String getParameter(String name) {
			if (additionalParams.containsKey(name)) {
				return additionalParams.get(name);
			}
			return wrapped.getParameter(name);
		}

		public String getHost() {
			return host;
		}

		public List<FormUpload> getFormUploads(String name) {
			List<FormUpload> uploads = new ArrayList<>();
			for (MultipartFile mf : wrapped.getFiles(name)) {
				if (!mf.isEmpty()) {
					uploads.add(getFormUpload(mf));
				}
			}
			return Collections.unmodifiableList(uploads);
		}

		private FormUpload getFormUpload(MultipartFile mf) {
			CommonsMultipartFile cmf = (CommonsMultipartFile) mf;
			DiskFileItem diskFileItem = (DiskFileItem) cmf.getFileItem();
			File file = diskFileItem.getStoreLocation();
			if (diskFileItem.isInMemory()) {
				try {
					diskFileItem.write(file);
				} catch (Exception e) {
					LOGGER.error(String.format("error writing %s", file.getAbsolutePath()), e);
				}
			}
			List<String> acceptedTypes = new ArrayList<>();
			FormUpload upload = new FormUploadBean(file, mf.getOriginalFilename(), mf.getContentType(), acceptedTypes,
					file.length());
			return upload;
		}

		public Map<String, List<FormUpload>> getFormUploads() {
			Map<String, List<FormUpload>> formuploads = new HashMap<String, List<FormUpload>>();
			Map<String, MultipartFile> fileMap = wrapped.getFileMap();
			for (String name : fileMap.keySet()) {
				formuploads.put(name, getFormUploads(name));
			}
			return Collections.unmodifiableMap(formuploads);
		}

		public void setTempDir(File tempDir) {
		}

		public void setMaxSize(long maxSize, boolean strict) {
		}

		public void setMaxSize(long maxSize) {
		}

		public void setEncoding(String encoding) {
		}

		public void setAcceptedTypes(String uploadName, String... types) {
		}

		public void process(HttpServletRequest httpServletRequest) {
		}

		public boolean isValid() {
			return true;
		}

		public boolean isPost() {
			return wrapped.getMethod().equalsIgnoreCase("POST");
		}

		public boolean isMultiPart() {
			return true;
		}

		public boolean isGet() {
			return wrapped.getMethod().equalsIgnoreCase("GET");
		}

		public HttpServletRequest getHttpServletRequest() {
			return wrapped;
		}

		public String getEncoding() {
			return wrapped.getCharacterEncoding();
		}

		public List<String> getAcceptedTypes(String uploadName) {
			return null;
		}

		public void addParameters(Map<String, String> parameters) {
			for (String name : parameters.keySet()) {
				addParameter(name, parameters.get(name));
			}
		}

		public void addParameter(String name, String value) {
			if (wrapped.getParameter(name) == null) {
				additionalParams.put(name, value);
			}
		}
	}
}