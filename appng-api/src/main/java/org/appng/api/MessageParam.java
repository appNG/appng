/*
 * Copyright 2011-2023 the original author or authors.
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

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A {@link MessageParam} provides a message key and optionally some message arguments which can be used when formatting
 * a message with {@link MessageFormat#format(String, Object...)}. The purpose is to make it easier to provide end-user
 * friendly messages.
 * 
 * @author Matthias Müller
 */
public interface MessageParam {
	/**
	 * Returns the key for the message in order to retrieve the real message from a {@link ResourceBundle}.
	 * 
	 * @return the key for this message
	 * 
	 * @see ResourceBundle#getString(String)
	 */
	String getMessageKey();

	/**
	 * Returns the arguments for this message in order to be used with {@link MessageFormat#format(String, Object...)}
	 * 
	 * @return the arguments for the message
	 */
	Object[] getMessageArgs();

}
