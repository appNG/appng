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
package org.appng.testsupport.validation;

import java.io.IOException;

import org.appng.xml.platform.Datafield;
import org.junit.Test;

public class XPathDifferenceHandlerTest extends DifferenceHandlerTest {

	@Test(expected = AssertionError.class)
	public void testValidateFailNoStripPositions() throws IOException {
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler(false);
		differenceListener.ignoreDifference("/result[1]/field[2]/value[1]/text()[1]");
		run(differenceListener);
	}

	@Test
	public void testValidateOKNoStripPositions() throws IOException {
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler(false);
		differenceListener.ignoreDifference("/result[1]/field[1]/value[1]/text()[1]");
		run(differenceListener);
	}

	@Test
	public void testValidateOKNoControlNode() throws IOException {
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler();
		differenceListener.ignoreDifference("/result[1]");
		differenceListener.ignoreDifference("/result[1]/field[2]");
		differenceListener.ignoreDifference("/result/field/value/text()");
		result.getFields().add(new Datafield());
		run(differenceListener);
	}

	@Test
	public void testValidateOK() throws IOException {
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler();
		differenceListener.ignoreDifference("/result/field/value/text()");
		run(differenceListener);
	}

}
