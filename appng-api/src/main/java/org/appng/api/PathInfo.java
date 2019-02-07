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
package org.appng.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * 
 * Default {@link Path}-implementation
 * 
 * @author Matthias Müller
 * 
 */
public class PathInfo implements Path {

	private static final String OUTPUT_PREFIX = "_";
	private static final String DOT = ".";
	private final String guiPath;
	private final String servicePath;
	private final List<String> blobDirectories;
	private final List<String> documentDirectories;
	private String repositoryPath;
	private final String host;
	private String rootPath;
	private String servletPath;
	private String applicationName;
	private String actionName;
	private String actionValue;
	private String currentSite;
	private List<String> pathElements;
	private List<String> jspUrlParams = null;

	private int siteIdx = -1;
	private int applicationIdx = -1;
	private int pageIdx = -1;
	private String page;
	private int rootIdx;
	private String extension;
	private String domain;

	/**
	 * Creates a new {@link PathInfo}
	 * 
	 * @param host
	 *            the host of the current {@link org.appng.api.model.Site}
	 * @param domain
	 *            the domain of the current {@link org.appng.api.model.Site}
	 * @param currentSite
	 *            the name of the current {@link org.appng.api.model.Site}
	 * @param servletPath
	 *            the current servletPath, as returned by {@link javax.servlet.http.HttpServletRequest#getServletPath()}
	 * @param guiPath
	 *            value of the property {@value org.appng.api.SiteProperties#MANAGER_PATH} of the current
	 *            {@link org.appng.api.model.Site}
	 * @param servicePath
	 *            value of the property {@value org.appng.api.SiteProperties#SERVICE_PATH} of the current
	 *            {@link org.appng.api.model.Site}
	 * @param blobDirectories
	 *            a list parsed from the property {@value org.appng.api.SiteProperties#ASSETS_DIR} of the current
	 *            {@link org.appng.api.model.Site}
	 * @param documentDirectories
	 *            a list parsed from the property {@value org.appng.api.SiteProperties#DOCUMENT_DIR} of the current
	 *            {@link org.appng.api.model.Site}
	 * @param repositoryPath
	 *            value of the platform property {@value org.appng.api.Platform.Property#REPOSITORY_PATH}
	 * @param extension
	 *            value of the platform property {@value org.appng.api.Platform.Property#JSP_FILE_TYPE}
	 */
	public PathInfo(String host, String domain, String currentSite, String servletPath, String guiPath,
			String servicePath, List<String> blobDirectories, List<String> documentDirectories, String repositoryPath,
			String extension) {
		this.host = host;
		this.domain = domain;
		this.currentSite = currentSite;
		this.servletPath = servletPath;
		this.guiPath = guiPath;
		this.servicePath = servicePath;
		this.blobDirectories = blobDirectories;
		this.documentDirectories = documentDirectories;
		this.repositoryPath = repositoryPath;
		this.extension = extension;
		parse();
	}

	private void parse() {
		if (null == servletPath) {
			this.servletPath = "";
		}
		this.pathElements = splitPath(servletPath);

		this.rootIdx = 1;
		if (hasElementAt(rootIdx)) {
			this.rootPath = SEPARATOR + pathElements.get(rootIdx);
		} else {
			this.rootPath = servletPath;
		}
		if (isGui()) {
			int idx = pathElements.indexOf(guiPath.substring(1));
			if (hasElementAt(idx)) {
				if (hasElementAt(idx + 1)) {
					siteIdx = idx + 1;
					if (pathElements.get(siteIdx).startsWith(OUTPUT_PREFIX)) {
						siteIdx += 1;
						if (pathElements.get(siteIdx).startsWith(OUTPUT_PREFIX)) {
							siteIdx += 1;
						}
					}

					if (hasElementAt(siteIdx + 1)) {
						applicationIdx = siteIdx + 1;
						if (hasElementAt(applicationIdx + 1)) {
							pageIdx = applicationIdx + 1;
						}
					}
				}
			}
		} else if (isService()) {
			int idx = pathElements.indexOf(servicePath.substring(1));
			if (hasElementAt(idx + 1)) {
				this.siteIdx = idx + 1;
			}
			if (hasElementAt(idx + 2)) {
				this.applicationIdx = idx + 2;
			}
		}
	}

