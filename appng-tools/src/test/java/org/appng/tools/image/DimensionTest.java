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
package org.appng.tools.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.core.Info;
import org.im4java.process.ArrayListOutputConsumer;
import org.im4java.process.ProcessStarter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This test shows the speed difference between using {@link ImageIO#read(File)} {@link ImageReader}, {@link Info} and
 * {@link IdentifyCmd} for retrieving the image dimensions. Turns out {@link ImageReader} is ~100 times as fast
 * {@link ImageIO#read(File)} and even ~150 times as fast as {@link Info}
 * 
 * @author mueller.matthias
 * 
 */
@Ignore("run locally when needed")
public class DimensionTest {

	private static final int LOOPS = 100;
	private File sourcefile;
	private int width = -1;
	private int height = -1;
	private long size = -1L;

	@Before
	public void setup() throws URISyntaxException {
		URI uri = DimensionTest.class.getClassLoader().getResource("images/Desert.jpg").toURI();
		sourcefile = new File(uri);
		String path = System.getenv("MAGICK_HOME");
		if (null == path) {
			path = "/usr/bin";
		}
		ProcessStarter.setGlobalSearchPath(path);
	}

	@After
	public void tearDowm() {
		Assert.assertEquals(1024, width);
		Assert.assertEquals(768, height);
		Assert.assertEquals(845941L, size);
	}

	@Test
	public void testInfo() throws Exception {
		for (int i = 0; i < LOOPS; i++) {
			Info info = new Info(sourcefile.getAbsolutePath(), true);
			width = info.getImageWidth();
			height = info.getImageHeight();
			size = sourcefile.length();
		}
	}

	@Test
	public void testImageIORead() throws Exception {
		for (int i = 0; i < LOOPS; i++) {
			BufferedImage image = ImageIO.read(sourcefile);
			width = image.getWidth();
			height = image.getHeight();
			size = sourcefile.length();
		}
	}

	@Test
	public void testImageReader() throws Exception {
		for (int i = 0; i < LOOPS; i++) {
			ImageInputStream input = new FileImageInputStream(sourcefile);
			Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(input);
			if (imageReaders.hasNext()) {
				ImageReader reader = imageReaders.next();
				reader.setInput(input);
				width = reader.getWidth(0);
				height = reader.getHeight(0);
				size = sourcefile.length();
			}
			input.close();
		}
	}

	@Test
	public void testIdentifyCmd() throws IOException, InterruptedException, IM4JavaException {
		for (int i = 0; i < LOOPS; i++) {
			IMOperation op = new IMOperation();
			op.format("%b\n%W\n%H");
			op.addImage(new String[] { sourcefile.getAbsolutePath() });
			IdentifyCmd identifyCmd = new IdentifyCmd();
			ArrayListOutputConsumer outputConsumer = new ArrayListOutputConsumer();
			identifyCmd.setOutputConsumer(outputConsumer);
			identifyCmd.run(op, new Object[0]);
			List<String> output = outputConsumer.getOutput();
			width = Integer.parseInt(output.get(1));
			height = Integer.parseInt(output.get(2));
			String sizeString = output.get(0);
			int byteMark = sizeString.indexOf("B");
			if (byteMark > 0) {
				sizeString = sizeString.substring(0, byteMark);
			}
			size = Long.parseLong(sizeString);
		}
	}

}
