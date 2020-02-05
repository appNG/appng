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
package org.appng.core.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.MessageParam;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Properties;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.HistoryRule;
import org.passay.LengthRule;
import org.passay.MatchBehavior;
import org.passay.PasswordData;
import org.passay.PasswordData.HistoricalReference;
import org.passay.PasswordData.Reference;
import org.passay.PasswordGenerator;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.UsernameRule;

/**
 * A configurable {@link PasswordPolicy} using a {@link org.passay.PasswordValidator}.<br/>
 * A multiline platform property named {@code configurablePasswordPolicy} can be used for configuration, defaults as
 * shown:
 * 
 * <pre>
 * minLowerCase = 1
 * minUppercase = 1
 * minDigits = 1
 * minSpecialChars = 1
 * allowedSpecialChars = !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
 * minLength = 8
 * maxLength = 255
 * useHistory = true
 * useUsername = true
 * generateMinLength = 8
 * generatLowerCase = 3
 * generateUppercase = 3
 * generateDigits = 1
 * generateSpecialChars = 1
 * </pre>
 * 
 * @author Matthias Müller
 */
public class ConfigurablePasswordPolicy implements PasswordPolicy {

	private PasswordValidator passwordValidator;
	private PasswordGenerator passwordGenerator;
	private List<CharacterRule> generationRules;
	private Integer generateMinLength;

	@Override
	public void configure(Properties platformProperties) {
		java.util.Properties properties = null;
		if (null != platformProperties) {
			properties = platformProperties.getProperties("configurablePasswordPolicy");
		}
		if (null == properties) {
			properties = new java.util.Properties();
		}
		Integer minLowerCase = Integer.valueOf(properties.getProperty("minLowerCase", "1"));
		Integer minUppercase = Integer.valueOf(properties.getProperty("minUppercase", "1"));
		Integer minDigits = Integer.valueOf(properties.getProperty("minDigits", "1"));
		Integer minSpecialChars = Integer.valueOf(properties.getProperty("minSpecialChars", "1"));

		String allowedSpecialChars = properties.getProperty("allowedSpecialChars", PUNCT);
		Integer minLength = Integer.valueOf(properties.getProperty("minLength", "8"));
		Integer maxLength = Integer.valueOf(properties.getProperty("maxLength", "255"));
		Boolean useHistory = Boolean.valueOf(properties.getProperty("useHistory", "true"));
		Boolean useUsername = Boolean.valueOf(properties.getProperty("useUsername", "true"));

		Integer generatLowerCase = Integer.valueOf(properties.getProperty("generatLowerCase", "3"));
		Integer generateUppercase = Integer.valueOf(properties.getProperty("generateUppercase", "3"));
		Integer generateDigits = Integer.valueOf(properties.getProperty("generateDigits", "1"));
		Integer generateSpecialChars = Integer.valueOf(properties.getProperty("generateSpecialChars", "1"));
		this.generateMinLength = Integer.valueOf(properties.getProperty("generateMinLength", "8"));

		List<Rule> rules = new ArrayList<>();

		addRule(rules, EnglishCharacterData.LowerCase, minLowerCase);
		addRule(rules, EnglishCharacterData.UpperCase, minUppercase);
		addRule(rules, EnglishCharacterData.Digit, minDigits);
		addRule(rules, getSpecialChars(allowedSpecialChars), minSpecialChars);

		generationRules = new ArrayList<>();
		addCharacterRulee(generationRules, EnglishCharacterData.LowerCase, generatLowerCase);
		addCharacterRulee(generationRules, EnglishCharacterData.UpperCase, generateUppercase);
		addCharacterRulee(generationRules, EnglishCharacterData.Digit, generateDigits);
		addCharacterRulee(generationRules, getSpecialChars(allowedSpecialChars), generateSpecialChars);

		rules.add(new LengthRule(minLength, maxLength));
		if (useHistory) {
			rules.add(new HistoryRule());
		}
		if (useUsername) {
			rules.add(new UsernameRule(true, true, MatchBehavior.Contains));
		}

		this.passwordValidator = new PasswordValidator(rules);
		this.passwordGenerator = new PasswordGenerator();

	}

	private CharacterData getSpecialChars(String allowedSpecialChars) {
		if (StringUtils.isBlank(allowedSpecialChars)) {
			return EnglishCharacterData.Special;
		}
		return new CharacterData() {

			public String getErrorCode() {
				return EnglishCharacterData.Special.getErrorCode();
			}

			@Override
			public String getCharacters() {
				return allowedSpecialChars;
			}
		};
	}

	private void addRule(List<Rule> rules, CharacterData characterData, Integer min) {
		if (min > 0) {
			rules.add(new CharacterRule(characterData, min));
		}
	}

	private void addCharacterRulee(List<CharacterRule> rules, CharacterData characterData, Integer min) {
		rules.add(new CharacterRule(characterData, min));
	}

	public boolean isValidPassword(char[] password) {
		return passwordValidator.validate(new PasswordData(new String(password))).isValid();
	}

	@Override
	public ValidationResult validatePassword(String username, char[] currentPassword, char[] password) {
		PasswordData passwordData = null;
		if (null == username) {
			passwordData = new PasswordData(new String(password));
		} else {
			List<Reference> references = new ArrayList<>();
			if (null != currentPassword) {
				references.add(new HistoricalReference(new String(currentPassword)));
			}
			passwordData = new PasswordData(username, new String(password), references);
		}
		RuleResult validate = passwordValidator.validate(passwordData);
		List<MessageParam> messages = validate.getDetails().stream().map(d -> new MessageParam() {

			public String getMessageKey() {
				return ConfigurablePasswordPolicy.class.getSimpleName() + "." + d.getErrorCode();
			}

			public Object[] getMessageArgs() {
				return d.getValues();
			}
		}).collect(Collectors.toList());

		return new ValidationResult(validate.isValid(), messages.toArray(new MessageParam[0]));
	}

	public String getErrorMessageKey() {
		return null;
	}

	public String generatePassword() {
		return passwordGenerator.generatePassword(generateMinLength, generationRules);
	}

	public PasswordValidator getValidator() {
		return passwordValidator;
	}

}
