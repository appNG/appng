package org.appng.core.templating;

import org.thymeleaf.context.EngineContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;
import org.thymeleaf.standard.expression.StandardExpressions;

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
				.createExecutedFragmentExpression(context, fragmentExpression,
						StandardExpressionExecutionContext.NORMAL);
		return executedFragmentExpression;
	}

}
