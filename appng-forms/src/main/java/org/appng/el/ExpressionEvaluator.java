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
package org.appng.el;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;

import org.apache.jasper.el.ELContextImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * Used for evaluating expressions that conform the
 * <a href="https://docs.oracle.com/javaee/7/tutorial/jsf-el.htm">Expression Language 2.2</a> syntax.<br/>
 * A valid expression must start with ${ and end with }. A list of the allowed operators an be found
 * <a href="https://docs.oracle.com/javaee/7/tutorial/jsf-el005.htm#BNAIK">here</a>.
 * <p/>
 * Example:
 * 
 * <pre>
 * Map&lt;String, Object&gt; parameters = new HashMap&lt;String, Object&gt;();
 * parameters.put(&quot;intValue&quot;, 5);
 * parameters.put(&quot;stringValue&quot;, &quot;foobar&quot;);
 * ExpressionEvaluator ee = new ExpressionEvaluator(parameters);
 * org.junit.Assert.assertEquals(&quot;foobar&quot;, ee.evaluate(&quot;${stringValue}&quot;, String.class));
 * org.junit.Assert.assertEquals(Integer.valueOf(5), ee.evaluate(&quot;${intValue}&quot;, Integer.class));
 * org.junit.Assert.assertTrue(ee.evaluate(&quot;${intValue eq (3+2)}&quot;));
 * org.junit.Assert.assertTrue(ee.evaluate(&quot;${(1 eq (2/2)) &amp;&amp; (2&lt;3)}&quot;));
 * org.junit.Assert.assertTrue(ee.evaluate(&quot;${stringValue.length() == 6}&quot;));
 * </pre>
 * 
 * @author Matthias Müller
 */
@Slf4j
public final class ExpressionEvaluator {

	private ExpressionFactory ef;
	private ELContext ctx;
	private javax.el.VariableMapper variableMapper;
	private final Map<String, Method> methods = new HashMap<>();

	/**
	 * Creates a new {@link ExpressionEvaluator} using the given variables.
	 * 
	 * @param variables
	 *                  a {@link Map} of variables to use
	 */
	public ExpressionEvaluator(Map<String, ?> variables) {
		this.ef = ExpressionFactory.newInstance();
		CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(new MapELResolver());
		resolver.add(new ListELResolver());
		resolver.add(new ArrayELResolver());
		resolver.add(new BeanELResolver());
		this.ctx = new ELContextImpl(resolver);
		this.variableMapper = new VariableMapper(ctx, ef);
		((ELContextImpl) ctx).setVariableMapper(variableMapper);
		((ELContextImpl) ctx).setFunctionMapper(getFunctionMapper());
		this.setVariables(variables);
	}

	/**
	 * Creates a new {@link ExpressionEvaluator} using the given {@link javax.el.VariableMapper}.
	 * 
	 * @param variableMapper
	 *                       the {@link javax.el.VariableMapper} to use
	 */
	public ExpressionEvaluator(javax.el.VariableMapper variableMapper) {
		this.ef = ExpressionFactory.newInstance();
		CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(new MapELResolver());
		resolver.add(new ListELResolver());
		resolver.add(new ArrayELResolver());
		resolver.add(new BeanELResolver());
		this.ctx = new ELContextImpl(resolver);
		this.variableMapper = variableMapper;
		((ELContextImpl) ctx).setVariableMapper(variableMapper);
		((ELContextImpl) ctx).setFunctionMapper(getFunctionMapper());
	}

	/**
	 * Evaluates the given expression to an object of the given targetType
	 * 
	 * @param expression
	 *                   the expression to parse
	 * @param targetType
	 *                   the expected target type
	 * 
	 * @return an object of the given type
	 * 
	 * @throws NullPointerException
	 *                              if targetType is {@code null}
	 * @throws ELException
	 *                              if the expression has syntactical errors
	 */
	@SuppressWarnings("unchecked")
	public final <T> T evaluate(String expression, Class<T> targetType) {
		ValueExpression ve = ef.createValueExpression(ctx, expression, targetType);
		Object value = ve.getValue(ctx);
		T result = (T) value;

		if (LOGGER.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder(expression + " = " + result + " [");
			sb.append(variableMapper.toString());
			sb.append("]");
			LOGGER.debug(sb.toString());
		}
		return result;
	}

	/**
	 * Evaluates the given expression to a {@code boolean}.
	 * 
	 * @param expression
	 *                   the expression to evaluate
	 * 
	 * @return evaluation result
	 */
	public final boolean evaluate(String expression) {
		return evaluate(expression, boolean.class);
	}

