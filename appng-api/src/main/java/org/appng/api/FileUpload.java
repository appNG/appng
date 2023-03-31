/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.text.NumberFormat;
import java.util.Collection;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.appng.api.support.validation.FileUploadListValidator;
import org.appng.api.support.validation.FileUploadValidator;
import org.appng.forms.FormUpload;
import org.appng.forms.FormUploadValidator;

/**
 * Used to validate the size, filetype(s) and amount of a ({@link Collection}) of) {@link FormUpload}(s).
 * 
 * @author Matthias Müller
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { FileUploadValidator.class, FileUploadListValidator.class })
public @interface FileUpload {

	static final int FACTOR = 1024;

	/**
	 * the unit to use for validating uploads by their size
	 */
	public enum Unit {
		/** byte */
		B(1),

		/** kilobyte */
		KB(FACTOR),

		/** megabyte */
		MB(FACTOR * FACTOR);

		private final long factor;

		Unit(long factor) {
			this.factor = factor;
		}

		/**
		 * the factor to multiply with to get the number of bytes per whole unit
		 * 
		 * @return the factor
		 */
		public long getFactor() {
			return factor;
		}

		/**
		 * Formats the given size using a {@link NumberFormat}
		 * 
		 * @param size
		 *               the size in bytes
		 * @param format
		 *               the {@link NumberFormat}
		 * 
		 * @return the formatted size
		 * 
		 * @see FileUpload.Unit#format(FileUpload.Unit, long, NumberFormat)
		 */
		public String format(long size, NumberFormat format) {
			return format(this, size, format);
		}

		/**
		 * Formats the given size of the given {@link Unit} using a {@link NumberFormat}, appending the given
		 * {@link Unit}.<br/>
		 * Example:
		 * 
		 * <pre>
		 * NumberFormat numberFormat = new DecimalFormat(&quot;0.0# &quot;, new DecimalFormatSymbols(Locale.ENGLISH));
		 * String format = Unit.format(Unit.MB, (long) (2.46d * 1024l * 1024l), numberFormat);
		 * org.junit.Assert.assertEquals("2.46 MB", format);
		 * 
		 * <pre>
		 * 
		 * @param unit
		 *               the {@link Unit}
		 * @param size
		 *               the size in bytes
		 * @param format
		 *               the {@link NumberFormat}
		 * 
		 * @return the formatted size
		 */
		public static String format(Unit unit, long size, NumberFormat format) {
			return format.format((double) size / unit.factor) + unit.name();
		}
	}

	String message() default ValidationMessages.VALIDATION_FILE_INVALID;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	/**
	 * @return an array of {@link FormUploadValidator}s for additional validation of a {@link FormUpload}.
	 */
	Class<? extends FormUploadValidator>[] uploadValidators() default {};

	/**
	 * @return the minimum size for an uploaded file (default: 0 {@link #unit()})
	 * 
	 * @see #unit()
	 */
	long minSize() default 0;

	/**
	 * @return the maximum size for an uploaded file (default: 10 {@link #unit()})
	 * 
	 * @see #unit()
	 */
	long maxSize() default 10;

	/**
	 * @return the {@link Unit} for {@link #minSize()} and {@link #maxSize()} (default: {@link Unit#MB})
	 */
	Unit unit() default Unit.MB;

	/**
	 * @return the minimum number of uploaded files (default: 1)
	 */
	int minCount() default 1;

	/**
	 * @return the maximum number of uploaded files (default: 1)
	 */
	int maxCount() default 1;

	/**
	 * @return a comma-separated list of allowed file-extensions (default "", which means everything is allowed)
	 */
	String fileTypes() default "";

}
