/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.appng.api.model.Application;
import org.springframework.context.annotation.Configuration;

/**
 * Marks a class as the main entry point for an {@link Application}'s configuration. When using
 * {@link AppNgApplication}, {@code beans.xml} does not need to contain any beans.
 * <p>
 * Can be used with
 * 
 * <pre>
 * &#64;Configuration
 * &#64;ComponentScan("com.acme.business")
 * &#64;AppNgApplication(entityPackage = "com.acme.domain")
 * &#64;EnableJpaRepositories(basePackages = "com.acme.repository", repositoryBaseClass = SearchRepositoryImpl.class)
 * public class AcmeConfig extends ApplicationConfigJPA {
 * 
 * }
 * </pre>
 * 
 * and without JPA support:
 * 
 * <pre>
 * &#64;Configuration
 * &#64;AppNgApplication
 * &#64;ComponentScan("com.acme.business")
 * public class AcmeConfig extends ApplicationConfig {
 * 
 * }
 * </pre>
 * </p>
 * <p>
 * <strong>Each application may only provide one configuration annotated with {@code @AppNgApplication}!</strong>
 * </p>
 * 
 * @author Matthias Müller
 */
@Retention(RUNTIME)
@Target(TYPE)
@Configuration
public @interface AppNgApplication {

	/**
	 * The name of the package where JPA entities reside
	 * 
	 * @return the name of the entity package
	 */
	String entityPackage() default "";

}
