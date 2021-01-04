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
package org.appng.core.model;

import java.io.File;

import org.appng.api.model.Application;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Properties;
import org.appng.api.search.Consumer;
import org.appng.api.search.DocumentEvent;
import org.appng.api.search.DocumentProducer;
import org.appng.core.service.ApplicationProperties;
import org.appng.search.indexer.DocumentIndexer;
import org.appng.tools.image.ImageProcessor;

/**
 * Default {@link FeatureProvider} implementation.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class FeatureProviderImpl implements FeatureProvider {

	private Properties applicationProperties;
	private ImageProcessorWrapper imageProcessorWrapper;
	private DocumentIndexer indexer;

	/**
	 * Creates a new {@link FeatureProviderImpl} using the given {@link Properties}, which have been retrieved from a
	 * {@link Application}.
	 * 
	 * @param applicationProperties
	 *            the {@link Properties} to use.
	 */
	public FeatureProviderImpl(Properties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	/**
	 * Initializes an {@link ImageProcessor}, in case the property
	 * {@value org.appng.core.service.ApplicationProperties#FEATURE_IMAGE_PROCESSING} equals to '{@code true}'.
	 * 
	 * @param imageMagickPath
	 *            the path to the ImageMagick executable
	 * @param imageCache
	 *            the folder to use for caching images
	 * @see #getImageProcessor(File, String)
	 */
	public void initImageProcessor(File imageMagickPath, File imageCache) {
		if (supports(ApplicationProperties.FEATURE_IMAGE_PROCESSING)) {
			imageProcessorWrapper = new ImageProcessorWrapper(imageMagickPath, imageCache);
		}
	}

	/**
	 * Sets the {@link DocumentIndexer} which is passed to the {@link Application} if
	 * {@value org.appng.core.service.ApplicationProperties#FEATURE_INDEXING} equals to '{@code true}'.
	 * 
	 * @param indexer
	 *            the {@link DocumentIndexer}
	 * @see #getIndexer()
	 */
	public void setIndexer(DocumentIndexer indexer) {
		this.indexer = indexer;
	}

	private boolean supports(String property) {
		return applicationProperties.getBoolean(property, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.FeatureProvider#getImageProcessor(java.io.File, java.lang.String)
	 */
	public ImageProcessor getImageProcessor(File sourceFile, String targetFile) {
		if (supports(ApplicationProperties.FEATURE_IMAGE_PROCESSING)) {
			return imageProcessorWrapper.getImageProcessor(sourceFile, targetFile);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.FeatureProvider#getImageCache()
	 */
	public File getImageCache() {
		return imageProcessorWrapper.getImageCache();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.FeatureProvider#getIndexer()
	 */
	public Consumer<DocumentEvent, DocumentProducer> getIndexer() {
		if (supports(ApplicationProperties.FEATURE_INDEXING)) {
			return indexer;
		}
		return null;
	}

	/**
	 * Wraps an {@link ImageProcessor}
	 * 
	 * @author Matthias Müller
	 * 
	 */
	class ImageProcessorWrapper {

		private final File imageCache;

		ImageProcessorWrapper(File imageMagickPath, File imageCache) {
			this.imageCache = imageCache;
			ImageProcessor.setGlobalSearchPath(imageMagickPath);
		}

		ImageProcessor getImageProcessor(File sourceFile, String targetFile) {
			return new ImageProcessor(sourceFile, new File(imageCache, targetFile));
		}

		File getImageCache() {
			return imageCache;
		}

	}
}
