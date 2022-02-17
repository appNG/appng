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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.AllArgsConstructor;

/**
 * A {@link Configuration} providing commonly used {@link HttpMessageConverter}s (for binary and JSON content) and a
 * {@link HandlerMethodArgumentResolver} that can resolve
 * 
 * @author Matthias Müller
 */
@Configuration
public class RestConfig {

	@Bean
	public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
		return new ByteArrayHttpMessageConverter();
	}

	/**
	 * Creates an {@link ObjectMapper} that can (de)serialize {@link OffsetDateTime} using the
	 * {@link DateTimeFormatter#ISO_DATE_TIME} pattern.
	 * 
	 * @return the {@link ObjectMapper}
	 */
	@Bean
	public ObjectMapper defaultObjectMapper() {
		SimpleModule dateModule = new SimpleModule();
		dateModule.addSerializer(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
			@Override
			public void serialize(OffsetDateTime value, JsonGenerator jsonGenerator, SerializerProvider provider)
					throws IOException {
				if (value != null) {
					jsonGenerator.writeString(DateTimeFormatter.ISO_DATE_TIME.format(value));
				}
			}
		});
		dateModule.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
			@Override
			public OffsetDateTime deserialize(JsonParser parser, DeserializationContext ctxt)
					throws IOException, JacksonException {
				return OffsetDateTime.parse(parser.getText(), DateTimeFormatter.ISO_DATE_TIME);
			}
		});
		// @formatter:off
		return new ObjectMapper()
			.setDefaultPropertyInclusion(Include.NON_ABSENT)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.registerModule(dateModule);
		// @formatter:on
	}

	@Bean
	public MappingJackson2HttpMessageConverter defaultJsonConverter(
			@Qualifier("defaultObjectMapper") ObjectMapper defaultObjectMapper,
			@Value("${site.jsonPrettyPrint:false}") boolean prettyPrint) {
		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(
				defaultObjectMapper);
		jsonConverter.setPrettyPrint(prettyPrint);
		return jsonConverter;
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