	/**
	 * Adds a prefixed function to this {@link ExpressionEvaluator}. The given {@link Method} must be
	 * {@code static}.<br/>
	 * Example:
	 * 
	 * <pre>
	 * ExpressionEvaluator ee = ...;
	 * ee.addFunction("math","max", Math.class.getMethod("max", int.class, int.class));
	 * org.junit.Assert.assertEquals(Integer.valueOf(47), ee.evaluate("${math:max(47,11)}", Integer.class));
	 * </pre>
	 * 
	 * @param prefix
	 *               the prefix for the method
	 * @param name
	 *               the name of the method
	 * @param method
	 *               the {@link Method} to add as a function
	 */
	public final void addFunction(String prefix, String name, Method method) {
		putFunction(prefix + ":" + name, method);
	}

	/**
	 * Adds a function to this {@link ExpressionEvaluator}. The given {@link Method} must be {@code static}.<br/>
	 * {@code static}.<br/>
	 * Example:
	 * 
	 * <pre>
	 * ExpressionEvaluator ee = ...;
	 * ee.addFunction("max", Math.class.getMethod("max", int.class, int.class));
	 * org.junit.Assert.assertEquals(Integer.valueOf(47), ee.evaluate("${max(47,11)}", Integer.class));
	 * </pre>
	 * 
	 * @param name
	 *               the name of the method
	 * @param method
	 *               the {@link Method} to add as a function
	 */
	public final void addFunction(String name, Method method) {
		putFunction(":" + name, method);
	}

	private void putFunction(String methodName, Method method) {
		methods.put(methodName, method);
		LOGGER.debug("registered function '{}' with method '{}'.", methodName, method);
	}

	private FunctionMapper getFunctionMapper() {
		FunctionMapper functionMapper = new FunctionMapper() {
			@Override
			public final Method resolveFunction(String prefix, String localname) {
				Method method = methods.get(prefix + ":" + localname);
				return method;
			}
		};
		return functionMapper;
	}

	/**
	 * Sets the variable with the given name to the given value.
	 * 
	 * @param name
	 *              the name of the variable
	 * @param value
	 *              the value of the variable (must not be {@code null})
	 */
	public final void setVariable(String name, Object value) {
		setVariable(name, value, value.getClass());
	}

	/**
	 * Sets the variable with the given name, which has the given type, to the given value.
	 * 
	 * @param name
	 *              the name of the variable
	 * @param value
	 *              the value of the variable (must not be {@code null})
	 * @param type
	 *              the type of the variable (must not be {@code null})
	 */
	private final void setVariable(String name, Object value, Class<?> type) {
		ValueExpression expression = ef.createValueExpression(value, type);
		variableMapper.setVariable(name, expression);
		if (null == value) {
			LOGGER.trace("setting variable '{}' to null", name);
		} else if (value instanceof String || value instanceof Number) {
			LOGGER.trace("setting variable '{}' of type '{}' to value '{}'", name, type.getName(), value.toString());
		}
	}

	/**
	 * Calls {@link #setVariable(String, Object)} for each entry of the given map.
	 * 
	 * @param variables
	 *                  a {@link Map} of variables
	 */
	public final void setVariables(Map<String, ?> variables) {
		for (String key : variables.keySet()) {
			setVariable(key, variables.get(key));
		}
	}

	/**
	 * Checks whether the given {@link String} is a valid expression (starts with ${ and ends with }).
	 * 
	 * @param value
	 *              the String to check
	 * 
	 * @return {@code true} if the given value is a valid expression, {@code false} otherwise
	 */
	public boolean isExpression(String value) {
		return null != value && value.matches("\\$\\{.*\\}");
	}

	/**
	 * Evaluates the given expression to a {@link String}, if possible.
	 * 
	 * @param expression
	 *                   the expression to evaluate
	 * 
	 * @return
	 *         <ul>
	 *         <li>the {@link String} evaluated from the expression, in cases it's a valid expression
	 *         <li>the given expression itself, otherwise
	 *         </ul>
	 * 
	 * @see #isExpression(String)
	 */
	public String getString(String expression) {
		if (isExpression(expression)) {
			return evaluate(expression, String.class);
		}
		return expression;
	}

	/**
	 * Evaluates the given expression to a {@link Boolean}, if possible.
	 * 
	 * @param expression
	 *                   the expression to evaluate
	 * 
	 * @return
	 *         <ul>
	 *         <li>the {@link Boolean} evaluated from the expression, in cases it's a valid expression
	 *         <li>{@code Boolean.valueOf(expression)} if the expression is not {@code null} but is not a valid
	 *         expression
	 *         <li>{@code null} if the expression is {@code null}
	 *         </ul>
	 * 
	 * @see #isExpression(String)
	 */
	public Boolean getBoolean(String expression) {
		if (isExpression(expression)) {
			return evaluate(expression);
		} else if (expression != null) {
			return Boolean.valueOf(expression);
		}
		return null;
	}

}
