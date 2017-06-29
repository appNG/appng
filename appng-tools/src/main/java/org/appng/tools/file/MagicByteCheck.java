/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.tools.file;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

/**
 * This is an utility class to to check the type of file by probing the magic bytes.
 * 
 * @author Claus Stümke, aiticon GmbH, 2016
 *
 */

public class MagicByteCheck {

	private static Logger LOG = LoggerFactory.getLogger(MagicByteCheck.class);

	/**
	 * It checks the magic bytes of the file and compares the extension by magic byte match with the extension in the
	 * file name. The magic byte match is not available for text files. Because here it is not possible to distinguish
	 * .csv from .txt files. The magic byte is the same. It is also not functioning reliable for MS Office or OpenOffice
	 * files. The method is tested for image files such as jpg, png and bmp. Please test this utility for other file
	 * types first.
	 * 
	 * @param sourceFile
	 *            the file to check
	 * @return true if the extension of the file is equal with the determined extension from magic bytes.
	 * 
	 * @throws IllegalArgumentException
	 *             if there is any issue reading the file
	 */
	public static boolean compareFileExtensionWithMagicBytes(File sourceFile) {
		StopWatch sw = new StopWatch();
		sw.start();
		String magicExtension = getExtensionByMagicBytes(sourceFile);
		String fileNameExtension = normalizeFileExtension(FilenameUtils.getExtension(sourceFile.getName()));
		boolean matches = magicExtension.equalsIgnoreCase(fileNameExtension);
		if (!matches) {
			LOG.debug("File type detected by magic byte ({}) is not identical with file extension for file {}",
					magicExtension, fileNameExtension, sourceFile.getAbsolutePath());
		}
		sw.stop();
		LOG.trace(sw.toString());
		return matches;
	}

	/**
	 * Retrieves the file's extension by magic byte detection.
	 * 
	 * @param file
	 *            the file to check
	 * @throws IllegalArgumentException
	 *             if there is any issue reading the file
	 */
	public static String getExtensionByMagicBytes(File file) {
		try {
			return normalizeFileExtension(Magic.getMagicMatch(file, false).getExtension());
		} catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
			LOG.error("Magic Exception for file " + file.getAbsolutePath(), e);
			throw new IllegalArgumentException(e);
		}
	}

	private static String normalizeFileExtension(String extension) {
		// jpeg and jpg is the same and both can be used. So we have to avoid an fail of the check because of this.
		return extension.equalsIgnoreCase("jpeg") ? "jpg" : extension;
	}
}
