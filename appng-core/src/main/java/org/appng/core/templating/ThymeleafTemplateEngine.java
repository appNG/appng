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
package org.appng.core.templating;

import java.util.List;

import org.thymeleaf.spring4.SpringTemplateEngine;

/**
 * An custom template engine to set a custom Thymeleaf dialect
 * 
 * @author Claus Stümke
 */
public class ThymeleafTemplateEngine extends SpringTemplateEngine {

	public void withInterceptors(List<ThymeleafReplaceInterceptor> interceptors) {
		super.setDialect(new AppNGThymeleafDialect(interceptors));
	}

}
