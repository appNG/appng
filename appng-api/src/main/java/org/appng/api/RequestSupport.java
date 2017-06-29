/*
 * Copyright 2011-2017 the original author or authors.
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

import org.appng.forms.RequestContainer;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;

/**
 * 
 * Provides commonly used methods for supporting request-processing and filling bindobjects.
 * 
 * @author Matthias Müller
 * 
 */
public interface RequestSupport extends ConversionService {

	/**
	 * Returns a {@link FieldConverter}
	 * 
	 * @return a {@link FieldConverter}
	 */
	FieldConverter getFieldConverter();

	/**
	 * Handles an {@link Exception}. If the given Exception is a {@link BusinessException}, the very same
	 * {@link BusinessException} is being re-thrown. Otherwise, the {@link Exception} is being wrapped into a new
	 * {@link BusinessException} which is then being thrown.<br/>
	 * If the given {@link Exception} is a {@link MessageParam}, {@link #addErrorMessage(FieldProcessor, MessageParam)}
	 * is being called.
	 * 
	 * @param fp
	 *            a {@link FieldProcessor}
	 * @param e
	 *            an {@link Exception}
	 * @throws BusinessException
	 * @see MessageParam
	 * @see #addErrorMessage(FieldProcessor, MessageParam)
	 */
	void handleException(FieldProcessor fp, Exception e) throws BusinessException;

	/**
	 * Adds an error to the {@link FieldProcessor} using {@link FieldProcessor#addErrorMessage(String)}.
	 * {@link MessageParam#getMessageKey()} is used for the message key, {@link MessageParam#getMessageArgs()} for the
	 * message arguments.
	 * 
	 * @param fp
	 *            a {@link FieldProcessor}
	 * @param messageParam
	 *            a {@link MessageParam}
	 * 
	 * @see MessageParam
	 */
	void addErrorMessage(FieldProcessor fp, MessageParam messageParam);

	/**
	 * Adds an error to a {@link FieldDef} using
	 * {@link FieldProcessor#addErrorMessage(org.appng.xml.platform.FieldDef, String)}.
	 * {@link MessageParam#getMessageKey()} is used for the message key, {@link MessageParam#getMessageArgs()} for the
	 * message arguments.
	 * 
	 * @param fp
	 *            a {@link FieldProcessor}
	 * @param messageParam
	 *            a {@link MessageParam}
	 * @param fieldBinding
	 *            the binding of the {@link FieldDef} to add the error to
	 * @see MessageParam
	 */
	void addErrorMessage(FieldProcessor fp, MessageParam messageParam, String fieldBinding);

	/**
	 * Copies the fields defined by the given {@link MetaData} from {@code source} to {@code target}.
	 * 
	 * @param source
	 *            the source object to read the fields from
	 * @param target
	 *            the target object to write the fields to
	 * @param metaData
	 *            the {@link MetaData}
	 * @throws IllegalArgumentException
	 *             if the type of {@code T} is incompatible to {@link MetaData#getBindClass()} or the bindclass can not
	 *             be found.
	 */
	<T> void setPropertyValues(T source, T target, MetaData metaData);

	/**
	 * Copies the property from {@code source} to {@code target}.
	 * 
	 * @param source
	 *            the source object to read the property from
	 * @param target
	 *            the target object to write the property to
	 * @param property
	 *            the name of the property to set
	 */
	<T> void setPropertyValue(T source, T target, String property);

	/**
	 * Creates, fills and returns a new bindobject. The type of the returned object is obtained from
	 * {@code fp.getMetaData().getBindClass()}. The {@link MetaData}'s {@link FieldDef}initions are responsible for
	 * which fields are getting filled.
	 * 
	 * @param fp
	 *            the current {@link FieldProcessor}
	 * @param container
	 *            the {@link RequestContainer} used to retrieve the values for filling the bindobject
	 * @param classLoader
	 *            the {@link ClassLoader} used to load the bindclass
	 * @return a new bindobject
	 * @throws BusinessException
	 *             if the bindclass could not be loaded, instantiated or initialized, or if an error occurred during
	 *             filling the bindobject
	 * 
	 * @see #fillBindObject(Object, FieldProcessor, RequestContainer, ClassLoader)
	 */
	Object getBindObject(FieldProcessor fp, RequestContainer container, ClassLoader classLoader)
			throws BusinessException;

	/**
	 * Fills the given {@code bindobject} with the (converted) values provided by a {@link RequestContainer}. The type
	 * of the {@code bindobject} must match {@code fp.getMetaData().getBindClass()}. The {@link MetaData}'s
	 * {@link FieldDef}initions are responsible for which fields are getting filled.
	 * 
	 * @param bindobject
	 *            the bindobject to fill
	 * @param fp
	 *            the current {@link FieldProcessor}
	 * @param container
	 *            the {@link RequestContainer} used to retrieve the values for filling the bindobject
	 * @param classLoader
	 *            the {@link ClassLoader} used to load the bindclass
	 * @throws BusinessException
	 *             <ul>
	 *             <li>if the type of the {@code bindobject} is not assignable from the type returned by
	 *             {@code fp.getMetaData().getBindClass()}
	 *             <li>if the type returned by {@code fp.getMetaData().getBindClass()} can not be instanciated
	 *             </ul>
	 */
	void fillBindObject(Object bindobject, FieldProcessor fp, RequestContainer container, ClassLoader classLoader)
			throws BusinessException;

	/**
	 * Returns the currently used {@link MessageSource}
	 * 
	 * @return a {@link MessageSource}
	 */
	MessageSource getMessageSource();

	/**
	 * Retrieves a message from the underlying {@link MessageSource}, using the {@link Locale} provided by
	 * {@link Environment#getLocale()}.
	 * 
	 * @param key
	 *            the message key
	 * @param args
	 *            the message arguments
	 */
	String getMessage(String key, Object... args);

	/**
	 * Returns the {@link Environment} for this {@link Request}.
	 * 
	 * @return the {@link Environment}
	 */
	Environment getEnvironment();

}
