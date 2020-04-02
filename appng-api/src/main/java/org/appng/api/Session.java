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

import java.util.Locale;
import java.util.TimeZone;

import javax.security.auth.Subject;

import org.appng.xml.platform.Messages;

/**
 * 
 * Utility-class providing constants used for accessing an {@link Environment}-attribute with {@link Scope#SESSION}.
 * 
 * @author Matthias Müller
 * 
 */
public final class Session {

	private Session() {

	}

	public static class Environment {

		/**
		 * The start time of the current session as unix-time, return type is {@code long}
		 */
		public static final String STARTTIME = "starttime";
		/**
		 * The timeout of the current session in milliseconds, return-type is {@code int}
		 */
		public static final String TIMEOUT = "timeout";
		/**
		 * The ID of the session, return-type is {@code String}.
		 */
		public static final String SID = "SID";
		/**
		 * A {@link Messages}-object, used to store messages that should survive a redirect.
		 */
		public static final String MESSAGES = "messages";
		/**
		 * The current {@link Subject}.
		 */
		public static final String SUBJECT = Scope.SESSION.name() + ".currentSubject";
		/**
		 * The current {@link Locale}.
		 */
		public static final String LOCALE = "locale";
		/**
		 * The current {@link TimeZone}.
		 */
		public static final String TIMEZONE = "timezone";
	}

}
