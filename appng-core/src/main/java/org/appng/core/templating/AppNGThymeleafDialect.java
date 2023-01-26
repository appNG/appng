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

import java.util.Collection;
import java.util.Set;

import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.spring4.dialect.SpringStandardDialect;
import org.thymeleaf.standard.processor.StandardReplaceTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * An appNG specific Thymeleaf dialect to enable the functionality to add application-specific interceptors. An
 * interceptor can redirect the 'replace' call of a fragment.
 * 
 * @author Claus Stümke
 */
public class AppNGThymeleafDialect extends SpringStandardDialect {

	private final Collection<ThymeleafReplaceInterceptor> interceptors;

	public AppNGThymeleafDialect(Collection<ThymeleafReplaceInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * Get all processors as composed by the {@link SpringStandardDialect}, remove the processor for the replace tag and
	 * add our own replace tag processor which enables the usage of interceptors.
	 */
	public Set<IProcessor> getProcessors(final String dialectPrefix) {
		Set<IProcessor> springStandardProcessorsSet = createSpringStandardProcessorsSet(dialectPrefix);
		IProcessor origReplaceProc = null;
		for (IProcessor proc : springStandardProcessorsSet) {
			if (proc instanceof StandardReplaceTagProcessor) {
				origReplaceProc = proc;
				break;
			}
		}
		if (null != origReplaceProc) {
			springStandardProcessorsSet.remove(origReplaceProc);
		}
		springStandardProcessorsSet.add(new ReplaceTagProcessor(TemplateMode.HTML, dialectPrefix, interceptors));

		return springStandardProcessorsSet;
	}

}
