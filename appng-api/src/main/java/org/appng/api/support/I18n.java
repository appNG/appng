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
package org.appng.api.support;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import org.appng.api.Request;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Label;

/**
 * Utility class used for formatting {@link String}s, {@link Date}s and {@link Number}s. Additionally, a message can be
 * retrieved by delegating to {@link Request#getMessage(String, Object...)}.<br/>
 * Since an instance of {@link I18n} is being added to each {@link ExpressionEvaluator} returned by
 * {@link Request#getExpressionEvaluator()}, you can easily use the methods provided by {@link I18n} in your
 * expressions.<br/>
 * <h3>Examples</h3><br/>
 * Consider a {@link ResourceBundle} containing the following entries:
 * 
 * <pre>
 * dateFormat = yyyy-MM-dd
 * today = Today: {0}
 * </pre>
 * 
 * Reading the format for a {@link FieldDef} using {@link #message(String, Object...)}:
 * 
 * <pre>
 * &lt;field name="validFrom" type="date" format="${i18n.message('dateFormat')}">
 * </pre>
 * 
 * leads to
 * 
 * <pre>
 * &lt;field name="validFrom" type="date" format="yyyy-MM-dd">
 * </pre>
 * 
 * <br/>
 * Setting the value for a {@link Label} using {@link #message(String, Object...)} and {@link #formatDate(Date, String)}
 * 
 * <pre>
 * &lt;label>${i18n.message('today', i18n.formatDate(current.date, i18n.message('dateFormat')))}&lt;/label>
 * </pre>
 * 
 * leads to (assuming current.today returns a {@link Date}):
 * 
 * <pre>
 * &lt;label>Today: 2013-03-08&lt;/label>
 * </pre>
 * 
 * @author Matthias Müller
 */
public class I18n {

	private Request request;
	private NumberFormat numberFormat;

	/**
	 * Creates a new {@link I18n} which uses the messages and the {@link Locale} provided by the given {@link Request}.
	 * 
	 * @param request
	 *                a {@link Request}
	 */
	public I18n(Request request) {
		this.request = request;
		this.numberFormat = NumberFormat.getNumberInstance(request.getLocale());
	}

	/**
	 * Returns an internationalized message by delegating to {@link Request#getMessage(String, Object...)}. Applies the
	 * {@link Locale} provided by {@link Request#getLocale()}.
	 * 
	 * @param key
	 *            the message key
	 * 
	 * @return the message
	 * 
	 * @see Request#getMessage(String, Object...)
	 */
	public String message(String key) {
		return request.getMessage(key);
	}

	/**
	 * Returns an internationalized message by delegating to {@link Request#getMessage(String, Object...)}. Applies the
	 * {@link Locale} provided by {@link Request#getLocale()}.
	 * 
	 * @param key
	 *             the message key
	 * @param args
	 *             the message arguments
	 * 
	 * @return the message
	 * 
	 * @see Request#getMessage(String, Object...)
	 */
	public String message(String key, Object... args) {
		return request.getMessage(key, args);
	}

	/**
	 * Formats the given {@link String} with the given arguments. Delegates to
	 * {@link String#format(java.util.Locale, String, Object...)} using the {@link Locale} provided by
	 * {@link Request#getLocale()}.
	 * 
	 * @param format
	 *               the {@link String} to format
	 * @param args
	 *               the arguments passed to
	 * 
	 * @return a formatted String
	 * 
	 * @see String#format(Locale, String, Object...)
	 */
	public String format(String format, Object... args) {
		return String.format(request.getLocale(), format, args);
	}

	/**
	 * Formats the given {@link Date} using the given {@code format}. Internally, a {@link SimpleDateFormat} is being
	 * used. Applies the {@link Locale} provided by {@link Request#getLocale()}.
	 * 
	 * @param date
	 *               the {@link Date} to format
	 * @param format
	 *               the the format
	 * 
	 * @return the formatted {@link Number}
	 * 
	 * @see SimpleDateFormat#SimpleDateFormat(String, Locale)
	 * @see SimpleDateFormat#format(Date)
	 */
	public String formatDate(Date date, String format) {
		return new SimpleDateFormat(format, request.getLocale()).format(date);
	}

	/**
	 * Formats the given {@link Number} using the given {@code format}. Internally, a {@link NumberFormat} is being
	 * used, which applies the {@link Locale} provided by {@link Request#getLocale()}.
	 * 
	 * @param number
	 *               the {@link Number} to format
	 * @param format
	 *               the the format
	 * 
	 * @return the formatted {@link Number}
	 * 
	 * @see NumberFormat#getNumberInstance(Locale)
	 */
	public String formatNumber(Number number, String format) {
		return numberFormat.format(number);
	}

}
