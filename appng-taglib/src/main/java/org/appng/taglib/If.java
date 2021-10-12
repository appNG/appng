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
package org.appng.taglib;

import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.appng.el.ExpressionEvaluator;

import lombok.extern.slf4j.Slf4j;

/**
 * Processes the taglet's body if the given condition evaluates to be true. In the condition, any request parameter can
 * be used. Since the condition is evaluated by an {@link ExpressionEvaluator}, the JSP expression language needs to be
 * used.
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;!-- assumed there's a request parameter named 'foo'-->
 * &lt;appNG:if condition="foo eq 'bar'">Bar!&lt;/appNG:if>
 * </pre>
 * 
 * @see ExpressionEvaluator
 * 
 * @author Matthias Herlitzius
 */
@Slf4j
public class If extends TagSupport {

	private static final long serialVersionUID = 1L;
	private String condition;

	@Override
	public int doStartTag() throws JspException {
		Map<String, String> parameters = TagletAdapter.getRequest(pageContext).getParameters();
		ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(parameters);
		boolean matches = expressionEvaluator.evaluate("${" + condition + "}");
		if (matches) {
			LOGGER.debug("{} = {}, evaluating body", condition, matches);
			return EVAL_BODY_INCLUDE;
		}
		LOGGER.debug("{} = {}, skipping body", condition, matches);
		return SKIP_BODY;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
