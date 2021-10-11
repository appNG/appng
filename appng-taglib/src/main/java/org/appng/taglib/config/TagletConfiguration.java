package org.appng.taglib.config;

import org.appng.taglib.TagletProcessor;
import org.appng.xml.MarshallService;
import org.appng.xml.transformation.StyleSheetProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class TagletConfiguration {

	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public TagletProcessor tagletProcessor(MarshallService marshallService, StyleSheetProvider styleSheetProvider)
			throws ReflectiveOperationException {
		TagletProcessor tagletProcessor = new TagletProcessor();
		tagletProcessor.setMarshallService(marshallService);
		tagletProcessor.setStyleSheetProvider(styleSheetProvider);
		return tagletProcessor;
	}

}