	private List<String> splitPath(String path) {
		List<String> elements = new ArrayList<>(Arrays.asList(path.split(SEPARATOR)));
		if (elements.size() > 0) {
			int lastIdx = elements.size() - 1;
			String lastElement = elements.get(lastIdx);
			int anchorIndex = lastElement.indexOf('#');
			if (anchorIndex > 0) {
				elements.remove(lastIdx);
				elements.add(lastElement.substring(0, anchorIndex));
			}
		}
		return elements;
	}

	public boolean hasElementAt(int idx) {
		return idx > -1 && idx < pathElements.size();
	}

	public String getLastElement() {
		return getElementAt(getElementCount() - 1);
	}

	public String getElementAt(int idx) {
		if (hasElementAt(idx)) {
			return pathElements.get(idx);
		}
		return null;
	}

	public void checkPathLength(int minLength) throws IOException {
		if (pathElements.size() < minLength) {
			throw new IOException("invalid path");
		}
	}

	public boolean isStaticContent() {
		return blobDirectories.contains(rootPath);
	}

	public boolean isDocument() {
		return documentDirectories.contains(rootPath);
	}

	public boolean isGui() {
		return hasElementAt(1) && pathElements.get(1).equals(guiPath.substring(1));
	}

	public boolean isService() {
		return hasElementAt(1) && pathElements.get(1).equals(servicePath.substring(1));
	}

	public boolean isJsp() {
		return servletPath != null && servletPath.endsWith(DOT + extension);
	}

	public String getSiteName() {
		if (hasSite()) {
			return pathElements.get(siteIdx);
		} else {
			return currentSite;
		}
	}

	public boolean hasSite() {
		return hasElementAt(siteIdx);
	}

	/**
	 * Manually sets the name of the selected {@link Application}
	 * 
	 * @param application
	 *            the name of the {@link Application}
	 */
	public void setApplicationName(String application) {
		this.applicationName = application;
		this.applicationIdx = pathElements.indexOf(application);
		this.pageIdx = -1;
	}

	public String getApplicationName() {
		if (null != applicationName) {
			return applicationName;
		} else if (hasApplication()) {
			return pathElements.get(applicationIdx);
		}
		return null;
	}

	public boolean hasApplication() {
		return hasElementAt(applicationIdx);
	}

	/**
	 * Manually sets the page within a {@link Application}
	 * 
	 * @param page
	 *            the page to set
	 */
	public void setPage(String page) {
		this.page = page;
		this.pageIdx = pathElements.indexOf(page);
	}

	public String getPage() {
		if (null == this.page) {
			if (hasElementAt(pageIdx)) {
				return pathElements.get(pageIdx);
			}
		}
		return page;
	}

	/**
	 * Manually sets the name and value for an action within a {@link Application}
	 * 
	 * @param actionName
	 *            the name of the action
	 * @param actionValue
	 *            the value for the action
	 */
	public void setAction(String actionName, String actionValue) {
		this.actionName = actionName;
		this.actionValue = actionValue;
	}

	public String getActionName() {
		return actionName;
	}

	public String getActionValue() {
		return actionValue;
	}

	public boolean hasAction() {
		return StringUtils.isNotEmpty(actionName) && StringUtils.isNotEmpty(actionValue);
	}

	private String initJspUrlParameters(File wwwRootFile) {
		String realServletPath = servletPath;
		PathSegmenter segmenter = new PathSegmenter(servletPath);
		for (int i = 1; i <= segmenter.size(); i++) {
			realServletPath = segmenter.getSegments(i);
			File jspFile = new File(wwwRootFile, realServletPath + DOT + extension);
			if (jspFile.exists()) {
				jspUrlParams = segmenter.getSegmentsList(i, segmenter.size());
				realServletPath += DOT + extension;
				break;
			}
		}
		return realServletPath;
	}

	public List<String> getApplicationUrlParameters() {
		if (isGui()) {
			if (hasElementAt(pageIdx) && hasElementAt(pageIdx + 1)) {
				List<String> params = pathElements.subList(pageIdx + 1, pathElements.size());
				return params;
			}
		} else if (isService()) {
			if (hasElementAt(applicationIdx) && hasElementAt(applicationIdx + 3)) {
				List<String> params = pathElements.subList(applicationIdx + 3, pathElements.size());
				return params;
			}
		}
		return new ArrayList<>(0);
	}

