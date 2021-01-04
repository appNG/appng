/*
 * Copyright 2011-2021 the original author or authors.
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
 * An {@link Exception} to be used inside business logic. If one of the constructors is used that take a
 * {@code messageKey}-argument, it's easy to create a user-friendly error message.
 * 
 * @author Matthias Müller
 * @see MessageParam
 */
public class BusinessException extends Exception implements MessageParam {

	private String messageKey;
	private Object[] messageArgs;

	public BusinessException(String exceptionMessage, String messageKey, Object... messageArgs) {
		this(exceptionMessage);
		this.messageKey = messageKey;
		this.messageArgs = messageArgs;
	}

	public BusinessException(String exceptionMessage, Throwable cause, String messageKey, Object... messageArgs) {
		this(exceptionMessage, cause);
		this.messageKey = messageKey;
		this.messageArgs = messageArgs;
	}

	public BusinessException(String exceptionMessage) {
		super(exceptionMessage);
	}

	public BusinessException(String exceptionMessage, Throwable cause) {
		super(exceptionMessage, cause);
	}

	public BusinessException(Throwable cause) {
		super(cause);
	}

	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getMessageArgs() {
		return messageArgs;
	}

}
