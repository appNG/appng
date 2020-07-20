/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.core.service;

import org.appng.api.model.Application;
import org.appng.api.model.FeatureProvider;

/**
 * Utility class providing {@link Application}-related constants.
 * 
 * @author Matthias Müller
 */
public class ApplicationProperties {

	/**
	 * Constant for the feature image-processing.
	 * 
	 * @see FeatureProvider#getImageCache()
	 * @see FeatureProvider#getImageProcessor(java.io.File, String)
	 */
	public static final String FEATURE_IMAGE_PROCESSING = "featureImageProcessing";

	/**
	 * Constant for the feature indexing.
	 * 
	 * @see FeatureProvider#getIndexer()
	 */
	public static final String FEATURE_INDEXING = "featureIndexing";

	/**
	 * Name for the application property that contains a comma-separated list of active profiles
	 */
	public static final String PROP_ACTIVE_PROFILES = "activeProfiles";

	/**
	 * Name for the application property that contains a multiline value for configuring application caches.<br/>
	 * Format:
	 * 
	 * <pre>
	 * mycache.ttl = 3600
	 * mycache.maxIdle = 3600
	 * </pre>
	 */
	public static final String PROP_CACHE_CONFIG = "cacheConfig";

	/**
	 * An array containing the names of all available {@link Application} features.
	 * 
	 * @see FeatureProvider
	 */
	public static final String[] FEATURES = { FEATURE_IMAGE_PROCESSING, FEATURE_INDEXING };

	/**
	 * Optional property describing the package where Flyway Java migration reside
	 */
	public static final String FLYWAY_MIGRATION_PACKAGE = "flywayMigrationPackage";

	private ApplicationProperties() {

	}

}
