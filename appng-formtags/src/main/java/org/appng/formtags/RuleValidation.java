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
package org.appng.formtags;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.FormUpload;
import org.appng.forms.Request;

/**
 * 
 * <table border="1">
 * <tr>
 * <th>rule</th>
 * <th>example</th>
 * </tr>
 * <tr>
 * <td>captcha(String value, String result)</td>
 * <td>{@code captcha('5','5')}</td>
 * </tr>
 * </table>
 * XXX TODO MM
 * 
 * @author Matthias Müller
 * 
 */
public class RuleValidation {

	static final String FILE_COUNT_MAX = "fileCountMax";
	static final String FILE_COUNT_MIN = "fileCountMin";
	static final String FILE_COUNT = "fileCount";
	static final String FILE_SIZE = "fileSize";
	static final String FILE_SIZE_MAX = "fileSizeMax";
	static final String FILE_SIZE_MIN = "fileSizeMin";
	static final String FILE_TYPE = "fileType";
	static final String SIZE_MIN_MAX = "sizeMinMax";
	static final String SIZE_MIN = "sizeMin";
	static final String SIZE_MAX = "sizeMax";
	static final String SIZE = "size";
	static final String CAPTCHA = "captcha";
	static final String NUMBER_FRACTION_DIGITS = "numberFractionDigits";
	static final String NUMBER = "number";
	static final String REG_EXP = "regExp";
	static final String EMAIL = "email";
	static final String STRING = "string";
	static final String EQUALS = "equals";
	private static final String EMPTY_STRING = "";
	private static final String KB = "KB";
	private static final String MB = "MB";
	private ExpressionEvaluator expressionEvaluator;
	private static final String COMMA = ",";
	private static final String EXP_NUMBER = "\\d+";
	private static final String EXP_STRING = "\\w+";
	private static final String EMAIL_PATTERN = "([a-zA-Z0-9_\\.-])+@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]){2,}";
	private static final char DOT = '.';

	public static final List<String> SHORT_RULES = Arrays.asList(STRING, EMAIL, NUMBER);

	public RuleValidation(Request container) {
		Map<String, Object> sessionParams = new HashMap<String, Object>();
		HttpSession session = container.getHttpServletRequest().getSession();
		Enumeration<String> attributeNames = session.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			String attribute = attributeNames.nextElement();
			Object value = session.getAttribute(attribute);
			sessionParams.put(attribute, value);
		}

