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
package org.appng.core.templating;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.StandardReplaceTagProcessor;

/**
 * This interface has to be implemented by interceptors to intercept the call of
 * the replace tag to determine if this call has to be replaced by another
 * fragment. The interceptor can also add additional context variables. An
 * interceptor also has to be available as bean in the spring context. Therefore
 * the &#64;{@link Component} annotation might be useful. <br/>
 * <br/>
 * Example:
 * 
 * <pre>
 * &#64;Component
 * public class CustomInterceptor extends ThymeleafReplaceInterceptorBase {
 * 
 * 	public boolean intercept(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
 * 			String attributeValue, IElementTagStructureHandler structureHandler,
 * 			ThymeleafStandardReplaceTagProcessorCaller tagProcessor) {
 * 
 * 		if (attributeValue.contains("::datacell")) {
 * 			// we want to replace each call of datacell with our own fragment
 * 			attributeValue = attributeValue.replace("::datacell", "customtemplate.html::datacell_custom");
 * 			// proceed processing with our customized attribute value
 * 			tagProcessor.callStandardDoProcess(context, tag, attributeName, attributeValue, structureHandler);
 * 			// tell the replace tag processor that this intereptor already triggered the
 * 			// processing
 * 			return true;
 * 		}
 * 		// tell the replace tag processor that this interceptor did not intercept
 * 		return false;
 * 	}
 * 
 * 	// add all needed template file names for this interceptor
 * 	public Collection<String> getAdditionalTemplateResourceNames() {
 * 		Set<String> resources = Sets.newHashSet();
 * 		resources.add("personregister.html");
 * 		return resources;
 * 	}
 * 
 * }
 * </pre>
 * 
 * @author Claus Stümke
 *
 */
public interface ThymeleafReplaceInterceptor {

	/**
	 * This method is called from the {@link ReplaceTagProcessor} before executing
	 * the ordinary replace logic as implemented by the
	 * {@link StandardReplaceTagProcessor}. With this method the interceptor can
	 * decide to overwrite the attribute value of the replace tag to call another
	 * target fragment instead.
	 * 
	 * Probably the interceptor also has to add additional variables to the context
	 * if the other target fragment has parameters which does not exist in the given
	 * context
	 * 
	 * If the interceptor wants to intercept, it has to modify the attributeValue
	 * and call the doProcess method of the given tagProcessor. It has to return
	 * true to indicate that processing was done within this interception to avoid
	 * another processing by the {@link ReplaceTagProcessor}
	 * 
	 * 
	 * @param context
	 * @param tag
	 * @param attributeName
	 * @param attributeValue
	 * @param structureHandler
	 * @param tagProcessor
	 * @return
	 */
	boolean intercept(final ITemplateContext context, final IProcessableElementTag tag,
			final AttributeName attributeName, final String attributeValue,
			final IElementTagStructureHandler structureHandler,
			ThymeleafStandardReplaceTagProcessorCaller tagProcessor);

	/**
	 * With this method the interceptor can define additional template files which
	 * should be added to the template engine that custom fragments are available
	 * during processing
	 * 
	 * @return
	 */
	Collection<String> getAdditionalTemplateResourceNames();

	/**
	 * In case that an application has different interceptors intercepting on the
	 * same replace call, the first interceptor will win. The order of interceptors
	 * can be affected by setting a high priority to interceptors which shall be
	 * processed first
	 * 
	 * @return
	 */
	default int getPriority() {
		return 0;
	}

}
