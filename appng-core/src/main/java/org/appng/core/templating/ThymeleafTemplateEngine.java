package org.appng.core.templating;

import java.util.Collection;

import org.thymeleaf.spring4.SpringTemplateEngine;

public class ThymeleafTemplateEngine extends SpringTemplateEngine {

	private static final AppNGThymeleafDialect APPNG_DIALECT = new AppNGThymeleafDialect();

	public ThymeleafTemplateEngine() {
		super();
		super.setDialect(APPNG_DIALECT);
	}

	public ThymeleafTemplateEngine(Collection<ThymeleafReplaceInterceptor> interceptors) {
		super();
		AppNGThymeleafDialect appNGDialect =  new AppNGThymeleafDialect(interceptors);
		super.setDialect(appNGDialect);
	}

}
