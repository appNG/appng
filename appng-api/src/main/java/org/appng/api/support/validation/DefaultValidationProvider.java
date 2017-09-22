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
package org.appng.api.support.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.FieldProcessor;
import org.appng.api.FileUpload;
import org.appng.api.ValidationProvider;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Rule;
import org.appng.xml.platform.Validation;
import org.appng.xml.platform.ValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.util.ClassUtils;

/**
 * Default {@link ValidationProvider} implementation.
 * 
 * @author Matthias Müller
 * 
 */
public class DefaultValidationProvider implements ValidationProvider {

	private static Logger log = LoggerFactory.getLogger(DefaultValidationProvider.class);

	private static final String INVALID_DIGIT = "invalid.digit";

	private static final String INVALID_INTEGER = "invalid.integer";

	private Validator validator;

	private MessageInterpolator messageInterpolator;
	private MessageSource messageSource;
	private Locale locale;
	private boolean contraintsAsRule;

	/**
	 * Creates a new {@link DefaultValidationProvider}.
	 * 
	 * @param messageInterpolator
	 *            the {@link MessageInterpolator} used when adding validation messages
	 * @param messageSource
	 *            the {@link MessageSource} used when adding validation messages
	 * @param locale
	 *            the {@link Locale} used when adding validation messages
	 */
	public DefaultValidationProvider(MessageInterpolator messageInterpolator, MessageSource messageSource,
			Locale locale) {
		this(messageInterpolator, messageSource, locale, false);
	}

	/**
	 * Creates a new {@link DefaultValidationProvider}.
	 * 
	 * @param messageInterpolator
	 *            the {@link MessageInterpolator} used when adding validation messages
	 * @param messageSource
	 *            the {@link MessageSource} used when adding validation messages
	 * @param locale
	 *            the {@link Locale} used when adding validation messages
	 * @param contraintsAsRule
	 *            whether validation constraints should be added as a {@link Rule} to the {@link FieldDef}s
	 *            {@link Validation}
	 */
	public DefaultValidationProvider(MessageInterpolator messageInterpolator, MessageSource messageSource,
			Locale locale, boolean contraintsAsRule) {
		Configuration<?> configuration = javax.validation.Validation.byDefaultProvider().configure();
		ValidatorFactory validatorFactory = configuration.messageInterpolator(messageInterpolator)
				.buildValidatorFactory();
		this.validator = validatorFactory.getValidator();
		this.messageInterpolator = messageInterpolator;
		this.locale = locale;
		this.messageSource = messageSource;
		this.contraintsAsRule = contraintsAsRule;
	}

	/**
	 * Creates a new {@link DefaultValidationProvider} using the {@link MessageInterpolator} returned from
	 * {@link ValidatorFactory#getMessageInterpolator()}.
	 */
	public DefaultValidationProvider() {
		ValidatorFactory validatorFactory = javax.validation.Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
		this.messageInterpolator = validatorFactory.getMessageInterpolator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.validation.ValidationProvider#addValidationMetaData(com .appng.xml.api.MetaData,
	 * java.lang.ClassLoader)
	 */
	public void addValidationMetaData(MetaData metaData, ClassLoader classLoader, Class<?>... groups)
			throws ClassNotFoundException {
		String className = metaData.getBindClass();
		List<FieldDef> fields = metaData.getFields();
		if (null != className) {
			Class<?> validationClass = ClassUtils.forName(className, classLoader);
			for (FieldDef fieldDef : fields) {
				if (!Boolean.TRUE.toString().equalsIgnoreCase(fieldDef.getReadonly())) {
					String propertyName = fieldDef.getBinding();
					Set<ConstraintDescriptor<?>> fieldConstraints = getConstraintsForProperty(validationClass,
							propertyName);
					if (fieldConstraints != null) {
						addValidationNode(fieldDef);
						for (ConstraintDescriptor<?> constraintDescriptor : fieldConstraints) {
							fillValidation(fieldDef, constraintDescriptor, groups);
						}
					}
				}
			}
		}
	}

	private Validation addValidationNode(FieldDef fieldDef) {
		if (null == fieldDef.getValidation()) {
			fieldDef.setValidation(new Validation());
		}
		return fieldDef.getValidation();
	}

	private Set<ConstraintDescriptor<?>> getConstraintsForProperty(Class<?> clazz, String property) {
		Set<ConstraintDescriptor<?>> constraintDescriptors = null;
		Class<?> propertyType = clazz;
		String propertyName = property;

		int idx = property.lastIndexOf('.');
		if (idx > 0) {
			BeanWrapper beanWrapper = new BeanWrapperImpl(clazz);
			beanWrapper.setAutoGrowNestedPaths(true);
			propertyName = property.substring(idx + 1);
			String propertyPath = property.substring(0, idx);
			propertyType = beanWrapper.getPropertyType(propertyPath);
		}
		if (null != propertyType) {
			BeanDescriptor beanDescriptor = validator.getConstraintsForClass(propertyType);
			PropertyDescriptor propertyDescriptor = beanDescriptor.getConstraintsForProperty(propertyName);
			if (null != propertyDescriptor) {
				constraintDescriptors = propertyDescriptor.getConstraintDescriptors();
				log.debug("validation rules for property '" + propertyName + "' of class '" + propertyType + "':"
						+ constraintDescriptors);
			}
		} else {
			log.debug("unable to determine type for property '{}' of class '{}'", property, clazz);
		}
		return constraintDescriptors;
	}

