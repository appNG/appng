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
package org.appng.el;

import java.util.HashMap;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

/**
 * A {@link Map}-based {@link javax.el.VariableMapper} implementation.
 * 
 * @author Matthias Müller
 * 
 */
class VariableMapper extends javax.el.VariableMapper {

	private final Map<String, ValueExpression> map = new HashMap<String, ValueExpression>();
	private ExpressionFactory ef;
	private ELContext ctx;

	VariableMapper(ELContext ctx, ExpressionFactory ef) {
		this.ef = ef;
		this.ctx = ctx;
	}

	public final ValueExpression setVariable(String variable, ValueExpression expression) {
		return map.put(variable, expression);
	}

	public final ValueExpression resolveVariable(String variable) {
		ValueExpression result = map.get(variable);
		if (null == result) {
			result = ef.createValueExpression(null, Void.class);
		}
		return result;
	}

	private String getVariable(String name) {
		ValueExpression variable = map.get(name);
		if (null == variable) {
			return "null";
		}
		return variable.getValue(ctx).toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String name : map.keySet()) {
			if (i++ > 0) {
				sb.append(", ");
			}
			sb.append("(" + name + " = " + getVariable(name) + ")");
		}
		return sb.toString();
	}

}
