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
package org.appng.api.support;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FieldWrapper;
import org.appng.api.MessageParam;
import org.appng.api.RequestSupport;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link RequestSupport} implementation
 * 
 * @author Matthias Müller
 */
@Slf4j
public class RequestSupportImpl extends AdapterBase implements RequestSupport {

	public RequestSupportImpl() {
	}

	public RequestSupportImpl(ConversionService conversionService, Environment environment,
			MessageSource messageSource) {
		setConversionService(conversionService);
		setEnvironment(environment);
		setMessageSource(messageSource);
	}

	public final void fillBindObject(Object instance, FieldProcessor fp, RequestContainer container,
			ClassLoader classLoader) throws BusinessException {
		FieldDef currentField = null;
		MetaData metaData = fp.getMetaData();
		String bindClassName = metaData.getBindClass();
		try {

			Class<?> bindClass = getBindClass(classLoader, bindClassName);

			if (!instance.getClass().isAssignableFrom(bindClass)) {
				throw new BusinessException("given object is of type '" + instance.getClass().getName()
						+ "', but bindClass is '" + bindClass.getName() + "'");
			}

			BeanWrapper beanWrapper = new BeanWrapperImpl(instance);
			for (FieldDef fieldDef : metaData.getFields()) {
				if (!Boolean.TRUE.toString().equalsIgnoreCase(fieldDef.getReadonly())) {
					currentField = fieldDef;
					FieldWrapper fieldWrapper = new FieldWrapper(fieldDef, beanWrapper);
					fieldConverter.setObject(fieldWrapper, container);
				} else {
					LOGGER.trace("{} is readonly!", fieldDef.getBinding());
				}
			}
		} catch (Exception e) {
			if (e instanceof BusinessException) {
				throw (BusinessException) e;
			}
			if (null != currentField) {
				throw new BusinessException("error while processing field '" + currentField.getName() + "' (type: "
						+ currentField.getType() + ", binding: '" + currentField.getBinding() + "')' on bindclass '"
						+ bindClassName + "'", e);
			} else {
				throw new BusinessException("error while creating bindobject field ", e);
			}
		}
	}

	public final Object getBindObject(FieldProcessor fp, RequestContainer container, ClassLoader classLoader)
			throws BusinessException {
		Object instance = getBindClassInstance(classLoader, fp);
		fillBindObject(instance, fp, container, classLoader);
		return instance;
	}

