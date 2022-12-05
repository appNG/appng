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
package org.appng.api.support.field;

import java.util.List;
import java.util.function.Function;

import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.api.ParameterSupport;
import org.appng.api.support.ElementHelper;
import org.appng.api.support.HashParameterSupport;
import org.appng.api.support.LabelSupport;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkable;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.OpenapiAction;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Base {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#LINKPANEL}.
 * 
 * @author Matthias Müller
 */
@Slf4j
class LinkPanelFieldHandler extends ConverterBase {

	private LabelSupport labelSupport;

	LinkPanelFieldHandler(ExpressionEvaluator expressionEvaluator, Environment environment,
			MessageSource messageSource) {
		setExpressionEvaluator(expressionEvaluator);
		setEnvironment(environment);
		setMessageSource(messageSource);
		this.labelSupport = new LabelSupport(getMessageSource(), getEnvironment().getLocale());
	}

	@Override
	public Datafield addField(DatafieldOwner dataFieldOwner, FieldWrapper fieldWrapper) {
		Linkpanel linkpanel = fieldWrapper.getLinkpanel();
		if (null != linkpanel) {
			if (linkpanel.getLocation().equals(PanelLocation.INLINE)) {
				Linkpanel copy = new Linkpanel();
				String panelId = linkpanel.getId();
				copy.setId(panelId);
				copy.setLocation(linkpanel.getLocation());
				List<Linkable> links = linkpanel.getLinks();
				for (Linkable link : links) {
					Condition linkCondition = link.getCondition();
					boolean showDisabled = Boolean.TRUE.equals(link.isShowDisabled());
					boolean conditionMatches = ElementHelper.conditionMatches(expressionEvaluator, linkCondition);
					if (conditionMatches || showDisabled) {
						Function<Linkable, String> getTarget;
						Function<String, Void> setTarget;
						Linkable linkCopy;

						HashParameterSupport fieldParams = new HashParameterSupport(dataFieldOwner.getFieldValues());
						fieldParams.allowDotInName();

						if (link instanceof Link) {
							linkCopy = new Link();
							((Link) linkCopy).setMode(((Link) link).getMode());
							((Link) linkCopy).setId(((Link) link).getId());
							getTarget = l -> ((Link) link).getTarget();
							setTarget = t -> {
								((Link) linkCopy).setTarget(t);
								return null;
							};
						} else {
							OpenapiAction actionCopy = new OpenapiAction();
							OpenapiAction originalLink = (OpenapiAction) link;
							actionCopy.setId(originalLink.getId());
							actionCopy.setEventId(originalLink.getEventId());
							actionCopy.setInteractive(originalLink.isInteractive());
							getTarget = l -> originalLink.getTarget();
							setTarget = t -> {
								actionCopy.setTarget(t);
								return null;
							};

							Params params = originalLink.getParams();
							if (null != params) {
								actionCopy.setParams(new Params());
								params.getParam().forEach(p -> {
									Param param = new Param();
									param.setName(p.getName());
									String value = fieldParams.replaceParameters(p.getValue());
									param.setValue(expressionEvaluator.evaluate(value, String.class));
									actionCopy.getParams().getParam().add(param);
								});
							}

							linkCopy = actionCopy;
						}

						setAttributes(link, getTarget, setTarget, showDisabled, conditionMatches, linkCopy, fieldParams);
						copy.getLinks().add(linkCopy);
					}
				}
				dataFieldOwner.getLinkpanels().add(copy);
			}
		} else {
			getLog().warn("linkpanel for field '" + fieldWrapper.getBinding() + "' is null!");
		}
		return null;
	}

	public void setAttributes(Linkable link, Function<Linkable, String> getTarget, Function<String, Void> setTarget,
			boolean showDisabled, boolean conditionMatches, Linkable linkCopy, ParameterSupport fieldParams) {
		linkCopy.setLabel(copyLabel(fieldParams, link.getLabel()));
		linkCopy.setIcon(link.getIcon());
		if (showDisabled && !conditionMatches) {
			linkCopy.setDisabled(true);
			setTarget.apply("");
		} else {
			String target = fieldParams.replaceParameters(getTarget.apply(link));
			setTarget.apply((expressionEvaluator.evaluate(target, String.class)));
			if (link.getDefault() != null) {
				linkCopy.setDefault(expressionEvaluator.evaluate(link.getDefault(), String.class));
			}
			linkCopy.setConfirmation(copyLabel(fieldParams, link.getConfirmation()));
		}
	}

	protected Label copyLabel(ParameterSupport fieldParameters, Label original) {
		Label copy = null;
		if (null != original) {
			copy = new Label();
			copy.setId(original.getId());
			copy.setParams(original.getParams());
			labelSupport.setLabel(copy, expressionEvaluator, fieldParameters);
		}
		return copy;
	}

	@Override
	protected Logger getLog() {
		return LOGGER;
	}

}
