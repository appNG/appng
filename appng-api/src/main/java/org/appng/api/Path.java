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
package org.appng.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.UrlParams;

/**
 * A {@code Path} provides informations about the {@link Site} to which the current {@link HttpServletRequest} belongs
 * to. Therefore, the servlet-path returned by {@link HttpServletRequest#getServletPath()} is split into path-elements
 * using {@code /} as a separator.
 * 
 * @author Matthias Müller
 */
public interface Path {

	/**
	 * The separator for a path
	 */
	String SEPARATOR = "/";

	/**
	 * Returns {@code true} if this {@code Path} has an elements at index {@code idx}.
	 * 
	 * @param  idx
	 *             the zero-based index to check
	 * @return     {@code true} if there is such a path element,{@code false} otherwise
	 */
	boolean hasElementAt(int idx);

	/**
	 * Returns the last element of this {@code Path}.
	 * 
	 * @return the last element
	 */
	String getLastElement();

	/**
	 * Returns the element at index {@code idx}, if present
	 * 
	 * @param  idx
	 *             the zero-based index
	 * @return     the element at the given index, or {@code null} if no such element exists
	 */
	String getElementAt(int idx);

	/**
	 * Checks whether this {@code Path} has the given minimum number of elements and throws an {@link IOException}
	 * otherwise
	 * 
	 * @param  minLength
	 *                     the minimum length to check
	 * @throws IOException
	 *                     if this {@code Path} does not have the required minimum length
	 */
	void checkPathLength(int minLength) throws IOException;

	/**
	 * Checks whether this {@code Path} represents a static resource from a blob-directory
	 * 
	 * @return {@code true} if this {@code Path} represents a static resource, {@code false} otherwise
	 * @see    #getBlobDirectories()
	 */
	boolean isStaticContent();

	/**
	 * Checks whether this {@code Path} represents a document from a document-folder.
	 * 
	 * @return {@code true} if this {@code Path} represents a JSP file from a document-folder, {@code false} otherwise
	 * @see    #getDocumentDirectories()
	 */
	boolean isDocument();

	/**
	 * Checks whether this {@code Path} represents a call to the appNG webapplication GUI
	 * 
	 * @return {@code true} if this {@code Path} represents a call to the appNG webapplication GUI, {@code false}
	 *         otherwise
	 * @see    #getGuiPath()
	 */
	boolean isGui();

	/**
	 * Checks whether this {@code Path} represents a call to a service ({@link Webservice}/{@link AttachmentWebservice},
	 * {@link SoapService} or an XML/JSON call of an {@link ActionProvider} or {@link DataProvider}.
	 * 
	 * @return {@code true} if this {@code Path} represents a call to a service, {@code false} otherwise
	 */
	boolean isService();

	/**
	 * Checks whether this {@code Path} represents the call of a JSP file.
	 * 
	 * @return {@code true} if this {@code Path} represents the call of a JSP file, {@code false} otherwise
	 * @see    #getExtension()
	 * @see    #isDocument()
	 */
	boolean isJsp();

	/**
	 * Returns the name of the {@link Site} which is being addressed by this {@code Path}
	 * 
	 * @return the site name, if present
	 * @see    #hasSite()
	 */
	String getSiteName();

	/**
	 * Checks whether this {@code Path} contains an element determining the current {@link Site}
	 * 
	 * @return {@code true} if this {@code Path} contains an element determining the current {@link Site}, {@code false}
	 *         otherwise
	 */
	boolean hasSite();

	/**
	 * Returns the name of the {@link Application} which is being addressed by this {@code Path}
	 * 
	 * @return the application name, if present
	 * @see    #hasApplication()
	 */
	String getApplicationName();

	/**
	 * Checks whether this {@code Path} contains an element determining the current {@link Application}
	 * 
	 * @return {@code true} if this {@code Path} contains an element determining the current {@link Application},
	 *         {@code false} otherwise
	 */
	boolean hasApplication();

	/**
	 * Returns the name of the currently selected page, if present
	 * 
	 * @return the name of the page, or {@code null} if no page is selected
	 * @see    #isGui()
	 */
	String getPage();

	/**
	 * Returns the name of the currently selected action, if present
	 * 
	 * @return the name of the currently selected action, or {@code null} if no action is selected
	 * @see    #isGui()
	 * @see    #getActionValue()
	 */
	String getActionName();

	/**
	 * Returns the value of the currently selected action, if present
	 * 
	 * @return the value of the currently selected action, or {@code null} if no action is selected
	 * @see    #isGui()
	 * @see    #getActionName()
	 */
	String getActionValue();

	/**
	 * Checks whether this {@code Path} has an action selected
	 * 
	 * @return {@code true} if this {@code Path} , {@code false} otherwise
	 * @see    #getActionName()
	 * @see    #getActionValue()
	 */
	boolean hasAction();

	/**
	 * Returns a {@link List} containing the values of the {@link UrlParams} of a {@link Application}s page or
	 * service-call.
	 * 
	 * @return a {@link List} of parameter values
	 * @see    #isGui()
	 * @see    #isService()
	 */
	List<String> getApplicationUrlParameters();

	/**
	 * Returns a {@link List} containing the values of the JSP-Paramters of a JSP-call. Every parameter after the JSPs
	 * name is a JSP-Parameter. For example, if the current servletpath is {@code /en/index/foo/bar}, with {@code en}
	 * being a document folder and {@code index} the JSP name, the list will contain {@code "foo"} and {@code "bar"}.
	 * 
	 * @return a {@link List} of parameter values
	 * @see    #isJsp()
	 */
	List<String> getJspUrlParameters();