		Map<String, Object> parameters = new HashMap<String, Object>(container.getParameters());
		Map<String, List<String>> multivaluedParameters = container.getParametersList();
		for (String key : parameters.keySet()) {
			if (multivaluedParameters.get(key).size() > 1) {
				parameters.put(key, multivaluedParameters.get(key));
			}
		}
		parameters.put("SESSION", sessionParams);
		expressionEvaluator = new ExpressionEvaluator(parameters);
		Map<String, List<FormUpload>> fileParams = getFileParams(container.getFormUploads());
		expressionEvaluator.setVariables(fileParams);
		registerFunctions();
	}

	private void registerFunctions() {
		addFunction(STRING, String.class);
		addFunction(EMAIL, String.class);
		addFunction(EQUALS, String.class, String.class);
		addFunction(REG_EXP, String.class, String.class);
		addFunction(NUMBER, String.class);
		addFunction(NUMBER_FRACTION_DIGITS, String.class, int.class, int.class);
		addFunction(SIZE, Object.class, int.class);
		addFunction(SIZE_MAX, Object.class, int.class);
		addFunction(SIZE_MIN, Object.class, int.class);
		addFunction(SIZE_MIN_MAX, Object.class, int.class, int.class);
		addFunction(FILE_TYPE, List.class, String.class);
		addFunction(FILE_SIZE_MIN, List.class, String.class);
		addFunction(FILE_SIZE_MAX, List.class, String.class);
		addFunction(FILE_SIZE, List.class, String.class, String.class);
		addFunction(FILE_COUNT, List.class, int.class, int.class);
		addFunction(FILE_COUNT_MIN, List.class, int.class);
		addFunction(FILE_COUNT_MAX, List.class, int.class);
		addFunction(CAPTCHA, String.class, String.class);
	}

	public Map<String, List<FormUpload>> getFileParams(Map<String, List<FormUpload>> formUploads) {
		Map<String, List<FormUpload>> fileParams = new HashMap<String, List<FormUpload>>();
		Set<String> keySet = formUploads.keySet();
		for (String key : keySet) {
			List<FormUpload> list = formUploads.get(key);
			if (list != null & !list.isEmpty()) {
				fileParams.put(key, list);
			}
		}
		return fileParams;
	}

	private void addFunction(String name, Class<?>... parameterTypes) {
		expressionEvaluator.addFunction(name, getMethod(name, parameterTypes));
	}

	private Method getMethod(String name, Class<?>... parameterTypes) {
		try {
			return RuleValidation.class.getMethod(name, parameterTypes);
		} catch (Exception e) {
			// can never ever happen
		}
		return null;
	}

	public boolean validate(String rule) {
		return expressionEvaluator.evaluate(rule);
	}

	public static boolean string(String string) {
		return regExp(string, EXP_STRING);
	}

	public static boolean captcha(String value, String result) {
		return StringUtils.equals(value, result);
	}

	public static boolean email(String string) {
		return regExp(string, EMAIL_PATTERN);
	}

	public static boolean equals(String string1, String string2) {
		return StringUtils.equals(string1, string2);
	}

	public static boolean number(String string) {
		return regExp(string, EXP_NUMBER);
	}

	public static boolean number(String string, char separator) {
		return regExp(string, EXP_NUMBER + separator + EXP_NUMBER);
	}

	public static boolean numberFractionDigits(String string, int digits, int fraction) {
		return number(string, DOT, digits, fraction);
	}

	public static boolean number(String string, char separator, int digits, int fraction) {
		String sep = null;
		if (DOT == separator) {
			sep = "\\.";
		} else {
			sep = new String(new char[] { separator });
		}
		return number(string, sep, digits, fraction);
	}

	private static boolean number(String string, String separator, int digits, int fraction) {
		return regExp(string, "\\d{1," + digits + "}(" + separator + "\\d{0," + fraction + "})?");
	}

	public static boolean regExp(String string, String regex) {
		return string.matches(regex);
	}

	private static int size(Object item) {
		if (null == item) {
			return 0;
		}
		if (Collection.class.isAssignableFrom(item.getClass())) {
			return ((Collection<?>) item).size();
		}
		if (CharSequence.class.isAssignableFrom(item.getClass())) {
			return ((CharSequence) item).length();
		}
		throw new UnsupportedOperationException("can not invoke size() on object of type" + item.getClass().getName()
				+ "!");
	}

	public static boolean size(Object item, int size) {
		return size(item) == size;
	}

	public static boolean sizeMax(Object item, int max) {
		return size(item) <= max;
	}

	public static boolean sizeMin(Object item, int min) {
		return size(item) >= min;
	}

	public static boolean sizeMinMax(Object item, int min, int max) {
		return sizeMax(item, max) && sizeMin(item, min);
	}

	public static boolean fileType(List<FormUpload> fUpload, String types) {
		List<String> fileTypes = Arrays.asList(types.split(COMMA));
		if (fUpload != null) {
			for (FormUpload fileName : fUpload) {
				if (!fileTypes.contains(fileName.getContentType())) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean fileSize(List<FormUpload> fUpload, String minSize, String maxSize) {
		return isValidFileUpload(fUpload, minSize, false) && isValidFileUpload(fUpload, maxSize, true);
	}

	public static boolean fileSizeMin(List<FormUpload> fUpload, String minSize) {
		return isValidFileUpload(fUpload, minSize, false);
	}

	public static boolean fileSizeMax(List<FormUpload> fUpload, String maxSize) {
		return isValidFileUpload(fUpload, maxSize, true);
	}

	public static boolean fileCountMin(List<FormUpload> fUpload, int count) {
		return getNumberOfFiles(fUpload) >= count;
	}

	public static boolean fileCountMax(List<FormUpload> fUpload, int count) {
		return getNumberOfFiles(fUpload) <= count;
	}

	public static boolean fileCount(List<FormUpload> fUpload, int minCount, int maxCount) {
		return fileCountMin(fUpload, minCount) && fileCountMax(fUpload, maxCount);
	}

	private static int getNumberOfFiles(List<FormUpload> fUpload) {
		if (fUpload != null && !fUpload.isEmpty()) {
			return fUpload.size();
		}
		return 0;
	}

	private static boolean isValidFileUpload(List<FormUpload> fUpload, String size, boolean isMax) {
		boolean isValidSize = false;
		if (fUpload != null && !fUpload.isEmpty()) {
			for (FormUpload fileName : fUpload) {
				isValidSize = isValidMaxSize(fileName, getAllowedFileSize(size), isMax);
				if (!isValidSize) {
					return false;
				}
			}
			return isValidSize;
		}
		return true;
	}

	private static double getAllowedFileSize(String maxSize) {
		if (maxSize.contains(MB)) {
			return Double.parseDouble(maxSize.replace(MB, EMPTY_STRING)) * FileUtils.ONE_MB;
		} else if (maxSize.contains(KB)) {
			return Double.parseDouble(maxSize.replace(KB, EMPTY_STRING)) * FileUtils.ONE_KB;
		}
		return 0;
	}

	private static boolean isValidMaxSize(FormUpload fileName, double size, boolean isMax) {
		long length = new File(fileName.getFile().getAbsolutePath()).length();
		return isMax ? length <= size : length >= size;
	}
}
