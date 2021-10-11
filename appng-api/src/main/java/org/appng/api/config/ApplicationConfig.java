package org.appng.api.config;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.support.RequestFactoryBean;
import org.appng.api.support.ResourceBundleMessageSource;
import org.appng.api.support.SelectionFactory;
import org.appng.api.support.environment.DefaultEnvironment;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.context.annotation.RequestScope;

import lombok.extern.slf4j.Slf4j;

/**
 * Base {@link Configuration} for all {@link Application}s.
 * 
 * @author Matthias Müller
 * 
 * @since 1.23
 */
@Slf4j
@Configuration
public class ApplicationConfig {

	@Bean
	public ConversionServiceFactoryBean conversionService() {
		return new ConversionServiceFactoryBean();
	}

	@Bean
	public ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource rbms = new ResourceBundleMessageSource();
		rbms.setAlwaysUseMessageFormat(true);
		rbms.setDefaultEncoding(StandardCharsets.UTF_8.name());
		rbms.setFallbackToSystemLocale(false);
		return rbms;
	}

	@Bean
	public SelectionFactory selectionFactory() {
		return new SelectionFactory();
	}

	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public Environment environment(HttpServletRequest request, HttpServletResponse response) {
		Environment environment = DefaultEnvironment.get(request, response);
		LOGGER.debug("created new environment#{}", environment.hashCode());
		return environment;
	}

	@Bean
	@RequestScope(proxyMode = ScopedProxyMode.NO)
	public RequestFactoryBean request(Environment env, HttpServletRequest request, ConversionService conversionService,
			MessageSource messageSource) {
		return new RequestFactoryBean(request, env, conversionService, messageSource);
	}

}
