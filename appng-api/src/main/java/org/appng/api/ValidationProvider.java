/*
 * Copyright 2011-2018 the original author or authors.
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

import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Validation;

/**
 * Provides some validation-related methods.
 * 
 * @author Matthias Müller
 * 
 */
public interface ValidationProvider {

	/**
	 * Validates the given bean by adding error-messages to the {@link FieldDef} initions, using
	 * {@link FieldProcessor#addErrorMessage(FieldDef, String)}.
	 * 
	 * @param bean
	 *            the bean to validate
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} containing the {@link MetaData} for the bean
	 * @param groups
	 *            the JSR-303 validation groups to use
	 * @see FieldProcessor#addErrorMessage(FieldDef, String)
	 */
	void validateBean(Object bean, FieldProcessor fieldProcessor, Class<?>... groups);

	/**
	 * Validates the given bean by adding error-messages to the {@link FieldDef} initions, using
	 * {@link FieldProcessor#addErrorMessage(FieldDef, String)}.
	 * 
	 * @param bean
	 *            the bean to validate
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} containing the {@link MetaData} for the bean
	 * @param excludeBindings
	 *            an array of field-bindings to ignore during validation
	 * @param groups
	 *            the JSR-303 validation groups to use
	 * @see FieldProcessor#addErrorMessage(FieldDef, String)
	 */
	void validateBean(Object bean, FieldProcessor fieldProcessor, String[] excludeBindings, Class<?>... groups);

	/**
	 * Validates a single {@link FieldDef} by adding error-messages to it, using
	 * {@link FieldProcessor#addErrorMessage(FieldDef, String)}.
	 * 
	 * @param bean
	 *            the bean to validate
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} containing the {@link MetaData} for the bean
	 * @param fieldBinding
	 *            the binding of the {@link FieldDef} to validate
	 * @param groups
	 *            the JSR-303 validation groups to use
	 * @see FieldProcessor#addErrorMessage(FieldDef, String)
	 */
	void validateField(Object bean, FieldProcessor fieldProcessor, String fieldBinding, Class<?>... groups);

	/**
	 * Based on the JSR-303 annotations of the {@link MetaData}'s bindclass, this method adds the {@link Validation} to
	 * the {@link FieldDef}initions.
	 * 
	 * @param metaData
	 *            the {@link MetaData} to add the {@link Validation}s to
	 * @param classLoader
	 *            the {@link ClassLoader} to load the {@link MetaData}s bindClass
	 * @param groups
	 *            the JSR-303 validation groups to use
	 * @throws ClassNotFoundException
	 *             if the bindClass can not be loaded
	 * 
	 * @see org.appng.xml.platform.Min
	 * @see org.appng.xml.platform.Max
	 * @see org.appng.xml.platform.Size
	 * @see org.appng.xml.platform.Digits
	 * @see org.appng.xml.platform.NotNull
	 * @see org.appng.xml.platform.Future
	 * @see org.appng.xml.platform.Past
	 * @see org.appng.xml.platform.Type
	 * @see org.appng.xml.platform.Pattern
	 * @see org.appng.xml.platform.FileUpload
	 * 
	 */
	void addValidationMetaData(MetaData metaData, ClassLoader classLoader, Class<?>... groups) throws ClassNotFoundException;

}
