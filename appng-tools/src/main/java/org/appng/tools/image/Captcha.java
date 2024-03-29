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
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.appng.tools.RandomUtil;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Captcha {

	public Captcha() {

	}

	public Integer getCaptcha(OutputStream out, int minRandom, int maxRandom, File backgroundPath, String gravity,
			String font, Integer pointsize, String color, String imageFormat) {
		int n1 = RandomUtil.randomValueBetween(minRandom, maxRandom);
		int oper = RandomUtil.randomValueBetween(0, 2);
		int n2 = RandomUtil.randomValueBetween(minRandom, maxRandom);
		for (int i = 0; i <= (n1 + oper * 7); i++) {
			n2 = RandomUtil.randomValueBetween(minRandom, maxRandom);
		}

		String calc;
		Integer result = null;
		if (oper == 0) {
			calc = n1 + " + " + n2;
			result = n1 + n2;
		} else {
			if (n1 > n2) {
				calc = n1 + " - " + n2;
				result = n1 - n2;
			} else {
				calc = n2 + " - " + n1;
				result = n2 - n1;
			}
		}
		IMOperation op = new IMOperation();
		op.addImage();
		op.gravity(gravity);
		if (StringUtils.isNotBlank(font)) {
			op.font(font);
		}
		op.pointsize(pointsize);

		if (StringUtils.isNotBlank(color)) {
			op.fill(color);
		}
		op.annotate(0, 0, 0, 0, calc + " = ");
		op.addImage(imageFormat + ":-");
		Pipe pipeOut = new Pipe(null, out);
		ConvertCmd cmd = new ConvertCmd();
		cmd.setOutputConsumer(pipeOut);
		try {
			cmd.run(op, backgroundPath.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error("IOException", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("InterruptedException", e);
		} catch (IM4JavaException e) {
			LOGGER.error("IM4JavaException", e);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// nothing to do
			}
		}
		return result;
	}

}
