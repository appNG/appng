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

/**
 * Enhancement of a {@link Webservice} which is used to provide a file as an attachment. It {@link #isAttachment()}
 * returns {@code true}, the response-header 'Content-Disposition' is being set. Otherwise the client will try to open
 * the file inline.
 * 
 * @author Matthias Müller
 * 
 */
public interface AttachmentWebservice extends Webservice {

	/**
	 * Returns the name of the file to open
	 * 
	 * @return the name of the file
	 */
	String getFileName();

	/**
	 * Tells the client whether to treat the file as an attachment
	 * 
	 * @return {@code true} if the file should be treated as an attachment
	 */
	boolean isAttachment();

}
