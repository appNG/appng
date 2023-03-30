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
package org.appng.core.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.MessageParam;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Properties;
import org.passay.AllowedCharacterRule;
import org.passay.CharacterCharacteristicsRule;
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
import org.passay.WhitespaceRule;

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
 * numCharacterGroups = 4
 * allowOtherCharacters = false
 * allowWhiteSpace = false
 * generateLength = 8
 * generateLowerCase = 3
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
	private Integer generateLength;

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
		Integer numCharacterGroups = Integer.valueOf(properties.getProperty("numCharacterGroups", "4"));
		Boolean useHistory = Boolean.valueOf(properties.getProperty("useHistory", "true"));
		Boolean useUsername = Boolean.valueOf(properties.getProperty("useUsername", "true"));
		Boolean allowWhiteSpace = Boolean.valueOf(properties.getProperty("allowWhiteSpace", "false"));
		Boolean allowOtherCharacters = Boolean.valueOf(properties.getProperty("allowOtherCharacters", "false"));

		Integer generateLowerCase = Integer.valueOf(properties.getProperty("generateLowerCase", "3"));
		Integer generateUppercase = Integer.valueOf(properties.getProperty("generateUppercase", "3"));
		Integer generateDigits = Integer.valueOf(properties.getProperty("generateDigits", "1"));
		Integer generateSpecialChars = Integer.valueOf(properties.getProperty("generateSpecialChars", "1"));
		this.generateLength = Integer.valueOf(properties.getProperty("generateLength", "8"));

		List<Rule> rules = new ArrayList<>();

		List<CharacterRule> characteristicsRules = new ArrayList<>();
		String allowedCharacters = StringUtils.EMPTY;
		allowedCharacters += addRule(characteristicsRules, EnglishCharacterData.LowerCase, minLowerCase);
		allowedCharacters += addRule(characteristicsRules, EnglishCharacterData.UpperCase, minUppercase);
		allowedCharacters += addRule(characteristicsRules, EnglishCharacterData.Digit, minDigits);
		allowedCharacters += addRule(characteristicsRules, getSpecialChars(allowedSpecialChars), minSpecialChars);

		int effectiveNumRules = characteristicsRules.size();
		int minCharacteristics = numCharacterGroups > effectiveNumRules ? effectiveNumRules : numCharacterGroups;
		CharacterCharacteristicsRule characteristicsRule = new CharacterCharacteristicsRule(minCharacteristics,
				characteristicsRules);
		characteristicsRule.setReportFailure(numCharacterGroups != effectiveNumRules);
		rules.add(characteristicsRule);

		generationRules = new ArrayList<>();
		addRule(generationRules, EnglishCharacterData.LowerCase, generateLowerCase);
		addRule(generationRules, EnglishCharacterData.UpperCase, generateUppercase);
		addRule(generationRules, EnglishCharacterData.Digit, generateDigits);
		addRule(generationRules, getSpecialChars(allowedSpecialChars), generateSpecialChars);

		rules.add(new LengthRule(minLength, maxLength));
		if (useHistory) {
			rules.add(new HistoryRule());
		}
		if (useUsername) {
			rules.add(new UsernameRule(true, true, MatchBehavior.Contains));
		}
		if (allowWhiteSpace) {
			allowedCharacters += StringUtils.SPACE;
		} else {
			rules.add(new WhitespaceRule());
		}

		if (!allowOtherCharacters) {
			rules.add(new AllowedCharacterRule(allowedCharacters.toCharArray(), true));
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

	private String addRule(List<CharacterRule> rules, CharacterData characterData, Integer min) {
		if (min > 0) {
			rules.add(new CharacterRule(characterData, min));
			return characterData.getCharacters();
		}
		return StringUtils.EMPTY;
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
		return passwordGenerator.generatePassword(generateLength, generationRules);
	}

	public PasswordValidator getValidator() {
		return passwordValidator;
	}

}