	/**
	 * Returns the root-path of this {@code Path}, which is the first element, or {@code /} if there is no first element
	 * 
	 * @return the root-path
	 */
	String getRootPath();

	/**
	 * Returns the host of the currently selected {@link Site}, if present
	 * 
	 * @return the host of the {@link Site}
	 * @see    #hasSite()
	 * @see    Site#getHost()
	 */
	String getHost();

	/**
	 * Returns the domain of the currently selected {@link Site}, if present
	 * 
	 * @return the domain of the {@link Site}
	 * @see    #hasSite()
	 * @see    Site#getDomain()
	 */
	String getDomain();

	/**
	 * Returns the complete servlet-path this {@code Path} was built from
	 * 
	 * @return the servlet-path
	 */
	String getServletPath();

	/**
	 * Returns the current path to the application, e.g. {@code /manager/mysite/myapp}
	 * 
	 * @return the current path to the application
	 */
	String getCurrentPath();

	/**
	 * Checks whether this {@code Path} represents the root-path
	 * 
	 * @return {@code true} if this {@code Path} represents the root-path, {@code false} otherwise
	 * @see    #getRootPath()
	 */
	boolean isRoot();

	/**
	 * Checks whether this {@code Path} represents the repository-folder of appNG
	 * 
	 * @return {@code true} if this {@code Path} represents the repository-folder of appNG, {@code false} otherwise
	 */
	boolean isRepository();

	/**
	 * Checks whether this {@code Path} represents a monitoring path
	 * 
	 * @return {@code true} if this {@code Path} represents a monitoring path, {@code false} otherwise
	 * @since  1.21.0
	 */
	default boolean isMonitoring() {
		return false;
	}

	/**
	 * Returns the path to the appNG webapplication.
	 * 
	 * @return the path to the appNG webapplication
	 * @see    #isGui()
	 */
	String getGuiPath();

	/**
	 * Returns a {@link List} of all directories which are being used to store
	 * <a href="http://en.wikipedia.org/wiki/Binary_large_object">BLOBs</a>
	 * 
	 * @return a {@link List} of all blob directories
	 */
	List<String> getBlobDirectories();

	/**
	 * Returns a {@link List} of all document directories, which contain the JSP-files
	 * 
	 * @return a {@link List} of all document directories
	 * @see    #isDocument()
	 * @see    #isJsp()
	 */
	List<String> getDocumentDirectories();

	/**
	 * Returns the complete absolute URL this {@code Path} represents, which is the {@link Site}s domain plus the
	 * servlet-path
	 * 
	 * @return the complete URL
	 * @see    #getDomain()
	 * @see    #getServletPath()
	 */
	String getPlatformUrl();

	/**
	 * Returns the ID of {@link OutputFormat}.<br />
	 * An {@link OutputFormat} may be defined by adding {@code _<format>} after the gui-path, e.g.
	 * {@code /ws/_html/site/application/page}.
	 * 
	 * @return the ID of the {@link OutputFormat}, if present
	 * @see    #isGui()
	 */
	String getOutputFormat();

	/**
	 * Returns the ID of {@link OutputType}.<br />
	 * An {@link OutputType} may be defined by adding {@code _<type>} after the output-format, e.g.
	 * {@code /ws/_html/_minimal/site/application/page}.
	 * 
	 * @return the ID of the {@link OutputType}, if present
	 * @see    #isGui()
	 * @see    #getOutputFormat()
	 */
	String getOutputType();

	/**
	 * Checks whether this {@code Path} has an {@link OutputFormat} set.
	 * 
	 * @return {@code true} if this {@code Path} has an {@link OutputFormat} set, {@code false} otherwise
	 * @see    #getOutputFormat()
	 */
	boolean hasOutputFormat();

	/**
	 * Checks whether this {@code Path} has an {@link OutputType} set.
	 * 
	 * @return {@code true} if this {@code Path} has an {@link OutputType} set, {@code false} otherwise
	 * @see    #getOutputType()
	 */
	boolean hasOutputType();

	/**
	 * Return the name of the service addressed by this {@code Path}
	 * 
	 * @return the name of the service, if present
	 * @see    #isService()
	 * @see    #getServicePath()
	 */
	String getService();

	/**
	 * Returns the path element that addresses a service
	 * 
	 * @return the path element that addresses a service, if present
	 * @see    #isService()
	 * @see    #getService()
	 */
	String getServicePath();

	/**
	 * Checks whether the given {@code servletPath} is being addressed by this {@code Path}, which basically means the
	 * servlet-path represented by this {@code Path} must start with the given {@code servletPath}.
	 * 
	 * @param  servletPath
	 *                     the servlet-path to check
	 * @return             {@code true} if this {@code Path} , {@code false} otherwise
	 * @see                Path#getServletPath()
	 */
	boolean isPathSelected(String servletPath);

	/**
	 * Returns the path prefix that contains the {@link OutputFormat} and {@link OutputType}, if those are set.
	 * 
	 * @return the path prefix
	 * @see    #getOutputFormat()
	 * @see    #getOutputType()
	 */
	String getOutputPrefix();

	/**
	 * Returns the file-extension for JSP-files
	 * 
	 * @return the file-extension
	 * @see    #isJsp()
	 */
	String getExtension();

	/**
	 * Return the index of the element representing the {@link Application} name
	 * 
	 * @return The index of the element representing the {@link Application} name
	 * @see    #getApplicationName()
	 * @see    #hasApplication()
	 * @see    #getElementAt(int)
	 */
	int getApplicationIndex();

	/**
	 * Returns the number of elements of this {@code Path}
	 * 
	 * @return the number of elements
	 */
	int getElementCount();

}
