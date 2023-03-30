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
package org.appng.tools.image;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ImageProcessorTest {

	private File sourceFile;

	private File targetFolder;

	@Before
	public void setup() {
		String path = System.getenv("MAGICK_HOME");
		if (path == null) {
			path = "/usr/bin";
		}
		ImageProcessor.setGlobalSearchPath(new File(path));
		Assume.assumeTrue("ImageMagick is not present.", ImageProcessor.isImageMagickPresent(null));

		URL resourceJpg = getClass().getClassLoader().getResource("images/Desert.jpg");
		sourceFile = new File(resourceJpg.getFile());
		targetFolder = new File("target/images").getAbsoluteFile();
		targetFolder.mkdirs();
	}

	public void _testQuality() throws IOException {
		ClassLoader classLoader = ImageProcessorTest.class.getClassLoader();
		URL resourceJpg = classLoader.getResource("images/Desert.jpg");
		URL resourcePng = classLoader.getResource("images/desert-landscape.png");
		compress(resourceJpg, 0d, 100d, 10d);
		compress(resourcePng, 0d, 95, 1d);
	}

	@Test
	public void testFitToWidth() throws IOException {
		ImageProcessor ip = getSource(new File(targetFolder, "desert-width-100.jpg"));
		ip.fitToWidth(100);
		ImageMetaData metaData = new ImageProcessor(ip.getImage(), null, true).getMetaData();
		assertDimensions(100, 75, metaData);
	}

	@Test
	public void testRotate() throws IOException {
		File targetFile = new File(targetFolder, "desert-rotated.jpg");
		ImageProcessor ip = getSource(targetFile);
		ip.fitToWidth(150).rotate(-45);
		ip.getImage();
	}

	@Test
	public void testCustomOp() throws IOException {
		File targetFile = new File(targetFolder, "desert-flipFlop.jpg");
		ImageProcessor ip = getSource(targetFile);
		ip.fitToWidth(200).getOp().flip().flop().gamma(2.0d);
		ip.getImage();
	}

	@Test
	public void testQuality() throws IOException {
		File targetFile = new File(targetFolder, "desert-quality-10.jpg");
		ImageProcessor ip = getSource(targetFile);
		ip.fitToWidth(384).quality(10d);
		ip.getImage();
	}

	@Test
	public void testFitToHeight() throws IOException {
		File targetFile = new File(targetFolder, "desert-height-100.jpg");
		ImageProcessor ip = getSource(targetFile);
		ip.fitToHeight(100);
		ImageMetaData metaData = new ImageProcessor(ip.getImage(), null, true).getMetaData();
		assertDimensions(133, 100, metaData);
	}

	@Test
	public void testFitToWidthAndHeight() throws IOException {
		ImageProcessor ip = getSource(new File(targetFolder, "desert-512x384.jpg"));
		ip.fitToWidthAndHeight(512, 512);
		ImageMetaData metaData = new ImageProcessor(ip.getImage(), null, true).getMetaData();
		assertDimensions(512, 384, metaData);
	}

	private ImageProcessor getSource(File targetFile) throws IOException {
		ImageProcessor ip = new ImageProcessor(sourceFile, targetFile, true);
		assertDimensions(1024, 768, ip.getMetaData());
		return ip;
	}

	private void assertDimensions(int width, int height, ImageMetaData metaData) {
		Assert.assertEquals(width, metaData.getWidth());
		Assert.assertEquals(height, metaData.getHeight());

	}

	@Test
	public void testMetaData() throws IOException {
		ImageProcessor ip = new ImageProcessor(sourceFile, null, true);
		ImageMetaData metaData = ip.getMetaData();
		Assert.assertEquals(1024, metaData.getWidth());
		Assert.assertEquals(768, metaData.getHeight());
		Assert.assertEquals(845941L, metaData.getFileSize(), 0.0d);
	}

	@Test(expected = IOException.class)
	public void testWrongTypePng() throws Exception {
		ClassLoader classLoader = ImageProcessorTest.class.getClassLoader();
		URL r1 = classLoader.getResource("images/wrong_extension/IsJpg.png");
		new ImageProcessor(new File(r1.toURI()), null, true);
	}

	@Test(expected = IOException.class)
	public void testWrongTypeJpeg() throws Exception {
		ClassLoader classLoader = ImageProcessorTest.class.getClassLoader();
		URL r1 = classLoader.getResource("images/wrong_extension/IsPng.jpeg");
		new ImageProcessor(new File(r1.toURI()), null, true);
	}

	private void compress(URL resourceJpg, double start, double end, double inc) throws IOException {
		File sourceFile = new File(resourceJpg.getFile());
		Double minRateDouble = Double.MAX_VALUE;
		for (double quality = start; quality <= end; quality += inc) {
			File targetFile = new File(sourceFile.getParentFile(), "quality-" + quality + "-" + sourceFile.getName());
			ImageProcessor ip = new ImageProcessor(sourceFile, targetFile);
			ip.quality(quality).getImage();
			System.out.println("source: " + sourceFile.length() + " Bytes");
			System.out.println("target: " + targetFile.length() + " Bytes");
			double rate = ((double) (targetFile.length() / (double) sourceFile.length())) * 100;
			if (rate < minRateDouble) {
				minRateDouble = rate;
			}
			System.out.println("quality " + quality + ": " + rate + " %");
		}
		System.out.println("+++" + minRateDouble);
	}
}
