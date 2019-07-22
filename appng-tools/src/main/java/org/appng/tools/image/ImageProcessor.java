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
package org.appng.tools.image;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FilenameUtils;
import org.appng.tools.file.MagicByteCheck;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.ImageCommand;
import org.im4java.process.ArrayListOutputConsumer;
import org.im4java.process.ProcessStarter;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for converting images using the
 * <a href="http://www.imagemagick.org/script/convert.php">convert</a>-command of
 * <a href="http://www.imagemagick.org">ImageMagick</a> via the API provided by
 * <a href="im4java">http://im4java.sourceforge.net/</a>
 * 
 * @author Matthias Herlitzius
 */
@Slf4j
public class ImageProcessor {

	private final File sourceFile;
	private final File targetFile;
	private final ImageCommand cmd;
	private final IMOperation op;

	/**
	 * Checks whether the ImageMagick convert-command is available.
	 * 
	 * @param imageMagickPath
	 *            the path to the convert-command, if {@code null} the system path is being used
	 * @return {@code true} if the convert-command is available, {@code false} otherwise
	 * @see #setGlobalSearchPath(File)
	 */
	public static boolean isImageMagickPresent(File imageMagickPath) {
		if (null != imageMagickPath) {
			setGlobalSearchPath(imageMagickPath);
		}
		try {
			ImageCommand convertCmd = new ConvertCmd();
			ArrayListOutputConsumer out = new ArrayListOutputConsumer();
			convertCmd.setOutputConsumer(out);
			convertCmd.run(new IMOperation().version());
			String version = out.getOutput().get(0);
			LOGGER.info(version);
			return true;
		} catch (Exception e) {
			LOGGER.error("error while retrieving ImageMagickVersion", e);
		}
		return false;
	}

	/**
	 * Creates a new {@link ImageProcessor}
	 * 
	 * @param imageMagickPath
	 *            the path to the convert-command, if {@code null} the system path is being used
	 * @param sourceFile
	 *            the source file
	 * @param targetFile
	 *            the target file
	 */
	public ImageProcessor(File imageMagickPath, File sourceFile, File targetFile) {
		if (null != imageMagickPath) {
			setGlobalSearchPath(imageMagickPath);
		}
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.cmd = new ConvertCmd();
		this.op = new IMOperation();
		op.addImage();
	}

	/**
	 * Creates a new {@link ImageProcessor}
	 * 
	 * @param imageMagickPath
	 *            the path to the convert-command, if {@code null} the system path is being used
	 * @param sourceFile
	 *            the source file
	 * @param targetFile
	 *            the target file
	 * @param checkMagicBytes
	 *            whether or not it should be checked that the source file's extension matches it's content type, using
	 *            magic byte detection
	 * @throws IOException
	 *             if {@code checkMagicBytes} is {@code true} and the source file's extension does not match it's
	 *             content type determined by magic byte detection
	 */
	public ImageProcessor(File imageMagickPath, File sourceFile, File targetFile, boolean checkMagicBytes)
			throws IOException {
		this(imageMagickPath, sourceFile, targetFile, true, checkMagicBytes);
	}

	/**
	 * Creates a new {@link ImageProcessor}
	 * 
	 * @param sourceFile
	 *            the source file
	 * @param targetFile
	 *            the target file
	 */
	public ImageProcessor(File sourceFile, File targetFile) {
		this(null, sourceFile, targetFile);
	}

	/**
	 * Creates a new {@link ImageProcessor}
	 * 
	 * @param sourceFile
	 *            the source file
	 * @param targetFile
	 *            the target file
	 * @param checkMagicBytes
	 *            whether or not it should be checked that the source file's extension matches it's content type, using
	 *            magic byte detection
	 * @throws IOException
	 *             if {@code checkMagicBytes} is {@code true} and the source file's extension does not match it's
	 *             content type determined by magic byte detection
	 */
	public ImageProcessor(File sourceFile, File targetFile, boolean checkMagicBytes) throws IOException {
		this(null, sourceFile, targetFile, false, checkMagicBytes);
	}

	private ImageProcessor(File imageMagickPath, File sourceFile, File targetFile, boolean setGlobalSearchPath,
			boolean checkMagicBytes) throws IOException {
		if (setGlobalSearchPath) {
			setGlobalSearchPath(imageMagickPath);
		}
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.cmd = new ConvertCmd();
		this.op = new IMOperation();

		if (checkMagicBytes) {
			checkSourceFileContentType();
		}
		op.addImage();
	}

	/**
	 * checks if the file extension fits with the type detected with the usage of the magic bytes.
	 */
	private void checkSourceFileContentType() throws IOException {
		String extension = FilenameUtils.getExtension(sourceFile.getName());
		String extensionByMagicBytes = MagicByteCheck.getExtensionByMagicBytes(sourceFile);
		if (!extension.equals(extensionByMagicBytes)) {
			throw new IOException(String.format(
					"File type detected by magic byte (%s) is not identical with file extension for file %s",
					extensionByMagicBytes, sourceFile.getAbsolutePath()));
		}
	}

