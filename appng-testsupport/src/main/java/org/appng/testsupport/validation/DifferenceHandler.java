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
package org.appng.testsupport.validation;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.w3c.dom.Node;

/**
 * A convenience {@link DifferenceListener} with an empty {@link #skippedComparison(Node, Node)}-implementation.
 * 
 * @author Matthias Müller
 * 
 * @see WritingXmlValidator#validateXml(org.appng.xml.BaseObject, String, DifferenceListener)
 * @see WritingXmlValidator#validateXml(String, java.io.File, DifferenceListener)
 * @see WritingXmlValidator#validateXml(String, String, DifferenceListener)
 * @see WritingXmlValidator#validateXml(String, java.io.File, boolean, DifferenceListener)
 * @see WritingXmlValidator#validateXml(String, String, boolean, DifferenceListener)
 */
public abstract class DifferenceHandler implements DifferenceListener {

	public abstract int differenceFound(Difference difference);

	/**
	 * Default (empty) implementation, may be overwritten.
	 * 
	 * @see DifferenceListener#skippedComparison(Node, Node)
	 */
	public void skippedComparison(Node paramNode1, Node paramNode2) {
	}

}
