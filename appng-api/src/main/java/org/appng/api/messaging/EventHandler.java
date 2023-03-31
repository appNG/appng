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
package org.appng.api.messaging;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Site;

/**
 * An event-handler is responsible for handling events of a certain type.
 * 
 * @author @author Matthias Müller
 *
 * @param <E>
 *            the type of the {@link Event}
 * 
 * @see EventRegistry
 * @see Receiver#setDefaultHandler(EventHandler)
 * @see Receiver#registerHandler(EventHandler)
 */
public interface EventHandler<E extends Event> {

	/**
	 * Handles the given {@link Event}, usually by calling it's {@code perform}-method.
	 * 
	 * @param event
	 *                    the {@link Event} to be handled
	 * @param environment
	 *                    the {@link Environment} to use
	 * @param site
	 *                    the {@link Site} where the {@link Event} occurred
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if there's a configuration error
	 * @throws BusinessException
	 *                                       if an error occurs while performing the event
	 * 
	 * @see Event#perform(Environment, Site)
	 */
	void onEvent(E event, Environment environment, Site site) throws InvalidConfigurationException, BusinessException;

	/**
	 * Returns the type of the {@link Event} this handler reacts to
	 * 
	 * @return the type of the {@link Event} this handler reacts to
	 */
	Class<E> getEventClass();
}
