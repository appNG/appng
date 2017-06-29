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
package org.appng.api.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ParameterSupport;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Labels;
import org.springframework.context.MessageSource;

/**
 * 
 * This class is responsible for setting the value of a {@link Label}. This is done by using the {@link Label}s id to
 * retrieve the message from a {@link MessageSource}. The {@link Label}'s
 * 
 * @author Matthias Müller
 * 
 */
public class LabelSupport {

	private static final String STRING_AFFIX = "'";
	private static final String PARAM_SEPARATOR = ",";
	private static final String EXPR_PREFIX = "$";
	protected static final String LABEL_SUFFIX = "}";
	protected static final String LABEL_PREFIX = "{";
	private final MessageSource messageSource;
	private final Locale locale;

	public LabelSupport(MessageSource messageSource, Locale locale) {
		this.messageSource = messageSource;
		this.locale = locale;
	}

	public final void setLabels(Config config, ExpressionEvaluator expressionEvaluator, ParameterSupport fieldParameters) {
		if (null != config) {
			setLabel(config.getTitle(), expressionEvaluator, fieldParameters);
			setLabel(config.getDescription(), expressionEvaluator, fieldParameters);
			setLabels(config.getLabels(), expressionEvaluator, fieldParameters);
		}
	}

	public final void setLabels(Labels labels, ExpressionEvaluator expressionEvaluator, ParameterSupport fieldParameters) {
		if (null != labels) {
			for (Label label : labels.getLabels()) {
				setLabel(label, expressionEvaluator, fieldParameters);
			}
		}
	}

	public final void setLabel(Label label, ExpressionEvaluator expressionEvaluator, ParameterSupport fieldParameters) {
		if (null != label) {
			String key = label.getId();
			String value = label.getValue();
			if (StringUtils.isNotBlank(value) && value.startsWith(EXPR_PREFIX)) {
				String message = value;
				if (null != fieldParameters) {
					message = fieldParameters.replaceParameters(message);
				}
				message = expressionEvaluator.evaluate(message, String.class);
				label.setValue(message);
			} else {
				if (StringUtils.isBlank(key) && StringUtils.isNotBlank(value)
						&& !StringUtils.startsWith(value, LABEL_PREFIX)) {
					key = value;
				}
				if (StringUtils.isNotBlank(key) && !key.startsWith(LABEL_PREFIX)) {
					String defaultValue = key;
					List<Object> args = new ArrayList<Object>();
					int idxParamStart = key.indexOf('[');
					int idxParamEnd = key.indexOf(']');
					if (idxParamStart > 0 && idxParamEnd > 0) {
						label.setId(key.substring(0, idxParamStart));
						label.setParams(key.substring(idxParamStart + 1, idxParamEnd));
					} else {
						label.setId(key);
					}

					String params = label.getParams();
					if (null != params) {
						String[] splitted = params.split(PARAM_SEPARATOR);
						for (String param : splitted) {
							param = param.trim();
							if (param.startsWith(STRING_AFFIX) && param.endsWith(STRING_AFFIX)) {
								param = param.substring(1, param.length() - 1);
							}
							boolean isFieldParam = param.startsWith("#{");
							if (isFieldParam && null != fieldParameters) {
								param = fieldParameters.replaceParameters(param);
							}
							boolean paramUsesCurrent = param.startsWith(EXPR_PREFIX + "{" + AdapterBase.CURRENT + "");
							String checkCurrent = EXPR_PREFIX + "{" + AdapterBase.CURRENT + " ne null}";
							if (!isFieldParam && (!paramUsesCurrent || expressionEvaluator.evaluate(checkCurrent))) {
								param = expressionEvaluator.evaluate(param, String.class);
							}
							args.add(param);
						}
					}

					String message = messageSource.getMessage(label.getId(), args.toArray(), defaultValue, locale);
					label.setValue(message);
				}

			}
		}
	}

}
