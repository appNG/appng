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
package org.appng.core.domain;

/**
 * 
 * Utility class providing constants for commonly used validation patterns
 * 
 * @author Matthias Müller
 * 
 */
public class ValidationPatterns {

	public static final String NAME_PATTERN = "([a-zA-Z0-9_\\.-]{3,}[ ]*)+([a-zA-Z0-9_\\.-][ ]*)+";
	public static final String NAME_STRICT_PATTERN = "[a-zA-Z0-9\\-]{3,}";
	public static final String PERMISSON_PATTERN = "[a-zA-Z0-9\\-\\.]{3,}";
	public static final String HOST_PATTERN = "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])";
	public static final String DOMAIN_PATTERN = "(http(s?)(\\://))?(" + HOST_PATTERN + ")(\\:[0-9]+)?";
	public static final String EMAIL_PATTERN = "^([a-zA-Z0-9_\\.-])+@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]){2,}$";
	// may be either a word or an e-mail address
	public static final String USERNAME_PATTERN = "^([a-zA-Z0-9_\\.-])+(@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]){2,})?$";

	public static final String NAME_MSSG = "{validation.name}";
	public static final String NAME_STRICT_MSSG = "{validation.nameStrict}";
	public static final String PERMISSON_MSSG = "{validation.permission}";
	public static final String HOST_MSSG = "{validation.host}";
	public static final String DOMAIN_MSSG = "{validation.domain}";
	public static final String USERNAME_MSSG = "{validation.username}";

	public static final int LENGTH_8192 = 8192;
	public static final int LENGTH_64 = 64;

	private ValidationPatterns() {
	}

}