	public List<String> getJspUrlParameters() {
		if (null == jspUrlParams) {
			jspUrlParams = new ArrayList<>();
		}
		return jspUrlParams;
	}

	public String getRootPath() {
		return rootPath;
	}

	public String getHost() {
		return host;
	}

	public String getDomain() {
		return domain;
	}

	public String getServletPath() {
		return servletPath;
	}

	public String getCurrentPath() {
		if (isGui() && isRoot()) {
			String result = rootPath + SEPARATOR + getSiteName();
			if (getApplicationName() != null) {
				result += SEPARATOR + getApplicationName();
			}
			return result;
		}
		return getServletPath();
	}

	public boolean isRoot() {
		return servletPath.equals(rootPath);
	}

	public boolean isRootIgnoreTrailingSlash() {
		if (isRoot() || (servletPath.equals(rootPath + SEPARATOR))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isRepository() {
		return servletPath.startsWith(SEPARATOR + repositoryPath);
	}

	public String getGuiPath() {
		return guiPath;
	}

	public List<String> getBlobDirectories() {
		return blobDirectories;
	}

	public List<String> getDocumentDirectories() {
		return documentDirectories;
	}

	public String getPlatformUrl() {
		return domain + servletPath;
	}

	public String getOutputFormat() {
		if (isGui()) {
			if (siteIdx - rootIdx > 1) {
				return pathElements.get(rootIdx + 1).substring(1);
			}
		}
		return null;
	}

	public String getOutputType() {
		if (isGui()) {
			if (siteIdx - rootIdx > 2) {
				return pathElements.get(rootIdx + 2).substring(1);
			}
		}
		return null;
	}

	public boolean hasOutputFormat() {
		return null != getOutputFormat();
	}

	public boolean hasOutputType() {
		return null != getOutputType();
	}

	public String getService() {
		if (isService()) {
			if (hasElementAt(applicationIdx) && hasElementAt(applicationIdx + 2)) {
				String parameters = pathElements.get(applicationIdx + 2);
				int indexOf = parameters.indexOf("?");
				if (indexOf > 0) {
					return parameters.substring(0, indexOf);
				}
				return parameters;
			}
		}
		return null;
	}

	public String getServicePath() {
		return servicePath;
	}

	public boolean isPathSelected(String path) {
		List<String> pathChunks = splitPath(path);
		if (getElementCount() >= pathChunks.size()) {
			for (int i = 0; i < pathChunks.size(); i++) {
				if (!pathChunks.get(i).equals(getElementAt(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public String getOutputPrefix() {
		String prefix = "";
		if (hasOutputFormat()) {
			prefix += SEPARATOR + OUTPUT_PREFIX + getOutputFormat();
			if (hasOutputType()) {
				prefix += SEPARATOR + OUTPUT_PREFIX + getOutputType();
			}
		}
		return prefix;
	}

	public String getExtension() {
		return extension;
	}

	/**
	 * Builds the the path to which a {@link HttpServletRequest} has to be forwarded to in order the retrieve a file
	 * from a document directory. If the requested fiel is a JSP, the JSP Url-Parameters are being initialized.
	 * 
	 * @param wwwRootPath
	 *            the relative path to a {@link Site}s web-folder, under which the document-folders reside
	 * @param wwwRootFile
	 *            a file representing the very same relative path
	 * @return the forward path
	 * @see #isDocument()
	 * @see #getDocumentDirectories()
	 * @see #isJsp()
	 */
	public String getForwardPath(String wwwRootPath, File wwwRootFile) {
		String realServletPath = initJspUrlParameters(wwwRootFile);
		return wwwRootPath + realServletPath;
	}

	public int getApplicationIndex() {
		return applicationIdx;
	}

	public int getElementCount() {
		return pathElements.size();
	}

	class PathSegmenter {

		private final List<String> segments;

		public PathSegmenter(String servletPath) {
			this.segments = Arrays.asList(servletPath.split(SEPARATOR));
		}

		public int size() {
			return segments.size();
		}

		public List<String> getSegmentsList(int fromIndex, int toIndex) {
			return segments.subList(fromIndex, toIndex);
		}

		public String getSegments(int index) {
			StringBuffer segmentString = new StringBuffer();
			getSegmentsList(1, index).forEach(segment -> segmentString.append(SEPARATOR + segment));
			return 0 == segmentString.length() ? SEPARATOR : segmentString.toString();
		}
	}
}
