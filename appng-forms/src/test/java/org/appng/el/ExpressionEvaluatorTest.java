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
package org.appng.el;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appng.el.ExpressionEvaluatorTest.Dummy.Type;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ExpressionEvaluatorTest {

	private static final Integer FIVE = Integer.valueOf(5);
	private static final Integer SIX = Integer.valueOf(6);
	private HashMap<String, Object> parameters;
	private ExpressionEvaluator evaluator;

	@Before
	public void setup() {
		Locale.setDefault(Locale.ENGLISH);
		parameters = new HashMap<String, Object>();
		parameters.put("a", FIVE);
		parameters.put("b", SIX);
		Map<String, Object> nested = new HashMap<String, Object>();
		nested.put("foo", 5);
		parameters.put("SESSION", nested);
		evaluator = new ExpressionEvaluator(parameters);
	}

	@Test
	public void testNestedMap() {
		Assert.assertTrue(evaluator.evaluate("${SESSION.foo eq 5}"));
		Assert.assertTrue(evaluator.evaluate("${SESSION['foo'] eq 5}"));
		Assert.assertTrue(evaluator.evaluate("${SESSION.foo ne null}"));
		Assert.assertFalse(evaluator.evaluate("${SESSION['bar'] eq 5}"));
		Assert.assertTrue(evaluator.evaluate("${SESSION.bar eq null}"));
	}

	@Test
	public void testParamIsNull() {
		Assert.assertTrue(evaluator.evaluate("${uizuihm eq null}"));
	}

	@Test
	public void testIsExpression() {
		Assert.assertTrue(evaluator.isExpression("${c eq null}"));
		Assert.assertTrue(evaluator.isExpression("${sdsdfsdf}"));
		Assert.assertFalse(evaluator.isExpression("#{sdsdfsdf}"));
		Assert.assertFalse(evaluator.isExpression("$sdsdfsdf}"));
		Assert.assertFalse(evaluator.isExpression("{sdsdfsdf}"));
		Assert.assertFalse(evaluator.isExpression("${sdsdfsdf"));
	}

	@Test
	public void testStringValue() {
		Assert.assertEquals("false", evaluator.evaluate("${1 eq 2}", String.class));
		Assert.assertEquals("true", evaluator.evaluate("${1 eq 2-1}", String.class));
		Assert.assertEquals("true", evaluator.getString("${1 eq 2-1}"));
		Assert.assertEquals("foobar", evaluator.getString("foobar"));
	}

	@Test
	public void testBooleanValue() {
		Assert.assertEquals("false", evaluator.evaluate("${1 eq 2}", String.class));
		Assert.assertEquals("true", evaluator.evaluate("${1 eq 2-1}", String.class));
		Assert.assertEquals(Boolean.TRUE, evaluator.getBoolean("${1 eq 2-1}"));
		Assert.assertFalse(evaluator.getBoolean("foobar"));
		Assert.assertNull(evaluator.getBoolean(null));
	}

	@Test
	public void testNoVariable() {
		assertTrue("${c eq null}");
		assertFalse("${a eq c}");
		assertFalse("${a eq null}");
	}

	@Test
	public void testDash() {
		parameters.put("site-id", "");
		evaluator = new ExpressionEvaluator(parameters);
		Object result = evaluator.evaluate("${site-id eq ''}", boolean.class);
		System.out.println(result);
		result = evaluator.evaluate("${site-id}", int.class);
		System.out.println(result);
	}

	@Test
	public void testTernary() {
		evaluator = new ExpressionEvaluator(parameters);
		String result = evaluator.evaluate("${a eq 5 ? 'is five' : 'is not five'}", String.class);
		Assert.assertEquals("is five", result);
		result = evaluator.evaluate("${b eq 5 ? 'is five' : 'is not five'}", String.class);
		Assert.assertEquals("is not five", result);
		result = evaluator.evaluate("${a eq null ? 'is null': 'is not null'}", String.class);
		Assert.assertEquals("is not null", result);
		result = evaluator.evaluate("${c eq null ? 'is null': 'is not null'}", String.class);
		Assert.assertEquals("is null", result);
	}

	@Test
	public void testNameConventions() {
		parameters.put("a", "1");
		parameters.put("a_", "2");
		parameters.put("A", "3");
		parameters.put("A_", "4");
		parameters.put("ab", "5");
		parameters.put("aB", "6");
		parameters.put("a_b", "7");
		parameters.put("aB_", "8");
		evaluator = new ExpressionEvaluator(parameters);
		Assert.assertEquals(Integer.valueOf(1), evaluator.evaluate("${a}", Integer.class));
		Assert.assertEquals(Integer.valueOf(2), evaluator.evaluate("${a_}", Integer.class));
		Assert.assertEquals(Integer.valueOf(3), evaluator.evaluate("${A}", Integer.class));
		Assert.assertEquals(Integer.valueOf(4), evaluator.evaluate("${A_}", Integer.class));
		Assert.assertEquals(Integer.valueOf(5), evaluator.evaluate("${ab}", Integer.class));
		Assert.assertEquals(Integer.valueOf(6), evaluator.evaluate("${aB}", Integer.class));
		Assert.assertEquals(Integer.valueOf(7), evaluator.evaluate("${a_b}", Integer.class));
		Assert.assertEquals(Integer.valueOf(8), evaluator.evaluate("${aB_}", Integer.class));
	}

	@Test
	public void testFunction() {
		assertTrue("${a.toString() == 5}");
		assertFalse("${b.toString() == 5}");
	}

	@Test
	public void testBoolean() {
		assertFalse("${a == b}");
		assertTrue("${a < b}");
		assertTrue("${a <= b}");
		assertFalse("${a > b}");
		assertFalse("${a >= b}");
		assertFalse("${a eq b}");
		assertTrue("${a ne b}");
		assertTrue("${a lt b}");
		assertFalse("${a gt b}");
	}

	@Test
	public void testNotDefinedInteger() throws Exception {
		boolean result = evaluator.evaluate("${c eq null}");
		System.out.println(result);
	}

	@Test
	public void testInteger() throws Exception {
		evaluator.addFunction("max", Math.class.getMethod("max", int.class, int.class));
		evaluator.addFunction("min", Math.class.getMethod("min", int.class, int.class));
		Method sqrt = Math.class.getMethod("sqrt", double.class);
		evaluator.addFunction("sqrt", sqrt);
		evaluator.addFunction("ait", "sqrt", sqrt);

		Integer result = evaluator.evaluate("${min(a,b)}", Integer.class);
		Assert.assertEquals(FIVE, result);
		result = evaluator.evaluate("${max(a,b)}", Integer.class);
		Assert.assertEquals(SIX, result);

		result = evaluator.evaluate("${a-b}", Integer.class);
		Assert.assertEquals(Integer.valueOf(-1), result);

		result = evaluator.evaluate("${a+b}", Integer.class);
		Assert.assertEquals(Integer.valueOf(11), result);

		result = evaluator.evaluate("${a*b}", Integer.class);
		Assert.assertEquals(Integer.valueOf(30), result);

		result = evaluator.evaluate("${(a*a) - (b/b)}", Integer.class);
		Assert.assertEquals(Integer.valueOf(24), result);

		Double dbl = evaluator.evaluate("${sqrt(25)}", Double.class);
		Assert.assertEquals(Double.valueOf(FIVE), dbl);

		dbl = evaluator.evaluate("${ait:sqrt(36)}", Double.class);
		Assert.assertEquals(Double.valueOf(SIX), dbl);
	}

	@Test
	public void testMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("foo", "bar");
		evaluator.setVariable("map", map);
		assertTrue("${map['foo'] eq 'bar'}");
		String result = evaluator.evaluate("${map['foo']}", String.class);
		Assert.assertEquals("bar", result);
	}

	@Test
	public void testArray() {
		int[] numbers = new int[] { 1, 2, 3, 4 };
		evaluator.setVariable("numbers", numbers);
		assertTrue("${numbers[0] == 1}");
		Assert.assertEquals(Integer.valueOf(5), evaluator.evaluate("${numbers[0] + numbers[3]}", Integer.class));
	}

	//
	public static <T> List<T> asList(T[] args) {
		return Arrays.asList(args);
	}

	@Test
	public void asList() throws Exception {
		Method method = getClass().getMethod("asList", Object[].class);
		evaluator.addFunction("asList", method);
		Object[] args = new Object[] { 1, "4", 56D };
		evaluator.setVariable("args", args);
		List<?> result = evaluator.evaluate("${asList(args)}", List.class);
		Assert.assertEquals(result, Arrays.asList(args));
	}

	@Test
	public void testDummy() throws Exception {
		Dummy d1 = new Dummy();
		d1.setType(Type.ON);
		d1.setBar("bar");
		d1.setFoo(42);
		ArrayList<Dummy> dummies = new ArrayList<>();
		Dummy d2 = new Dummy();
		dummies.add(d2);
		d2.setFoo(23);
		d2.setType(Type.OFF);
		d1.setDummies(dummies);
		evaluator.setVariable("dummy", d1);
		evaluator.setVariable("type", Type.ON);

		assertTrue("${dummy.type eq 'ON'}");
		assertTrue("${dummy.type eq type}");
		assertFalse("${dummy.type eq 'OFF'}");
		assertFalse("${dummy.dummies[0].type eq dummy.type}");
		assertTrue("${dummy.foo eq 42}");
		assertTrue("${dummy.bar eq 'bar'}");
		assertTrue("${empty dummy.dummies[0].dummies}");
		assertTrue("${dummy.dummies[0].foo eq 23}");

		evaluator.addFunction("size", ExpressionEvaluatorTest.class.getMethod("size", Collection.class));
		assertTrue("${size(dummy.dummies) eq 1}");
	}

	public static int size(Collection<?> collection) {
		return collection == null ? 0 : collection.size();
	}

	private void assertFalse(String expression) {
		boolean result = evaluator.evaluate(expression);
		Assert.assertFalse(expression, result);
	}

	private void assertTrue(String expression) {
		boolean result = evaluator.evaluate(expression);
		Assert.assertTrue(expression, result);
	}

	public static class Dummy {

		public static enum Type {
			ON, OFF;
		}

		private Integer foo;
		private String bar;
		private Type type;
		private List<Dummy> dummies;

		public Integer getFoo() {
			return foo;
		}

		public void setFoo(Integer foo) {
			this.foo = foo;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		public List<Dummy> getDummies() {
			return dummies;
		}

		public void setDummies(List<Dummy> dummies) {
			this.dummies = dummies;
		}

		public String toString() {
			return "foo=" + foo + ", bar=" + null + ", dummies=" + dummies;
		}

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}
	}
}