	/**
	 * Sets the global search path to the convert-command
	 * 
	 * @param imageMagickPath
	 *            the path to the convert-command, may not be null
	 */
	public static void setGlobalSearchPath(File imageMagickPath) {
		ProcessStarter.setGlobalSearchPath(imageMagickPath.getAbsolutePath());
	}

	/**
	 * Retrieves the {@link ImageMetaData} for the source-file.
	 * 
	 * @return the {@link ImageMetaData}
	 * @throws IOException
	 *             if the source-file could not be read
	 */
	public ImageMetaData getMetaData() throws IOException {
		ImageInputStream input = new FileImageInputStream(sourceFile);
		Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(input);
		if (imageReaders.hasNext()) {
			ImageReader reader = imageReaders.next();
			reader.setInput(input);
			ImageMetaData imageMetaData = new ImageMetaData(sourceFile, reader.getWidth(0), reader.getHeight(0));
			input.close();
			return imageMetaData;
		} else {
			input.close();
			throw new IOException("no ImageReader found for " + sourceFile.getAbsolutePath());
		}
	}

	/**
	 * Adds a <a href="http://www.imagemagick.org/script/command-line-options.php#rotate">rotate</a>-option to the
	 * convert-command.
	 * 
	 * @param degrees
	 *            the degrees
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor rotate(int degrees) {
		op.rotate((double) degrees);
		return this;
	}

	/**
	 * Adds a <a href="http://www.imagemagick.org/script/command-line-options.php#resize">resize</a>-option to the
	 * convert-command. Aspect ratio may get lost, the image may be upscaled.
	 * 
	 * @param targetWidth
	 *            the target width, must be greater 0
	 * @param targetHeight
	 *            the target height, must be greater 0
	 * @param scaleUp
	 *            set to {@code true} if the image should be scaled up (in case its dimensions are smaller than the
	 *            desired ones).
	 * @return this {@link ImageProcessor}
	 * @see #resize(int, int)
	 */
	public ImageProcessor resize(int targetWidth, int targetHeight, boolean scaleUp) {
		if (!scaleUp) {
			ImageMetaData metaData;
			try {
				metaData = getMetaData();
			} catch (IOException e) {
				throw new IllegalArgumentException("error while reading " + sourceFile, e);
			}

			int sourceWidth = metaData.getWidth();
			int sourceHeight = metaData.getHeight();

			targetWidth = (targetWidth > sourceWidth) ? sourceWidth : targetWidth;
			targetHeight = (targetHeight > sourceHeight) ? sourceHeight : targetHeight;
		}
		resize(targetWidth, targetHeight);
		return this;
	}

	/**
	 * Adds a <a href="http://www.imagemagick.org/script/command-line-options.php#resize">resize</a>-option to the
	 * convert-command. Aspect ratio may get lost, the image may be upscaled.
	 * 
	 * @param targetWidth
	 *            the target width, must be greater 0
	 * @param targetHeight
	 *            the target height, must be greater 0
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor resize(int targetWidth, int targetHeight) {
		if (targetWidth > 0 && targetHeight > 0) {
			op.resize(targetWidth, targetHeight);
		}
		return this;
	}

	/**
	 * Adds a <a href="http://www.imagemagick.org/script/command-line-options.php#quality">quality</a>-option to the
	 * convert-command.
	 * 
	 * @param quality
	 *            the quality
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor quality(double quality) {
		op.quality(quality);
		return this;
	}

	/**
	 * Adds a <a href="http://www.imagemagick.org/script/command-line-options.php#strip">strip</a>-option to the
	 * convert-command. This removes all comments and profiles from the image.
	 * 
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor strip() {
		op.strip();
		return this;
	}

	/**
	 * TODO doc
	 * 
	 * @param positionWidth
	 * @param positionHeight
	 * @param sourceWidth
	 * @param sourceHeight
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor autoCrop(int positionWidth, int positionHeight, int sourceWidth, int sourceHeight) {
		return autoCrop(positionWidth, positionHeight, sourceWidth, sourceHeight, 0, 0);
	}

	/**
	 * TODO doc
	 * 
	 * @param positionWidth
	 * @param positionHeight
	 * @param variantWidth
	 * @param variantHeight
	 * @param variantOffsetWidth
	 * @param variantOffsetHeight
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor autoCrop(int positionWidth, int positionHeight, int variantWidth, int variantHeight,
			int variantOffsetWidth, int variantOffsetHeight) {
		int offsetWidth;
		int offsetHeight;
		int uncroppedPositionWidth;
		int uncroppedPositionHeight;

		double variantFactor = variantWidth / (double) variantHeight;
		double positionFactor = positionWidth / (double) positionHeight;

		if (variantFactor > positionFactor) {
			uncroppedPositionWidth = Math.round(variantHeight * positionWidth / (float) positionHeight);
			uncroppedPositionHeight = variantHeight;
			offsetWidth = variantOffsetWidth + Math.round((variantWidth - uncroppedPositionWidth) / (float) 2);
			offsetHeight = variantOffsetHeight;
		} else {
			uncroppedPositionWidth = variantWidth;
			uncroppedPositionHeight = Math.round(positionHeight * variantWidth / (float) positionWidth);
			offsetWidth = variantOffsetWidth;
			offsetHeight = variantOffsetHeight + Math.round((variantHeight - uncroppedPositionHeight) / (float) 2);
		}

		crop(uncroppedPositionWidth, uncroppedPositionHeight, offsetWidth, offsetHeight);
		return resize(positionWidth, positionHeight);
	}

	/**
	 * Adds a <a href="http://www.imagemagick.org/script/command-line-options.php#crop">crop</a>-option to the
	 * convert-command.
	 * 
	 * @param targetWidth
	 *            the width of the cutting
	 * @param targetHeight
	 *            the height of the cutting
	 * @param offsetWidth
	 *            the width-offset to start cropping at.
	 * @param offsetHeight
	 *            the height-offset to start cropping at.
	 * @return this {@link ImageProcessor}
	 */
	public ImageProcessor crop(int targetWidth, int targetHeight, int offsetWidth, int offsetHeight) {
		op.crop(targetWidth, targetHeight, offsetWidth, offsetHeight);
		return this;
	}

