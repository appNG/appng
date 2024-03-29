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
package org.appng.api.search;

import org.appng.api.observe.Observable.Event;

/**
 * A {@link DocumentEvent} encapsulates a {@link Document} and an {@link Event} associated with this {@link Document}.
 * TODO appng-search is the better place for this
 * 
 * @author Matthias Müller
 */
public class DocumentEvent {
	private final Document document;
	private final Event event;

	public DocumentEvent(Document document, Event event) {
		this.document = document;
		this.event = event;
	}

	public Document getDocument() {
		return document;
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public String toString() {
		return event + " " + document.getType() + "#" + document.getId();
	}
}
