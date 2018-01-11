package org.appng.core.templating;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.AbstractStandardFragmentInsertionTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

public class ThymeleafReplaceTagProcessor extends AbstractStandardFragmentInsertionTagProcessor implements ThymeleafStandardReplaceTagProcessorCaller  {

	public static final int PRECEDENCE = 100;
	public static final String ATTR_NAME = "replace";
	private static Logger log = LoggerFactory.getLogger(ThymeleafReplaceTagProcessor.class);

	Collection<ThymeleafReplaceInterceptor> interceptors;

	protected ThymeleafReplaceTagProcessor(TemplateMode templateMode, String dialectPrefix,
			Collection<ThymeleafReplaceInterceptor> interceptors) {
		super(templateMode, dialectPrefix, ATTR_NAME, PRECEDENCE, true);
		this.interceptors = interceptors;
	}

	protected void doProcess(final ITemplateContext context, final IProcessableElementTag tag,
			final AttributeName attributeName, final String attributeValue,
			final IElementTagStructureHandler structureHandler) {
		// here we can decide if we want to call the original fragment or if there is a
		// replacement for that fragment from the application
		log.debug("called replace tag processor with tag {} attributeName {} and attributeValue {}", tag, attributeName,
				attributeValue);
		if (null != interceptors) {
			for (ThymeleafReplaceInterceptor interceptor : interceptors) {
				if (interceptor.intercept(context, tag, attributeName, attributeValue, structureHandler,
						this)) {
					return;
				}
			}
		}
		// no interceptor invoked, so simply call the original logic of the
		// AbstractStandardFragmentInsertionTagProcessor
		super.doProcess(context, tag, attributeName, attributeValue, structureHandler);
	}

	public void callStandardDoProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
			String attributeValue, IElementTagStructureHandler structureHandler) {
		super.doProcess(context, tag, attributeName, attributeValue, structureHandler);
		
	}
}