	private void fillValidation(FieldDef fieldDef, final ConstraintDescriptor<?> constraintDescriptor,
			Class<?>... groups) {
		Annotation annotation = constraintDescriptor.getAnnotation();
		Set<Class<?>> constraintGroups = constraintDescriptor.getGroups();
		List<Class<?>> groupList = Arrays.asList(groups);
		boolean doAdd = groups.length == 0 && constraintGroups.contains(Default.class)
				|| !CollectionUtils.intersection(groupList, constraintGroups).isEmpty();

		ValidationRule validationRule = null;
		Validation validation = fieldDef.getValidation();

		FieldType type = fieldDef.getType();
		switch (type) {
		case LONG:
		case INT: {
			org.appng.xml.platform.Type fieldType = new org.appng.xml.platform.Type();
			validation.setType(fieldType);
			String messageText = messageSource.getMessage(INVALID_INTEGER, new Object[0], locale);
			addMessage(fieldDef, fieldType, messageText);
			break;
		}
		case DECIMAL: {
			org.appng.xml.platform.Type fieldType = new org.appng.xml.platform.Type();
			validation.setType(fieldType);
			String messageText = messageSource.getMessage(INVALID_DIGIT, new Object[0], locale);
			addMessage(fieldDef, fieldType, messageText);
			break;
		}
		default:
			break;
		}

		if (doAdd) {
			if (contraintsAsRule) {
				addRule(fieldDef, constraintDescriptor, annotation, validation);
			} else if (annotation instanceof NotNull || containsType(annotation, NotNull.class)) {
				org.appng.xml.platform.NotNull notNull = new org.appng.xml.platform.NotNull();
				validationRule = notNull;
				validation.setNotNull(notNull);
			} else if (annotation instanceof Size) {
				org.appng.xml.platform.Size size = new org.appng.xml.platform.Size();
				size.setMin(((Size) annotation).min());
				size.setMax(((Size) annotation).max());
				validationRule = size;
				validation.setSize(size);
			} else if (annotation instanceof Digits) {
				org.appng.xml.platform.Digits digits = new org.appng.xml.platform.Digits();
				digits.setInteger(((Digits) annotation).integer());
				digits.setFraction(((Digits) annotation).fraction());
				validationRule = digits;
				validation.setDigits(digits);
			} else if (annotation instanceof Future) {
				org.appng.xml.platform.Future future = new org.appng.xml.platform.Future();
				validationRule = future;
				validation.setFuture(future);
			} else if (annotation instanceof Past) {
				org.appng.xml.platform.Past past = new org.appng.xml.platform.Past();
				validationRule = past;
				validation.setPast(past);
			} else if (annotation instanceof Pattern) {
				org.appng.xml.platform.Pattern pattern = new org.appng.xml.platform.Pattern();
				pattern.setRegexp(((Pattern) annotation).regexp());
				validationRule = pattern;
				validation.setPattern(pattern);
			} else if (annotation instanceof Min) {
				validationRule = setMin(validation, new BigDecimal(((Min) annotation).value()));
			} else if (annotation instanceof Max) {
				validationRule = setMax(validation, new BigDecimal(((Max) annotation).value()));
			} else if (annotation instanceof DecimalMin) {
				validationRule = setMin(validation, new BigDecimal(((DecimalMin) annotation).value()));
			} else if (annotation instanceof DecimalMax) {
				validationRule = setMax(validation, new BigDecimal(((DecimalMax) annotation).value()));
			} else if (annotation instanceof FileUpload) {
				org.appng.xml.platform.FileUpload fileUpload = new org.appng.xml.platform.FileUpload();
				FileUpload fUp = (FileUpload) annotation;
				fileUpload.setFileTypes(fUp.fileTypes());
				fileUpload.setMinCount(fUp.minCount());
				fileUpload.setMaxCount(fUp.maxCount());
				fileUpload.setMinSize(fUp.minSize());
				fileUpload.setMaxSize(fUp.maxSize());
				fileUpload.setUnit(fUp.unit().toString());
				validationRule = fileUpload;
				validation.setFileUpload(fileUpload);
			} else {
				addRule(fieldDef, constraintDescriptor, annotation, validation);
			}
			Collections.sort(fieldDef.getValidation().getRules(), new Comparator<Rule>() {
				public int compare(Rule r1, Rule r2) {
					return r1.getName().compareTo(r2.getName());
				}
			});
			if (null != validationRule) {
				addMessage(fieldDef, constraintDescriptor, validationRule);
			}
		}
	}

	private void addRule(FieldDef fieldDef, final ConstraintDescriptor<?> constraintDescriptor, Annotation annotation,
			Validation validation) {
		Rule rule = getRule(annotation, null);
		validation.getRules().add(rule);
		addMessage(fieldDef, constraintDescriptor, rule);
	}

