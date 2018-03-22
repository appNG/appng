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
package org.appng.api.support;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldProcessor;
import org.appng.api.MessageParam;
import org.appng.api.ParameterSupport;
import org.appng.api.PermissionProcessor;
import org.appng.api.Request;
import org.appng.api.RequestSupport;
import org.appng.api.ValidationProvider;
import org.appng.api.model.Subject;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.FormUpload;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Labels;
import org.appng.xml.platform.MetaData;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpHeaders;

/**
 * 
 * Default {@link Request} implementation, mostly delegating method-calls to one of the internal objects.
 * 
 * @author Matthias Müller
 * 
 */
public class ApplicationRequest implements Request {

	public static final String I18N_VAR = "i18n";
	private PermissionProcessor permissionProcessor;
	private LabelSupport labelSupport;
	private org.appng.forms.Request wrappedRequest;
	private RequestSupport requestSupport;
	private ValidationProvider validationProvider;

	private String redirectTarget;
	private ApplicationConfigProvider applicationConfigProvider;
	private List<String> urlParameters;
	private HttpHeaders headers;

	public ApplicationRequest() {

	}

	public ApplicationRequest(org.appng.forms.Request request, PermissionProcessor permissionProcessor,
			RequestSupport requestSupport) {
		this.permissionProcessor = permissionProcessor;
		this.wrappedRequest = request;
		this.headers = HttpHeaderUtils.parse(request.getHttpServletRequest());
		setRequestSupport(requestSupport);
	}

	public PermissionProcessor getPermissionProcessor() {
		return permissionProcessor;
	}

	public void setPermissionProcessor(PermissionProcessor permissionProcessor) {
		this.permissionProcessor = permissionProcessor;
	}

	public LabelSupport getLabelSupport() {
		return labelSupport;
	}

	public void setLabelSupport(LabelSupport labelSupport) {
		this.labelSupport = labelSupport;
	}

	public org.appng.forms.Request getWrappedRequest() {
		return wrappedRequest;
	}

	public void setWrappedRequest(org.appng.forms.Request wrappedRequest) {
		this.wrappedRequest = wrappedRequest;
	}

	public Environment getEnvironment() {
		return requestSupport.getEnvironment();
	}

	public RequestSupport getRequestSupport() {
		return requestSupport;
	}

	public void setRequestSupport(RequestSupport requestSupport) {
		this.requestSupport = requestSupport;
		this.labelSupport = new LabelSupport(requestSupport.getMessageSource(), getLocale());
	}

	public String getRedirectTarget() {
		return redirectTarget;
	}

	public void setRedirectTarget(String redirectTarget) {
		this.redirectTarget = redirectTarget;
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return requestSupport.canConvert(sourceType, targetType);
	}

	public void handleException(FieldProcessor fp, Exception e) throws BusinessException {
		requestSupport.handleException(fp, e);
	}

	public void addErrorMessage(FieldProcessor fp, MessageParam localizable) {
		requestSupport.addErrorMessage(fp, localizable);
	}

	public void addErrorMessage(FieldProcessor fp, MessageParam localizable, String fieldBinding) {
		requestSupport.addErrorMessage(fp, localizable, fieldBinding);
	}

	public <T> T convert(Object source, Class<T> targetType) {
		return requestSupport.convert(source, targetType);
	}

	public <T> void setPropertyValues(T source, T target, MetaData metaData) {
		requestSupport.setPropertyValues(source, target, metaData);
	}

