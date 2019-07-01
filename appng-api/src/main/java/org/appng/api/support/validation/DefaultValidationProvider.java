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
package org.appng.api.support.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Valid;
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
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Rule;
import org.appng.xml.platform.Validation;
import org.appng.xml.platform.ValidationRule;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link ValidationProvider} implementation.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class DefaultValidationProvider implements ValidationProvider {

	private static final String INVALID_DIGIT = "invalid.digit";

	private static final String INVALID_INTEGER = "invalid.integer";

	private static final String INDEXED = "[]";
	private static final String INDEX_PATTERN = "\\[\\d*\\]";

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
		if (null != className) {
			Class<?> validationClass = ClassUtils.forName(className, classLoader);
			for (FieldDef fieldDef : metaData.getFields()) {
				fillValidation(validationClass, fieldDef, groups);
			}
		}
	}

	private void fillValidation(Class<?> validationClass, FieldDef fieldDef, Class<?>... groups) {
		if (!Boolean.TRUE.toString().equalsIgnoreCase(fieldDef.getReadonly())) {
			String propertyName = fieldDef.getBinding();
			LOGGER.debug("Adding validation data for field {} for class {}", propertyName, validationClass);
			Set<ConstraintDescriptor<?>> fieldConstraints = getConstraintsForProperty(validationClass, propertyName);
			if (fieldConstraints != null) {
				for (ConstraintDescriptor<?> constraintDescriptor : fieldConstraints) {
					fillValidation(fieldDef, constraintDescriptor, groups);
				}
			}
			for (FieldDef childField : fieldDef.getFields()) {
				fillValidation(validationClass, childField, groups);
			}
		}
	}

	private Validation getValidationNode(FieldDef fieldDef) {
		if (null == fieldDef.getValidation()) {
			fieldDef.setValidation(new Validation());
		}
		return fieldDef.getValidation();
	}

	private Set<ConstraintDescriptor<?>> getConstraintsForProperty(final Class<?> validationClass,
			final String propertyPath) {
		String normalizedPath = propertyPath.replaceAll(INDEX_PATTERN, StringUtils.EMPTY);
		int separator = normalizedPath.lastIndexOf('.');
		String rootPath = separator > 0 ? normalizedPath.substring(0, separator) : normalizedPath;
		String leafName = separator > 0 ? normalizedPath.substring(separator + 1) : normalizedPath;

		Set<ConstraintDescriptor<?>> constraints = null;

		Class<?> propertyType = validationClass;
		Class<?> concreteType = validationClass;
		Field ancestor = null;
		if (!rootPath.equals(leafName)) {
			for (String segment : rootPath.split("\\.")) {
				Field field = ReflectionUtils.findField(propertyType, segment);
				if (null != field) {
					if (null != ancestor) {
						Valid fieldAnnotation = field.getAnnotation(Valid.class);
						Method getter = ReflectionUtils.findMethod(propertyType,
								"get" + StringUtils.capitalize(segment));
						Valid methodAnnotation = null == getter ? null : getter.getAnnotation(Valid.class);
						if (null == fieldAnnotation && null == methodAnnotation) {
							LOGGER.debug("Annotation @{} not found on property {}.{} of {}, returning",
									Valid.class.getName(), ancestor.getName(), field.getName(), validationClass);
							return null;
						}
						LOGGER.debug("Annotation @{} found on property {}.{} of {}", Valid.class.getName(),
								ancestor.getName(), field.getName(), validationClass);
					}
					propertyType = new BeanWrapperImpl(propertyType).getPropertyType(segment);
					ancestor = field;
					if (Collection.class.isAssignableFrom(propertyType)) {
						concreteType = (Class<?>) ((ParameterizedType) field.getGenericType())
								.getActualTypeArguments()[0];
					} else if (propertyType.isArray()) {
						concreteType = propertyType.getComponentType();
					} else {
						concreteType = propertyType;
					}
				}
			}
		}

		BeanDescriptor beanDescriptor = validator.getConstraintsForClass(concreteType);
		if (null != beanDescriptor) {
			PropertyDescriptor propertyDescriptor = beanDescriptor.getConstraintsForProperty(leafName);
			if (null != propertyDescriptor) {
				constraints = propertyDescriptor.getConstraintDescriptors();
				LOGGER.debug("Found constraint(s) for path {} on type {}: {}", propertyPath, validationClass,
						constraints);
			}
		}

		return constraints;

	}

	private void fillValidation(FieldDef fieldDef, final ConstraintDescriptor<?> constraintDescriptor,
			Class<?>... groups) {
		Annotation annotation = constraintDescriptor.getAnnotation();
		Set<Class<?>> constraintGroups = constraintDescriptor.getGroups();
		List<Class<?>> groupList = Arrays.asList(groups);
		boolean doAdd = groups.length == 0 && constraintGroups.contains(Default.class)
				|| !CollectionUtils.intersection(groupList, constraintGroups).isEmpty();

		ValidationRule validationRule = null;

		FieldType type = fieldDef.getType();
		switch (type) {
		case LONG:
		case INT: {
			org.appng.xml.platform.Type fieldType = new org.appng.xml.platform.Type();
			getValidationNode(fieldDef).setType(fieldType);
			String messageText = messageSource.getMessage(INVALID_INTEGER, new Object[0], locale);
			addMessage(fieldDef, fieldType, INVALID_INTEGER, messageText);
			break;
		}
		case DECIMAL: {
			org.appng.xml.platform.Type fieldType = new org.appng.xml.platform.Type();
			getValidationNode(fieldDef).setType(fieldType);
			String messageText = messageSource.getMessage(INVALID_DIGIT, new Object[0], locale);
			addMessage(fieldDef, fieldType, INVALID_DIGIT, messageText);
			break;
		}
		default:
			break;
		}

		if (doAdd) {
			if (contraintsAsRule) {
				addRule(fieldDef, constraintDescriptor, annotation, getValidationNode(fieldDef));
			} else if (annotation instanceof NotNull || containsType(annotation, NotNull.class)) {
				org.appng.xml.platform.NotNull notNull = new org.appng.xml.platform.NotNull();
				validationRule = notNull;
				getValidationNode(fieldDef).setNotNull(notNull);
			} else if (annotation instanceof Size) {
				org.appng.xml.platform.Size size = new org.appng.xml.platform.Size();
				size.setMin(((Size) annotation).min());
				size.setMax(((Size) annotation).max());
				validationRule = size;
				getValidationNode(fieldDef).setSize(size);
			} else if (annotation instanceof Digits) {
				org.appng.xml.platform.Digits digits = new org.appng.xml.platform.Digits();
				digits.setInteger(((Digits) annotation).integer());
				digits.setFraction(((Digits) annotation).fraction());
				validationRule = digits;
				getValidationNode(fieldDef).setDigits(digits);
			} else if (annotation instanceof Future) {
				org.appng.xml.platform.Future future = new org.appng.xml.platform.Future();
				validationRule = future;
				getValidationNode(fieldDef).setFuture(future);
			} else if (annotation instanceof Past) {
				org.appng.xml.platform.Past past = new org.appng.xml.platform.Past();
				validationRule = past;
				getValidationNode(fieldDef).setPast(past);
			} else if (annotation instanceof Pattern) {
				org.appng.xml.platform.Pattern pattern = new org.appng.xml.platform.Pattern();
				pattern.setRegexp(((Pattern) annotation).regexp());
				validationRule = pattern;
				getValidationNode(fieldDef).setPattern(pattern);
			} else if (annotation instanceof Min) {
				validationRule = setMin(getValidationNode(fieldDef), new BigDecimal(((Min) annotation).value()));
			} else if (annotation instanceof Max) {
				validationRule = setMax(getValidationNode(fieldDef), new BigDecimal(((Max) annotation).value()));
			} else if (annotation instanceof DecimalMin) {
				validationRule = setMin(getValidationNode(fieldDef), new BigDecimal(((DecimalMin) annotation).value()));
			} else if (annotation instanceof DecimalMax) {
				validationRule = setMax(getValidationNode(fieldDef), new BigDecimal(((DecimalMax) annotation).value()));
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
				getValidationNode(fieldDef).setFileUpload(fileUpload);
			} else {
				addRule(fieldDef, constraintDescriptor, annotation, getValidationNode(fieldDef));
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
			List<Method> methods = new ArrayList<>(Arrays.asList(annotationType.getMethods()));
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
			LOGGER.error(String.format("error processing annotation %s", annotation), e);
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
			String messageTemplate = constraintDescriptor.getMessageTemplate();
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
			addMessage(field, validationRule, messageTemplate, messageText);
		} catch (Exception e) {
			LOGGER.warn(String.format("error while getting message from %s", constraintDescriptor), e);
		}
	}

	protected void addMessage(FieldDef field, ValidationRule validationRule, String messageTemplate,
			String messageText) {
		Message message = getMessage(field, field.getBinding(), messageTemplate, messageText);
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
			validateFields(bean, fp.getFields(), groups);
		}
	}

	private void validateFields(Object bean, List<FieldDef> fields, Class<?>... groups) {
		for (FieldDef fieldDef : fields) {
			if (!FieldType.OBJECT.equals(fieldDef.getType()) && !FieldType.LIST_OBJECT.equals(fieldDef.getType())) {
				if (!Boolean.parseBoolean(fieldDef.getReadonly())) {
					String reference = fieldDef.getBinding();
					boolean isArray = reference.contains(INDEXED);
					if (isArray) {
						String arrayProperty = fieldDef.getBinding().substring(0,
								fieldDef.getBinding().indexOf(INDEXED));
						BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
						Object collectionValue = beanWrapper.getPropertyValue(arrayProperty);
						int size = ((Collection<?>) collectionValue).size();
						for (int i = 0; i < size; i++) {
							String indexedPropertyName = String.format("%s[%s]", arrayProperty, i);
							Object item = beanWrapper.getPropertyValue(indexedPropertyName);
							addFieldMessage(validator.validateProperty(item, fieldDef.getName(), groups),
									indexedPropertyName, fieldDef.getName(), fieldDef);
						}
					} else {
						try {
							addFieldMessage(validator.validateProperty(bean, reference, groups), null,
									fieldDef.getBinding(), fieldDef);
						} catch (IllegalArgumentException e) {
							// may occur when using properties like foo['bar']
						}
					}
				}
			}
			validateFields(bean, fieldDef.getFields(), groups);
		}

	}

	private void addFieldMessage(Set<ConstraintViolation<Object>> violations, String propertyRoot,
			String relativePropertyPath, FieldDef fieldDef) {
		for (ConstraintViolation<Object> cv : getSortedViolations(violations)) {
			String constraintPath = cv.getPropertyPath().toString();
			String expectedBinding = constraintPath.replaceAll(INDEX_PATTERN, INDEXED);
			int count = 0;
			String absolutePropertyPath = null == propertyRoot ? constraintPath : propertyRoot + "." + constraintPath;
			if (constraintPath.equals(relativePropertyPath) || expectedBinding.equals(relativePropertyPath)) {
				Message errorMessage = addFieldMessage(fieldDef, absolutePropertyPath, cv);
				LOGGER.debug("Added message '{}' to field {}", errorMessage.getContent(), absolutePropertyPath);
				count++;
			}
			LOGGER.debug("Added {} messages for field {}", count, absolutePropertyPath);
		}
	}

	private Collection<ConstraintViolation<Object>> getSortedViolations(Set<ConstraintViolation<Object>> violations) {
		List<ConstraintViolation<Object>> sortedViolations = new ArrayList<>(violations);
		sortedViolations.sort((ConstraintViolation<Object> cv1, ConstraintViolation<Object> cv2) -> cv1
				.getConstraintDescriptor().getAnnotation().getClass().getName()
				.compareTo(cv2.getConstraintDescriptor().getAnnotation().getClass().getName()));
		return sortedViolations;
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
			addFieldMessage(field, field.getBinding(), cv);
		}
	}

	private Message addFieldMessage(FieldDef field, String reference, ConstraintViolation<?> cv) {
		Messages messages = field.getMessages();
		if (null == messages) {
			messages = new Messages();
			messages.setRef(field.getBinding());
			field.setMessages(messages);
		}
		Message message = getMessage(field, reference, cv.getMessageTemplate(), cv.getMessage());
		messages.getMessageList().add(message);
		return message;
	}

	private Message getMessage(FieldDef field, String reference, String messageTemplate, String messageText) {
		Message message = new Message();
		message.setRef(reference);
		message.setClazz(MessageType.ERROR);
		message.setContent(messageText);
		message.setCode(messageTemplate);
		return message;
	}

}
