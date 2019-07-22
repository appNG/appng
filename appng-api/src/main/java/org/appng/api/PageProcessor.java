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

/**
 * Interface that optionally can be implemented by a
 * <ul>
 * <li>{@link Taglet}
 * <li>{@link GlobalTaglet}
 * <li>{@link XMLTaglet}
 * <li>{@link GlobalXMLTaglet}
 * </ul>
 * 
 * If {@link #processPage()} returns {@code false}, the rest of the JSP-page will be skipped.
 * 
 * @author Matthias Müller
 * 
 * @see Taglet
 * @see GlobalTaglet
 * @see XMLTaglet
 * @see GlobalXMLTaglet
 */
public interface PageProcessor {

	/**
	 * Returns whether or not the rest of the JSP-page should be skipped
	 * @return {@code true} if the rest of the JSP-page should be skipped, {@code false} otherwise
	 */
	boolean processPage();

}