	public <T> void setPropertyValue(T source, T target, String property) {
		requestSupport.setPropertyValue(source, target, property);
	}

	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return requestSupport.canConvert(sourceType, targetType);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return requestSupport.convert(source, sourceType, targetType);
	}

	public Object getBindObject(FieldProcessor fp, RequestContainer container, ClassLoader classLoader)
			throws BusinessException {
		return requestSupport.getBindObject(fp, container, classLoader);
	}

	public void fillBindObject(Object instance, FieldProcessor fp, RequestContainer container, ClassLoader classLoader)
			throws BusinessException {
		requestSupport.fillBindObject(instance, fp, container, classLoader);
	}

	public String getMessage(String key, Object... args) {
		return requestSupport.getMessage(key, args);
	}

	public MessageSource getMessageSource() {
		return requestSupport.getMessageSource();
	}

	public String getHost() {
		return wrappedRequest.getHost();
	}

	public Map<String, List<String>> getParametersList() {
		return wrappedRequest.getParametersList();
	}

	public Map<String, String> getParameters() {
		return wrappedRequest.getParameters();
	}

	public String getParameter(String name) {
		return wrappedRequest.getParameter(name);
	}

	public HttpServletRequest getHttpServletRequest() {
		return wrappedRequest.getHttpServletRequest();
	}

	public Set<String> getParameterNames() {
		return wrappedRequest.getParameterNames();
	}

	public String getEncoding() {
		return wrappedRequest.getEncoding();
	}

	public boolean hasParameter(String name) {
		return wrappedRequest.hasParameter(name);
	}

	public void setEncoding(String encoding) {
		wrappedRequest.setEncoding(encoding);
	}

	public List<String> getParameterList(String name) {
		return wrappedRequest.getParameterList(name);
	}

	public boolean isMultiPart() {
		return wrappedRequest.isMultiPart();
	}

	public boolean isPost() {
		return wrappedRequest.isPost();
	}

	public Map<String, List<FormUpload>> getFormUploads() {
		return wrappedRequest.getFormUploads();
	}

	public boolean isGet() {
		return wrappedRequest.isGet();
	}

	public boolean isValid() {
		return wrappedRequest.isValid();
	}

	public void setTempDir(File tempDir) {
		wrappedRequest.setTempDir(tempDir);
	}

	public List<FormUpload> getFormUploads(String name) {
		return wrappedRequest.getFormUploads(name);
	}

	public void setMaxSize(long maxSize) {
		wrappedRequest.setMaxSize(maxSize);
	}

	public void setMaxSize(long maxSize, boolean strict) {
		wrappedRequest.setMaxSize(maxSize, strict);
	}

	public void setAcceptedTypes(String uploadName, String... types) {
		wrappedRequest.setAcceptedTypes(uploadName, types);
	}

	public List<String> getAcceptedTypes(String uploadName) {
		return wrappedRequest.getAcceptedTypes(uploadName);
	}

	public void addParameters(Map<String, String> singleParameters) {
		wrappedRequest.addParameters(singleParameters);
	}

	public void addParameter(String key, String value) {
		wrappedRequest.addParameter(key, value);
	}

	public Subject getSubject() {
		return getEnvironment().getSubject();
	}

	public Locale getLocale() {
		return getEnvironment().getLocale();
	}

	public ExpressionEvaluator getExpressionEvaluator() {
		ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(wrappedRequest.getParameters());
		expressionEvaluator.setVariable(I18N_VAR, new I18n(this));
		return expressionEvaluator;
	}

	public ParameterSupport getParameterSupportDollar() {
		return new DollarParameterSupport(getParameters());
	}

	@Override
	public boolean isRedirect() {
		return StringUtils.isNotBlank(redirectTarget);
	}

	public void setLabels(Config config) {
		labelSupport.setLabels(config, getExpressionEvaluator(), null);
	}

	public final void setLabels(Config config, ParameterSupport fieldParameters) {
		labelSupport.setLabels(config, getExpressionEvaluator(), fieldParameters);
	}

	public void setLabel(Label label) {
		labelSupport.setLabel(label, getExpressionEvaluator(), null);
	}

	public void setLabels(Labels labels) {
		labelSupport.setLabels(labels, getExpressionEvaluator(), null);
	}

	public FieldConverter getFieldConverter() {
		return requestSupport.getFieldConverter();
	}

	public ApplicationConfigProvider getApplicationConfig() {
		return applicationConfigProvider;
	}

	public void setApplicationConfig(ApplicationConfigProvider applicationConfigProvider) {
		this.applicationConfigProvider = applicationConfigProvider;
	}

	public ValidationProvider getValidationProvider() {
		return validationProvider;
	}

	public void setValidationProvider(ValidationProvider validationProvider) {
		this.validationProvider = validationProvider;
	}

	public List<String> getUrlParameters() {
		return urlParameters;
	}

	public void setUrlParameters(List<String> urlParameters) {
		this.urlParameters = urlParameters;
	}

	public void validateBean(Object bean, FieldProcessor fp, Class<?>... groups) {
		validationProvider.validateBean(bean, fp, groups);
	}

	public void validateBean(Object bean, FieldProcessor fp, String[] excludeBindings, Class<?>... groups) {
		validationProvider.validateBean(bean, fp, excludeBindings, groups);
	}

	public void validateField(Object bean, FieldProcessor fp, String fieldBinding, Class<?>... groups) {
		validationProvider.validateField(bean, fp, fieldBinding, groups);
	}

	public void addValidationMetaData(MetaData metaData, ClassLoader classLoader, Class<?>... groups)
			throws ClassNotFoundException {
		validationProvider.addValidationMetaData(metaData, classLoader, groups);
	}

	public HttpHeaders headers() {
		return headers;
	}

}
