/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.api.model;

import java.io.File;

import org.appng.api.search.Consumer;
import org.appng.api.search.DocumentEvent;
import org.appng.api.search.DocumentProducer;
import org.appng.tools.image.ImageProcessor;

/**
 * A {@link Application} can make use of several features provided by appNG. To enable a feature, a certain
 * {@link Property} needs to be set for the {@link Application}. This can be done in the application's
 * {@code application-info.xml} or via the administration-tool.
 * <p/>
 * 
 * @author Matthias Herlitzius
 */
public interface FeatureProvider {

	/**
	 * Returns an {@link ImageProcessor} for the given sourceFile.<br/>
	 * To enable this feature, set the {@link Application}'s {@link Property} {@code featureImageProcessing} to
	 * {@code true}.
	 * 
	 * @param sourceFile
	 *                   the source image (should have an absolute path)
	 * @param targetFile
	 *                   the path to the target file to be created, relative to
	 *                   {@link org.appng.api.Platform.Property#IMAGE_CACHE_FOLDER}.
	 * 
	 * @return an {@link ImageProcessor} for the given source file
	 */
	ImageProcessor getImageProcessor(File sourceFile, String targetFile);

	/**
	 * Returns the folder to use for caching images, only available if {@code featureImageProcessing} is enabled.
	 * 
	 * @see FeatureProvider#getImageProcessor(File, String)
	 * 
	 * @return the cache folder for images
	 */
	File getImageCache();

	/**
	 * Returns a {@link Consumer} which consumes {@link DocumentEvent}s produced by a {@link DocumentProducer}.The
	 * returned {@link Consumer} writes to the <a href="http://lucene.apache.org/">Lucene</a>-powered<br/>
	 * search-index of the {@link Site}.<br/>
	 * To enable this feature, set the {@link Application}'s {@link Property} {@code featureIndexing} to {@code true} .
	 * 
	 * @return a {@link Consumer} consuming {@link DocumentEvent}s produced by a {@link DocumentProducer}
	 */
	Consumer<DocumentEvent, DocumentProducer> getIndexer();

}
