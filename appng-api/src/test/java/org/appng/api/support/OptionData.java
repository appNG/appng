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
package org.appng.api.support;

import java.util.HashMap;
import java.util.Map;

import org.appng.api.Option;

/**
 * Testdata for {@link OptionImplTest} and {@link OptionsImplTest}.
 * 
 * @author Gajanan Nilwarn
 * 
 */
public class OptionData {

	public static Map<String, Option> getOptionsMap() {
		Map<String, Option> optionsMap = new HashMap<String, Option>();
		optionsMap.put("options-1", new OptionImpl("op-1").addAttribute("attribute-1", "value-1"));
		optionsMap.put("options-2", new OptionImpl("op-2").addAttribute("attribute-2", "value-2"));
		optionsMap.put("options-3", new OptionImpl("op-3").addAttribute("attribute-3", "value-3"));
		optionsMap.put("options-4", new OptionImpl("op-4").addAttribute("attribute-4", "value-4"));
		optionsMap.put("options-5", new OptionImpl("op-5").addAttribute("attribute-5", "value-5"));
		return optionsMap;
	}

	public static Map<String, String> getAttributesMap() {
		Map<String, String> attributesMap = new HashMap<String, String>();
		attributesMap.put("attribute-1", "value-1");
		attributesMap.put("attribute-2", "value-2");
		attributesMap.put("attribute-3", "value-3");
		attributesMap.put("attribute-4", "value-4");
		attributesMap.put("attribute-5", "value-5");
		return attributesMap;
	}
}