	/**
	 * Executes the convert-command with all the previously applied options and returns the target-file this
	 * {@link ImageProcessor} was created with.
	 * 
	 * @return the target-file this {@link ImageProcessor} was created with
	 * @throws IOException
	 *             if something goes wrong while executing the convert-command
	 */
	public File getImage() throws IOException {
		op.interlace("Plane");
		op.addImage();
		try {
			cmd.run(op, sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
		} catch (InterruptedException ex) {
			throw new IOException(ex);
		} catch (IM4JavaException ex) {
			throw new IOException(ex);
		}
		return targetFile;
	}

	/**
	 * Fits the image to the given maximum width and height using {@link #resize(int, int)}. No upscaling is done here,
	 * the aspect ratio is being kept.
	 * 
	 * @param maxwidth
	 *            the maximum width of the image
	 * @param maxHeight
	 *            the maximum height of the image
	 * @return this {@link ImageProcessor}
	 * @throws IOException
	 *             if no {@link ImageMetaData} could be obtained from the source file
	 * 
	 * @see #resize(int, int)
	 */
	public ImageProcessor fitToWidthAndHeight(int maxwidth, int maxHeight) throws IOException {
		ImageMetaData metaData = getMetaData();
		int width = metaData.getWidth();
		int height = metaData.getHeight();
		float ratioWidth = (float) maxwidth / width;
		float ratioHeight = (float) maxHeight / height;
		float ratio = Math.min(ratioWidth, ratioHeight);
		int scaledWidth = Math.round(width * ratio);
		int scaledHeight = Math.round(height * ratio);
		return resize(scaledWidth, scaledHeight);
	}

	/**
	 * Fits the image to the given maximum width using {@link #resize(int, int)}. No upscaling is done here, the aspect
	 * ratio is being kept.
	 * 
	 * @param maxWidth
	 *            the maximum width of the image
	 * @return this {@link ImageProcessor}
	 * @throws IOException
	 *             if no {@link ImageMetaData} could be obtained from the source file
	 * @see #resize(int, int)
	 */
	public ImageProcessor fitToWidth(Integer maxWidth) throws IOException {
		ImageMetaData metaData = getMetaData();
		int width = metaData.getWidth();
		int height = metaData.getHeight();
		if (width > maxWidth) {
			width = maxWidth;
			height = Math.round(((float) maxWidth / width) * height);
		}
		return resize(width, height);
	}

	/**
	 * Fits the image to the given maximum height using {@link #resize(int, int)}. No upscaling is done here, the aspect
	 * ratio is being kept.
	 * 
	 * @param maxHeight
	 *            the maximum height of the image
	 * @return this {@link ImageProcessor}
	 * @throws IOException
	 *             if no {@link ImageMetaData} could be obtained from the source file
	 * @see #resize(int, int)
	 */
	public ImageProcessor fitToHeight(Integer maxHeight) throws IOException {
		ImageMetaData metaData = getMetaData();
		int width = metaData.getWidth();
		int height = metaData.getHeight();
		if (height > maxHeight) {
			height = maxHeight;
			width = Math.round(((float) maxHeight / height) * width);
		}
		return resize(width, height);
	}

	public IMOperation getOp() {
		return op;
	}

}
