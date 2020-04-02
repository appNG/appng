/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.core.templating;

import org.thymeleaf.context.EngineContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

/**
 * A Base class for thymeleaf interceptors providing some utility functionality
 * 
 * @author Claus Stümke
 *
 */
public abstract class ThymeleafReplaceInterceptorBase implements ThymeleafReplaceInterceptor {

	protected void addVariable(ITemplateContext context, String key, Object value) {
		((EngineContext) context).setVariable(key, value);
	}

	protected FragmentExpression.ExecutedFragmentExpression getExecutedFragmentExpression(ITemplateContext context,
			String attributeValue) {
		final IStandardExpressionParser expressionParser = StandardExpressions
				.getExpressionParser(context.getConfiguration());

		final FragmentExpression fragmentExpression = (FragmentExpression) expressionParser.parseExpression(context,
				"~{" + attributeValue.trim() + "}");

		final FragmentExpression.ExecutedFragmentExpression executedFragmentExpression = FragmentExpression
				.createExecutedFragmentExpression(context, fragmentExpression);
		return executedFragmentExpression;
	}

}
