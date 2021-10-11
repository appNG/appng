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
package org.appng.tools.image;

import java.io.File;

/**
 * @author herlitzius.matthias
 */
public class ImageMetaData {

	private final File file;
	private final int width;
	private final int height;

	public ImageMetaData(File file, int width, int height) {
		this.file = file;
		this.width = width;
		this.height = height;
	}

	public File getFile() {
		return file;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public long getFileSize() {
		return file.length();
	}

	public String toString() {
		return file + " (" + width + "x" + height + ", " + getFileSize() + " bytes)";
	}
}
