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
package org.appng.api.validation.contraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * Validates that the plain text length of the given string is >= {@link RichtextSize#min()} and <=
 * {@link RichtextSize#max()}
 * 
 * @author Matthias Müller
 */
public class RichtextSizeValidator implements ConstraintValidator<RichtextSize, String> {

	private RichtextSize maxRichtextLength;

	@Override
	public void initialize(RichtextSize maxRichtextLength) {
		this.maxRichtextLength = maxRichtextLength;
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		String cleanedValue = Jsoup.clean(StringUtils.trimToEmpty(value), Whitelist.none());
		return cleanedValue.length() >= maxRichtextLength.min() && cleanedValue.length() <= maxRichtextLength.max();
	}

}