	private Object getBindClassInstance(ClassLoader classLoader, FieldProcessor fp) throws BusinessException {
		String bindClassName = fp.getMetaData().getBindClass();
		String errorMssg = String.format(
				"could not instanciate class '%s', is it an interface or default-constructor missing?", bindClassName);
		try {
			Class<?> bindClass = getBindClass(classLoader, bindClassName);
			Class<?> enclosingClass = bindClass.getEnclosingClass();
			int modifier = bindClass.getModifiers();
			if (enclosingClass == null || Modifier.isStatic(modifier)) {
				return bindClass.newInstance();
			} else {
				if (Modifier.isPublic(modifier)) {
					return bindClass.getConstructor(enclosingClass).newInstance(enclosingClass.newInstance());
				} else {
					throw new BusinessException("bindClass " + bindClass.getName() + " needs to be public!");
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new BusinessException(errorMssg, e);
		}
	}

	private Class<?> getBindClass(ClassLoader classLoader, String bindClass) throws BusinessException {
		if (StringUtils.isNotBlank(bindClass)) {
			try {
				return ClassUtils.forName(bindClass, classLoader);
			} catch (ClassNotFoundException e) {
				throw new BusinessException("class '" + bindClass + "' not found in classloader " + classLoader, e);
			}
		} else {
			throw new BusinessException("bindclass is empty!");
		}
	}

	public <T> T convert(Object source, Class<T> target, T defaultValue) {
		T result = conversionService.convert(source, target);
		if (null != result) {
			return result;
		}
		return defaultValue;
	}

	public <T> T getDefaultIfNull(T source, T defaultValue) {
		if (null == source) {
			return defaultValue;
		}
		return source;
	}

	public String getMessage(String key, Object... args) {
		return getMessageSource().getMessage(key, args, getEnvironment().getLocale());
	}

	public void addErrorMessage(FieldProcessor fp, MessageParam messageParam) {
		String messageKey = messageParam.getMessageKey();
		if (null != messageKey) {
			Object[] messageArgs = messageParam.getMessageArgs();
			fp.addErrorMessage(getMessage(messageKey, messageArgs));
		}
	}

	public void addErrorMessage(FieldProcessor fp, MessageParam messageParam, String fieldBinding) {
		String messageKey = messageParam.getMessageKey();
		if (null != messageKey) {
			Object[] messageArgs = messageParam.getMessageArgs();
			String message = getMessage(messageKey, messageArgs);
			if (null != fieldBinding) {
				FieldDef field = fp.getField(fieldBinding);
				if (null != field) {
					fp.addErrorMessage(field, message);
					return;
				}
			}
			fp.addErrorMessage(message);
		}
	}

	public void handleException(FieldProcessor fp, Exception e) throws BusinessException {
		if (e instanceof MessageParam) {
			MessageParam messageParam = (MessageParam) e;
			addErrorMessage(fp, messageParam);
		}
		if (e instanceof BusinessException) {
			BusinessException businessException = (BusinessException) e;
			throw businessException;
		} else {
			throw new BusinessException(e);
		}
	}

	public <T> void setPropertyValues(T source, T target, MetaData metaData) {
		String bindClassName = metaData.getBindClass();
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Class<?> bindClass = getBindClass(classLoader, bindClassName);

			Class<? extends Object> sourceClass = source.getClass();
			if (!bindClass.isAssignableFrom(sourceClass)) {
				throw new IllegalArgumentException("source object '" + sourceClass.getName()
						+ "' is not an instance of bindClass '" + bindClassName + "'");
			}
			Class<? extends Object> targetClass = target.getClass();
			if (!bindClass.isAssignableFrom(targetClass)) {
				throw new IllegalArgumentException("target object '" + targetClass.getName()
						+ "' is not an instance of bindClass  '" + bindClassName + "'");
			}
		} catch (BusinessException e) {
			throw new IllegalArgumentException("invalid bindclass: '" + bindClassName + "'", e);
		}
		BeanWrapper sourceWrapper = new BeanWrapperImpl(source);
		BeanWrapper targetWrapper = new BeanWrapperImpl(target);
		ExpressionEvaluator currentEvaluator = new ExpressionEvaluator(new HashMap<>());
		currentEvaluator.setVariable(CURRENT, source);
		setPropertyValues(sourceWrapper, targetWrapper, metaData,
				new ElementHelper(environment, site, application, currentEvaluator));
	}

	public <T> void setPropertyValue(T source, T target, String property) {
		BeanWrapper sourceWrapper = new BeanWrapperImpl(source);
		BeanWrapper targetWrapper = new BeanWrapperImpl(target);
		setPropertyValues(sourceWrapper, targetWrapper, property);
	}

	private <T> void setPropertyValues(BeanWrapper sourceWrapper, BeanWrapper targetWrapper, MetaData metaData,
			ElementHelper elementHelper) {
		for (FieldDef fieldDef : metaData.getFields()) {
			boolean doWrite = true;
			String fieldBinding = fieldDef.getBinding();
			Condition condition = fieldDef.getCondition();
			String expression = null;
			if (null != condition) {
				expression = condition.getExpression();
				if (StringUtils.isNotBlank(expression)) {
					doWrite = elementHelper.conditionMatches(condition);
					if (doWrite) {
						LOGGER.debug("condition '{}' for property '{}' matched", expression, fieldBinding);
					} else {
						LOGGER.debug("condition '{}' for property '{}' did not match, skipping field", expression,
								fieldBinding);
					}
				}
			}
			if (doWrite) {
				setPropertyValues(sourceWrapper, targetWrapper, fieldBinding);
			}
		}
	}

	private <T> void setPropertyValues(BeanWrapper sourceWrapper, BeanWrapper targetWrapper, String... name) {
		for (String property : name) {
			if (sourceWrapper.isReadableProperty(property)) {
				if (targetWrapper.isWritableProperty(property)) {
					Object propertyValue = sourceWrapper.getPropertyValue(property);
					if (!(propertyValue instanceof Collection<?>)) {
						LOGGER.debug("setting property '{}' of class '{}' to '{}'", property,
								targetWrapper.getWrappedClass().getName(), propertyValue);
						targetWrapper.setPropertyValue(property, propertyValue);
					}
				}
			} else {
				// should never ever happen
				LOGGER.error(
						"property '" + property + "' not readable in class '" + sourceWrapper.getWrappedClass() + "'");
			}
		}
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return conversionService.canConvert(sourceType, targetType);
	}

	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return conversionService.canConvert(sourceType, targetType);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return conversionService.convert(source, sourceType, targetType);
	}

	public <T> T convert(Object source, Class<T> target) {
		return conversionService.convert(source, target);
	}

}
