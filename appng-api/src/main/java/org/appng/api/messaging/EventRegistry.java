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
package org.appng.api.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Site;

/**
 * A registry for {@link EventHandler}s to be used by {@link Receiver} implementations.
 * 
 * @author @author Matthias Müller
 * 
 * @see Receiver#setDefaultHandler(EventHandler)
 * @see Receiver#registerHandler(EventHandler)
 */
public class EventRegistry {

	private Map<Class<? extends Event>, List<EventHandler<? extends Event>>> handlers = new ConcurrentHashMap<>();
	private EventHandler<? extends Event> defaultHandler;

	/**
	 * Registers the given {@link EventHandler}
	 * 
	 * @param handler
	 *                the {@link EventHandler} to be registered
	 */
	public <E extends Event, H extends EventHandler<E>> void register(H handler) {
		if (!handlers.containsKey(handler.getEventClass())) {
			handlers.put(handler.getEventClass(), new ArrayList<EventHandler<? extends Event>>());
		}
		handlers.get(handler.getEventClass()).add(handler);
	}

	/**
	 * Retrieves a list of {@link EventHandler}s that have been registered for the given type of {@link Event}. If no
	 * such handlers have been registered, the list will only contain the default handler.
	 * 
	 * @param event
	 *              the {@link Event} to retrieve the {@link EventHandler}s for
	 * 
	 * @return a list of {@link EventHandler}s
	 * 
	 * @see #setDefaultHandler(EventHandler)
	 */
	@SuppressWarnings("unchecked")
	public <E extends Event, H extends EventHandler<E>> List<H> getHandlers(E event) {
		List<H> handlerList = (List<H>) handlers.get(event.getClass());
		if (null == handlerList || handlerList.isEmpty()) {
			return (List<H>) Arrays.asList(null == defaultHandler ? new DefaultEventHandler() : defaultHandler);
		}
		return handlerList;
	}

	/**
	 * Set the default {@link EventHandler}
	 * 
	 * @param defaultHandler
	 *                       the default {@link EventHandler}
	 */
	public void setDefaultHandler(EventHandler<? extends Event> defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	class DefaultEventHandler implements EventHandler<Event> {

		public void onEvent(Event event, Environment environment, Site site)
				throws InvalidConfigurationException, BusinessException {
			event.perform(environment, site);
		}

		public Class<Event> getEventClass() {
			return Event.class;
		}

	}
}
