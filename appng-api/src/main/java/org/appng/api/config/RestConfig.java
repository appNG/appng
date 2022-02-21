/*
 * Copyright 2011-2022 the original author or authors.
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
package org.appng.api.config;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Configuration}that adds a {@link MappingJackson2HttpMessageConverter} and an {@link ObjectMapper} to the
 * context, if not already present. <br/>
 * Also checks the context for Jackson {@link Module}s and adds them to the {@link ObjectMapper}.<br/>
 * Additionally, modules for handling these {@link Temporal}-types are registered:
 * <ul>
 * <li>{@link OffsetDateTime}, using {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
 * <li>{@link LocalDate}, using {@link DateTimeFormatter#ISO_LOCAL_DATE}
 * <li>{@link LocalTime}, using {@link DateTimeFormatter#ISO_LOCAL_TIME}
 * <li>{@link LocalDateTime}, using {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
 * </ul>
 * <br/>
 * Also adds a {@link HandlerMethodArgumentResolver} that can resolve the current {@link Environment}, {@link Site} and
 * {@link Application}.
 * 
 * @author Matthias Müller
 */
@Slf4j
@Configuration
public class RestConfig implements BeanFactoryPostProcessor {

	private static final String DEFAULT_JACKSON_CONVERTER = "defaultJacksonConverter";
	private static final String DEFAULT_OBJECT_MAPPER = "defaultObjectMapper";

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Map<String, MappingJackson2HttpMessageConverter> jacksonConverters = beanFactory
				.getBeansOfType(MappingJackson2HttpMessageConverter.class);
		LOGGER.info("Found {} MappingJackson2HttpMessageConverters: {}", jacksonConverters.size(),
				StringUtils.join(jacksonConverters.keySet(), ", "));

		Map<String, ObjectMapper> objectMappers = beanFactory.getBeansOfType(ObjectMapper.class);
		LOGGER.info("Found {} ObjectMappers: {}", objectMappers.size(), StringUtils.join(objectMappers.keySet(), ", "));

		Map<String, Module> modules = beanFactory.getBeansOfType(Module.class);
		LOGGER.info("Found {} Modules: {}", modules.size(), StringUtils.join(modules.keySet(), ", "));

		Map<String, Object> primaryBeans = beanFactory.getBeansWithAnnotation(Primary.class);
		LOGGER.info("Found {} @Primary Beans: {}", primaryBeans.size(), StringUtils.join(primaryBeans.keySet(), ", "));

		boolean registerObjectMapper = false;
		ObjectMapper objectMapper;
		if (registerObjectMapper = objectMappers.isEmpty()) {
			objectMapper = new ObjectMapper().setDefaultPropertyInclusion(Include.NON_ABSENT)
					.enable(SerializationFeature.INDENT_OUTPUT).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			LOGGER.info("No ObjectMapper found in context, creating default.");
		} else {
			objectMapper = getPrimaryOrFirst(objectMappers, primaryBeans);
		}

		boolean registerConverter = false;
		MappingJackson2HttpMessageConverter converter;
		if (registerConverter = jacksonConverters.isEmpty()) {
			converter = new MappingJackson2HttpMessageConverter(objectMapper);
			LOGGER.info("No MappingJackson2HttpMessageConverter found in context, creating default.");
		} else {
			converter = getPrimaryOrFirst(jacksonConverters, primaryBeans);
			objectMapper = converter.getObjectMapper();
		}

		addDateModules(objectMapper);
		for (Entry<String, Module> moduleEntry : modules.entrySet()) {
			objectMapper.registerModule(moduleEntry.getValue());
			LOGGER.info("Adding Module '{}' to ObjectMapper", moduleEntry.getKey());
		}

		if (registerObjectMapper) {
			beanFactory.registerSingleton(DEFAULT_OBJECT_MAPPER, objectMapper);
			LOGGER.info("Registering ObjectMapper '{}'", DEFAULT_OBJECT_MAPPER);
		}

		if (registerConverter) {
			beanFactory.registerSingleton(DEFAULT_JACKSON_CONVERTER, converter);
			LOGGER.info("Registering MappingJackson2HttpMessageConverter '{}'", DEFAULT_JACKSON_CONVERTER);
		}
	}

	protected <T> T getPrimaryOrFirst(Map<String, T> beans, Map<String, Object> primaryBeans) {
		T bean;
		Optional<Entry<String, T>> entry = beans.entrySet().stream().filter(e -> primaryBeans.containsKey(e.getKey()))
				.findFirst();
		boolean isPrimary = false;
		if (isPrimary = entry.isPresent()) {
			bean = entry.get().getValue();
		} else {
			entry = Optional.of(beans.entrySet().iterator().next());
			bean = entry.get().getValue();
		}
		LOGGER.info("Found {} '{}'", (isPrimary ? "@Primary " : "") + entry.get().getValue().getClass().getName(),
				entry.get().getKey());
		return bean;
	}

	// @formatter:off
	protected void addDateModules(ObjectMapper objectMapper) {
		objectMapper.registerModule(getDateModule(
			OffsetDateTime.class,
			OffsetDateTime::parse,
			DateTimeFormatter.ISO_OFFSET_DATE_TIME
		));

		objectMapper.registerModule(getDateModule(
			LocalDate.class,
			LocalDate::parse,
			DateTimeFormatter.ISO_LOCAL_DATE
		));

		objectMapper.registerModule(getDateModule(
			LocalTime.class,
			LocalTime::parse,
			DateTimeFormatter.ISO_LOCAL_TIME
		));

		objectMapper.registerModule(getDateModule(
			LocalDateTime.class,
			LocalDateTime::parse,
			DateTimeFormatter.ISO_LOCAL_DATE_TIME
		));
	}	
	// @formatter:on

	protected <T extends Temporal> SimpleModule getDateModule(Class<T> temporal, Function<String, T> parseFunction,
			DateTimeFormatter formatter) {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(temporal, new JsonDeserializer<T>() {
			@Override
			public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
				if (StringUtils.isNotBlank(parser.getText())) {
					return parseFunction.apply(parser.getText());
				}
				return null;
			}
		});
		module.addSerializer(temporal, new JsonSerializer<T>() {
			@Override
			public void serialize(T value, JsonGenerator jsonGenerator, SerializerProvider provider)
					throws IOException {
				if (value != null) {
					jsonGenerator.writeString(formatter.format(value));
				}
			}
		});
		LOGGER.debug("Added Module handling {}.", temporal.getName());
		return module;
	}

	@Bean
	public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
		return new ByteArrayHttpMessageConverter();
	}

	@Bean
	@Lazy
	public SiteAwareHandlerMethodArgumentResolver siteAwareHandlerMethodArgumentResolver(Site site,
			Application application, Environment environment) {
		return new SiteAwareHandlerMethodArgumentResolver(site, environment, application);
	}

	/**
	 * A {@link HandlerMethodArgumentResolver} that can resolve the current {@link Application}, {@link Environment} and
	 * {@link Site}.
	 */
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
