package org.appng.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

@Configuration
public class RestConfig {

	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping(ApplicationContext context) {
		RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
		requestMappingHandlerMapping.setApplicationContext(context);
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		urlPathHelper.setRemoveSemicolonContent(false);
		requestMappingHandlerMapping.setUrlPathHelper(urlPathHelper);
		return requestMappingHandlerMapping;
	}

	@Bean
	@Lazy
	@RequestScope
	public RequestMappingHandlerAdapter RequestMappingHandlerAdapter(ApplicationContext context, Site site,
			Application application, Environment environment) {
		RequestMappingHandlerAdapter rmha = new RequestMappingHandlerAdapter();
		rmha.setApplicationContext(context);

		List<HttpMessageConverter<?>> messageConverters = getMessageConverters(context);
		if (!messageConverters.isEmpty()) {
			rmha.setMessageConverters(messageConverters);
		}
		rmha.setCustomArgumentResolvers(getArgumentResolvers(context));
		return rmha;
	}

	public static List<HttpMessageConverter<?>> getMessageConverters(ApplicationContext context) {
		return context.getBeansOfType(HttpMessageConverter.class).values().stream()
				.map(m -> (HttpMessageConverter<?>) m).collect(Collectors.toList());
	}

	@Bean
	public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
		return new ByteArrayHttpMessageConverter();
	}

	@Bean
	@Primary
	public MappingJackson2HttpMessageConverter defaultJsonConverter(
			@Value("${site.jsonPrettyPrint:false}") boolean prettyPrint) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDefaultPropertyInclusion(Include.NON_ABSENT);
		MappingJackson2HttpMessageConverter defaultJsonConverter = new MappingJackson2HttpMessageConverter(
				objectMapper);
		defaultJsonConverter.setPrettyPrint(prettyPrint);
		return defaultJsonConverter;
	}

	@Bean
	@Lazy
	public SiteAwareHandlerMethodArgumentResolver siteAwareHandlerMethodArgumentResolver(Site site,
			Application application, Environment environment) {
		return new SiteAwareHandlerMethodArgumentResolver(site, environment, application);
	}

	public static List<HandlerMethodArgumentResolver> getArgumentResolvers(ApplicationContext context) {
		return new ArrayList<>(context.getBeansOfType(HandlerMethodArgumentResolver.class).values());
	}

	@AllArgsConstructor
	public static class SiteAwareHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

		private final Site site;
		private final Environment environment;
		private final Application application;

		public boolean supportsParameter(MethodParameter parameter) {
			return isSite(parameter) || isEnvironment(parameter) || isApplication(parameter);
		}

		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
			return isSite(parameter) ? site
					: (isEnvironment(parameter) ? environment : (isApplication(parameter) ? application : null));
		}

		private boolean isEnvironment(MethodParameter parameter) {
			return isParameterType(parameter, Environment.class);
		}

		protected boolean isSite(MethodParameter parameter) {
			return isParameterType(parameter, Site.class);
		}

		private boolean isApplication(MethodParameter parameter) {
			return isParameterType(parameter, Application.class);
		}

		private boolean isParameterType(MethodParameter parameter, Class<?> type) {
			return parameter.getParameterType().equals(type);
		}

	}

}
