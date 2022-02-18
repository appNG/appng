/*
 * Copyright 2011-2021 the original author or authors.
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
package org.appng.core.controller.rest;

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

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.controller.rest.RestOperation.RestErrorHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.WebApplicationContext;

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

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that adds a {@link MappingJackson2HttpMessageConverter} and an
 * {@link ObjectMapper} to the context, if not already present. <br/>
 * Also checks the context for Jackson {@link Module}s and adds them to the {@link ObjectMapper}.<br/>
 * Additionally, modules for handling these {@link Temporal}-types are registered:
 * <ul>
 * <li>{@link OffsetDateTime}, using {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
 * <li>{@link LocalDate}, using {@link DateTimeFormatter#ISO_LOCAL_DATE}
 * <li>{@link LocalTime}, using {@link DateTimeFormatter#ISO_LOCAL_TIME}
 * <li>{@link LocalDateTime}, using {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
 * </ul>
 */
@Slf4j
public class RestPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

	private static final String DEFAULT_JACKSON_CONVERTER = "defaultJacksonConverter";
	private static final String DEFAULT_OBJECT_MAPPER = "defaultObjectMapper";

	public RestPostProcessor() {
	}

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		registerRequestScoped(registry, RestAction.class);
		registerRequestScoped(registry, RestDataSource.class);

		StandardAnnotationMetadata restErrorHandlerMetaData = new StandardAnnotationMetadata(RestErrorHandler.class);
		AnnotatedGenericBeanDefinition restErrorHandler = new AnnotatedGenericBeanDefinition(restErrorHandlerMetaData);
		registry.registerBeanDefinition("restErrorHandler", restErrorHandler);
	}

	private void registerRequestScoped(BeanDefinitionRegistry registry, Class<?> beanClass) {
		BeanDefinition bean = new AnnotatedGenericBeanDefinition(new StandardAnnotationMetadata(beanClass));
		bean.setScope(WebApplicationContext.SCOPE_REQUEST);
		registry.registerBeanDefinition(beanClass.getSimpleName(), bean);
	}

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
	public void addDateModules(ObjectMapper objectMapper) {
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

	private <T extends Temporal> SimpleModule getDateModule(Class<T> temporal, Function<String, T> parseFunction,
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
		LOGGER.debug("Added Module handling {} using {}", temporal.getName(), formatter);
		return module;
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	static class RestAction extends RestActionBase {
		@Autowired
		public RestAction(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}

	}

	static class RestDataSource extends RestDataSourceBase {
		@Autowired
		public RestDataSource(Site site, Application application, Request request, MessageSource messageSource,
				@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
			super(site, application, request, messageSource, supportPathParameters);
		}

	}

}