	protected Rule getRule(Annotation annotation, String type) {
		try {
			Rule rule = new Rule();
			Class<? extends Annotation> annotationType = annotation.annotationType();
			rule.setType(annotationType.getName());
			rule.setName(StringUtils.uncapitalize(annotationType.getSimpleName()));
			List<String> ignoredMethods = Arrays.asList("message", "flags", "annotationType", "groups", "payload",
					"hashCode", "toString");
			List<Method> methods = new ArrayList<Method>(Arrays.asList(annotationType.getMethods()));
			Collections.sort(methods, new Comparator<Method>() {
				public int compare(Method o1, Method o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (Method method : methods) {
				String name = method.getName();
				if (method.getParameterTypes().length == 0 && !ignoredMethods.contains(name)) {
					Rule.Option option = new Rule.Option();
					Object invoked = method.invoke(annotation);
					if (invoked.getClass().isArray()) {
						option.setValue(StringUtils.join((Object[]) invoked, ','));
					} else if (Iterable.class.isAssignableFrom(invoked.getClass())) {
						option.setValue(StringUtils.join((Iterable<?>) invoked, ','));
					} else {
						option.setValue(invoked.toString());
					}
					option.setName(name);
					rule.getOption().add(option);
				}
			}
			return rule;
		} catch (Exception e) {
			log.error("error processing annotation " + annotation, e);
		}
		return null;
	}

	private boolean containsType(Annotation annotation, Class<? extends Annotation> annotationClass) {
		for (Annotation a : annotation.annotationType().getAnnotations()) {
			if (a.annotationType().equals(annotationClass)) {
				return true;
			}
		}
		return false;
	}

	private org.appng.xml.platform.Min setMin(Validation validation, BigDecimal value) {
		org.appng.xml.platform.Min min = validation.getMin();
		if (null == min) {
			min = new org.appng.xml.platform.Min();
			min.setValue(value);
			validation.setMin(min);
		}
		return min;
	}

	private org.appng.xml.platform.Max setMax(Validation validation, BigDecimal value) {
		org.appng.xml.platform.Max max = validation.getMax();
		if (null == max) {
			max = new org.appng.xml.platform.Max();
			max.setValue(value);
			validation.setMax(max);
		}
		return max;
	}

	private void addMessage(FieldDef field, final ConstraintDescriptor<?> constraintDescriptor,
			ValidationRule validationRule) {
		try {
			Object annotation = constraintDescriptor.getAnnotation();
			String messageTemplate = (String) annotation.getClass().getMethod("message").invoke(annotation);
			String messageText = messageInterpolator.interpolate(messageTemplate, new MessageInterpolator.Context() {
				public Object getValidatedValue() {
					return null;
				}

				public ConstraintDescriptor<?> getConstraintDescriptor() {
					return constraintDescriptor;
				}

				public <T> T unwrap(Class<T> type) {
					return null;
				}
			});
			addMessage(field, validationRule, messageText);
		} catch (Exception e) {
			log.warn("error while getting message from " + constraintDescriptor, e);
		}
	}

	protected void addMessage(FieldDef field, ValidationRule validationRule, String messageText) {
		Message message = new Message();
		message.setRef(field.getBinding());
		message.setClazz(MessageType.ERROR);
		message.setContent(messageText);
		validationRule.setMessage(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.validation.ValidationProvider#validateBean(java.lang.Object , org.appng.api.FieldProcessor,
	 * java.lang.Class<?>[])
	 */
	public void validateBean(Object bean, FieldProcessor fp, Class<?>... groups) {
		if (null != bean) {
			for (ConstraintViolation<Object> cv : validator.validate(bean, groups)) {
				String reference = cv.getPropertyPath().toString();
				if (fp.hasField(reference)) {
					fp.addErrorMessage(fp.getField(reference), cv.getMessage());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.ValidationProvider#validateBean(java.lang.Object, org.appng.api.FieldProcessor,
	 * java.lang.String[], java.lang.Class[])
	 */
	public void validateBean(Object bean, FieldProcessor fp, String[] excludeBindings, Class<?>... groups) {
		List<String> excludeFields = Arrays.asList(excludeBindings);
		for (FieldDef fieldDef : fp.getFields()) {
			String binding = fieldDef.getBinding();
			if (!excludeFields.contains(binding)) {
				validateField(bean, fp, fieldDef, groups);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.validation.ValidationProvider#validateField(java.lang.Object , org.appng.api.FieldProcessor,
	 * java.lang.String, java.lang.Class<?>[])
	 */
	public void validateField(Object bean, FieldProcessor fp, String fieldBinding, Class<?>... groups) {
		FieldDef field = fp.getField(fieldBinding);
		if (null != field) {
			validateField(bean, fp, field, groups);
		}
	}

	private void validateField(Object bean, FieldProcessor fp, FieldDef field, Class<?>... groups) {
		Set<ConstraintViolation<Object>> violations = validator.validateProperty(bean, field.getBinding(), groups);
		for (ConstraintViolation<Object> cv : violations) {
			String message = cv.getMessage();
			fp.addErrorMessage(field, message);
		}
	}

}
