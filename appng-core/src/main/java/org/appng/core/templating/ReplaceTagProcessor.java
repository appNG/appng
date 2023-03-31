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
package org.appng.core.templating;

import java.util.Collection;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.AbstractStandardFragmentInsertionTagProcessor;
import org.thymeleaf.standard.processor.StandardReplaceTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

import lombok.extern.slf4j.Slf4j;

/**
 * A custom implementation of the {@link StandardReplaceTagProcessor}. Before calling the original doProcess method, it
 * first calls all known interceptors. The interceptors can customize the process attributes and context to call another
 * fragment instead. In this case the interceptor has to return 'true' to indicate that the processing is already done
 * 
 * @author Claus Stümke
 */
@Slf4j
public class ReplaceTagProcessor extends AbstractStandardFragmentInsertionTagProcessor
		implements ThymeleafStandardReplaceTagProcessorCaller {

	public static final int PRECEDENCE = 100;
	public static final String ATTR_NAME = "replace";

	Collection<ThymeleafReplaceInterceptor> interceptors;

	protected ReplaceTagProcessor(TemplateMode templateMode, String dialectPrefix,
			Collection<ThymeleafReplaceInterceptor> interceptors) {
		super(templateMode, dialectPrefix, ATTR_NAME, PRECEDENCE, true);
		this.interceptors = interceptors;
	}

	protected void doProcess(final ITemplateContext context, final IProcessableElementTag tag,
			final AttributeName attributeName, final String attributeValue,
			final IElementTagStructureHandler structureHandler) {
		LOGGER.debug("called replace tag processor with tag {} attributeName {} and attributeValue {}", tag,
				attributeName, attributeValue);
		if (null != interceptors) {
			// an interceptor can decide if the original fragment shall be called, with
			// the original context and attributes. An interceptor can change the attributes
			// and context to customize this call
			for (ThymeleafReplaceInterceptor interceptor : interceptors) {
				if (interceptor.intercept(context, tag, attributeName, attributeValue, structureHandler, this)) {
					// do return if an interceptor already did the processing
					return;
				}
			}
		}
		// no interceptor invoked, so simply call the original logic of the
		// AbstractStandardFragmentInsertionTagProcessor
		super.doProcess(context, tag, attributeName, attributeValue, structureHandler);
	}

	/**
	 * this method is for the interceptors to be able to do their own kind of processing call with customized attributes
	 * or context
	 */
	public void callStandardDoProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
			String attributeValue, IElementTagStructureHandler structureHandler) {
		super.doProcess(context, tag, attributeName, attributeValue, structureHandler);

	}
}
