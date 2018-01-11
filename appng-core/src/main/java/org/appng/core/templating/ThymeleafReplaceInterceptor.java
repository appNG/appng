package org.appng.core.templating;

import java.util.Collection;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.StandardReplaceTagProcessor;

public interface ThymeleafReplaceInterceptor {

	/**
	 * This method is called from the {@link ThymeleafReplaceTagProcessor}
	 * before executing the ordinary replace logic as implemented by the
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
	 * another processing by the {@link ThymeleafReplaceTagProcessor}
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

}
