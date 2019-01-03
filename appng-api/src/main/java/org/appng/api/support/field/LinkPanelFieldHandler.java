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
package org.appng.api.support.field;

import java.util.List;

import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.api.ParameterSupport;
import org.appng.api.support.HashParameterSupport;
import org.appng.api.support.LabelSupport;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.PanelLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * 
 * Base {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#LINKPANEL}.
 * 
 * @author Matthias Müller
 * 
 */
class LinkPanelFieldHandler extends ConverterBase {

	protected static final Logger LOG = LoggerFactory.getLogger(LinkPanelFieldHandler.class);
	private LabelSupport labelSupport;

	LinkPanelFieldHandler(ExpressionEvaluator expressionEvaluator, Environment environment, MessageSource messageSource) {
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
				List<Link> links = linkpanel.getLinks();
				for (Link link : links) {
					Condition linkCondition = link.getCondition();
					boolean showDisabled = Boolean.TRUE.equals(link.isShowDisabled());
					boolean conditionMatches = null == linkCondition
							|| expressionEvaluator.evaluate(linkCondition.getExpression());
					if (conditionMatches || showDisabled) {
						Link linkCopy = new Link();
						linkCopy.setId(link.getId());
						HashParameterSupport fieldParams = new HashParameterSupport(dataFieldOwner.getFieldValues());
						fieldParams.allowDotInName();
						linkCopy.setLabel(copyLabel(fieldParams, link.getLabel()));
						if (showDisabled && !conditionMatches) {
							linkCopy.setDisabled(true);
							linkCopy.setTarget("");
						} else {
							String target = fieldParams.replaceParameters(link.getTarget());
							linkCopy.setTarget(expressionEvaluator.evaluate(target, String.class));
							if (link.getDefault() != null) {
								linkCopy.setDefault(expressionEvaluator.evaluate(link.getDefault(), String.class));
							}
							linkCopy.setConfirmation(copyLabel(fieldParams, link.getConfirmation()));
						}
						linkCopy.setMode(link.getMode());
						linkCopy.setIcon(link.getIcon());

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
		return LOG;
	}

}
