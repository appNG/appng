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

import java.util.List;

import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.springframework.data.domain.Pageable;

/**
 * A {@link FieldProcessor} keeps track of the {@link MetaData} and {@link Linkpanel}s defined in the
 * {@link org.appng.xml.platform.DataConfig} of a {@link org.appng.xml.platform.Datasource}. It provides several methods
 * to add {@link org.appng.xml.platform.Message}s, which can be field-specific.
 * 
 * @author Matthias Müller
 */
public interface FieldProcessor {

	/**
	 * Returns all {@link FieldDef}intions known by this {@link FieldProcessor}.
	 * 
	 * @return a {@link List} of {@link FieldDef}intions
	 */
	List<FieldDef> getFields();

	/**
	 * Returns the {@link FieldDef} with the given binding, if existing.
	 * 
	 * @param fieldBinding
	 *                     the binding of the {@link FieldDef} to return
	 * 
	 * @return the {@link FieldDef} with the given binding, or {@code null} if no such {@link FieldDef} exists
	 * 
	 * @see FieldDef#getBinding()
	 */
	FieldDef getField(String fieldBinding);

	/**
	 * Returns the {@link MetaData} of this {@link FieldProcessor}.
	 * 
	 * @return the {@link MetaData}
	 */
	MetaData getMetaData();

	/**
	 * Checks whether a {@link FieldDef} with the given binding exists for this {@link FieldProcessor}.
	 * 
	 * @param fieldBinding
	 *                     the binding of the {@link FieldDef} to check
	 * 
	 * @return {@code true} if such a {@link FieldDef} exists, {@code false} otherwise.
	 * 
	 * @see #getField(String)
	 */
	boolean hasField(String fieldBinding);

	/**
	 * Returns the id of the {@link org.appng.xml.platform.Action} or {@link org.appng.xml.platform.Datasource} this
	 * {@link FieldProcessor} was created for.
	 * 
	 * @return the reference id
	 */
	String getReference();

	/**
	 * Checks whether this {@link FieldProcessor} or one of its fields has a {@link org.appng.xml.platform.Message} of
	 * type {@link org.appng.xml.platform.MessageType#ERROR}.
	 * 
	 * @return {@code true} if there are any {@link org.appng.xml.platform.Message}s of
	 *         {@link org.appng.xml.platform.MessageType#ERROR}, {@code false} otherwise
	 * 
	 * @see #addErrorMessage(String)
	 * @see #addErrorMessage(FieldDef, String)
	 */
	boolean hasErrors();

	/**
	 * Checks whether one of the fields has a {@link org.appng.xml.platform.Message} of type
	 * {@link org.appng.xml.platform.MessageType#ERROR}.
	 * 
	 * @return {@code true} if any of the {@link FieldProcessor}s {@link FieldDef}s has a
	 *         {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#ERROR},
	 *         {@code false} otherwise
	 * 
	 * @see #addErrorMessage(FieldDef, String)
	 */
	boolean hasFieldErrors();

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#OK}.
	 * 
	 * @param message
	 *                the message to add
	 */
	void addOkMessage(String message);

	/**
	 * Adds the given list of {@link Linkpanel}s to this {@link FieldProcessor}
	 * 
	 * @param panels
	 *               a list of {@link Linkpanel}s
	 */
	void addLinkPanels(List<Linkpanel> panels);

	/**
	 * Returns the {@link Linkpanel} with the given name, if existing.
	 * 
	 * @param fieldName
	 *                  the name of the {@link FieldDef} respectively the {@link Linkpanel}
	 * 
	 * @return the {@link Linkpanel}, if existing, {@code null} otherwise
	 */
	Linkpanel getLinkPanel(String fieldName);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#OK} to the given
	 * field.
	 * 
	 * @param field
	 *                the {@link FieldDef} to add the {@link org.appng.xml.platform.Message} to
	 * @param message
	 *                the message to add
	 */
	void addOkMessage(FieldDef field, String message);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#ERROR}.
	 * 
	 * @param message
	 *                the message to add
	 * 
	 * @see FieldProcessor#hasErrors()
	 */
	void addErrorMessage(String message);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#ERROR} to the
	 * given field.
	 * 
	 * @param field
	 *                the {@link FieldDef} to add the {@link org.appng.xml.platform.Message} to
	 * @param message
	 *                the message to add
	 * 
	 * @see FieldProcessor#hasErrors()
	 * @see FieldProcessor#hasFieldErrors()
	 */
	void addErrorMessage(FieldDef field, String message);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#INVALID}.
	 * 
	 * @param message
	 *                the message to add
	 */
	void addInvalidMessage(String message);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#INVALID} to the
	 * given field.
	 * 
	 * @param field
	 *                the {@link FieldDef} to add the {@link org.appng.xml.platform.Message} to
	 * @param message
	 *                the message to add
	 */
	void addInvalidMessage(FieldDef field, String message);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#INVALID}.
	 * 
	 * @param message
	 *                the message to add
	 */
	void addNoticeMessage(String message);

	/**
	 * Adds a {@link org.appng.xml.platform.Message} of type {@link org.appng.xml.platform.MessageType#NOTICE} to the
	 * given field.
	 * 
	 * @param field
	 *                the {@link FieldDef} to add the {@link org.appng.xml.platform.Message} to
	 * @param message
	 *                the message to add
	 */
	void addNoticeMessage(FieldDef field, String message);

	/**
	 * Returns a new {@link Messages}-object containing all {@link org.appng.xml.platform.Message}s previously added
	 * using {@code addXXMessage(String message)}.
	 * 
	 * @return a new {@link Messages} instance containing the previously added {@link org.appng.xml.platform.Message}s
	 * 
	 * @see #addOkMessage(String)
	 * @see #addNoticeMessage(String)
	 * @see #addInvalidMessage(String)
	 * @see #addErrorMessage(String)
	 */
	Messages getMessages();

	/**
	 * Clears all {@link org.appng.xml.platform.Message}s which were previously added using
	 * {@code addXXMessage(String message)}.
	 * 
	 * @see #addOkMessage(String)
	 * @see #addNoticeMessage(String)
	 * @see #addInvalidMessage(String)
	 * @see #addErrorMessage(String)
	 */
	void clearMessages();

	/**
	 * Clears all {@link org.appng.xml.platform.Message}s which were previously added using
	 * {@code addXXMessage(FieldDef field, String message)}.
	 * 
	 * @see #addOkMessage(FieldDef, String)
	 * @see #addNoticeMessage(FieldDef, String)
	 * @see #addInvalidMessage(FieldDef, String)
	 * @see #addErrorMessage(FieldDef, String)
	 */
	void clearFieldMessages();

	/**
	 * Clears all {@link org.appng.xml.platform.Message}s for the {@link FieldDef}initions with the given bindings.
	 * 
	 * @param fieldBindings
	 *                      the bindings of the fields to clear the messages for
	 * 
	 * @see #addOkMessage(FieldDef, String)
	 * @see #addNoticeMessage(FieldDef, String)
	 * @see #addInvalidMessage(FieldDef, String)
	 * @see #addErrorMessage(FieldDef, String)
	 * @see FieldDef#getBinding()
	 */
	void clearFieldMessages(String... fieldBindings);

	/**
	 * Returns the {@link Pageable} for this {@link FieldProcessor}.
	 * 
	 * @return the {@link Pageable}
	 */
	Pageable getPageable();

}
