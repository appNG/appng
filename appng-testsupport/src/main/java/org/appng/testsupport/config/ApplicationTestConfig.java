package org.appng.testsupport.config;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;

import org.appng.api.config.ApplicationConfig;
import org.appng.xml.MarshallService;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public CustomScopeConfigurer CustomScopeConfigurer() {
		CustomScopeConfigurer customScopeConfigurer = new CustomScopeConfigurer();
		customScopeConfigurer.addScope(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope());
		customScopeConfigurer.addScope(WebApplicationContext.SCOPE_SESSION, new SimpleThreadScope());
		return customScopeConfigurer;
	}

	@Bean
	public MockServletContext servletContext() {
		return new MockServletContext(new FileSystemResourceLoader());
	}

	@Bean
	@Scope("prototype")
	public MockHttpServletRequest httpServletRequest(ServletContext context) {
		return new MockHttpServletRequest(context);
	}
	
	@Bean
	@Scope("prototype")
	public MockHttpServletResponse htpServletResponse() {
		return new MockHttpServletResponse();
	}

	@Bean
	public MarshallService applicationMarshallService() throws JAXBException {
		return MarshallService.getApplicationMarshallService();
	}

	@Bean
	public MarshallService marshallService() throws JAXBException {
		return MarshallService.getMarshallService();
	}

}
