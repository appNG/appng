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

import static org.custommonkey.xmlunit.DifferenceConstants.CDATA_VALUE;
import static org.custommonkey.xmlunit.DifferenceConstants.TEXT_VALUE;

import java.util.Arrays;
import java.util.List;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.w3c.dom.Node;

/**
 * A {@link DifferenceListener} which ignores the (text/cdata) values of certain elements.<br/>
 * For example, if setting up a {@link TextValueDifferenceHandler} like this
 * 
 * <pre>
 * DifferenceHandler handler = new TextValueDifferenceHandler(&quot;value&quot;);
 * </pre>
 * 
 * the following nodes would be considered as identical:
 * 
 * <pre>
 * &lt;field name="foo" type="text">
 *  &lt;value>bar&lt;/value>
 * &lt;/field>
 * </pre>
 * 
 * and
 * 
 * <pre>
 * &lt;field name="foo" type="text">
 *  &lt;value>foo&lt;/value>
 * &lt;/field>
 * </pre>
 * 
 * Note {@link TextValueDifferenceHandler} is getting applied to <b>the whole document</b>, ignoring every
 * {@link Difference} in the text value of any element with the defined name. If a more precise distinction is required,
 * use {@link XPathDifferenceHandler} instead.
 * 
 * @author Matthias Müller
 * 
 */
public class TextValueDifferenceHandler extends DifferenceHandler {

	private List<String> ignored;

	/**
	 * 
	 * @param ignored
	 *            name(s) of the attribute(s) to ignore the value for
	 */
	public TextValueDifferenceHandler(String... ignored) {
		this.ignored = Arrays.asList(ignored);
	}

	@Override
	public int differenceFound(Difference difference) {
		Node parentNode = difference.getTestNodeDetail().getNode().getParentNode();
		if (null != parentNode) {
			boolean isIgnoreable = ignored.indexOf(parentNode.getNodeName()) > -1;
			boolean isDifferent = TEXT_VALUE.equals(difference) || CDATA_VALUE.equals(difference);
			if (isDifferent && isIgnoreable) {
				return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
			}
		}
		return RETURN_ACCEPT_DIFFERENCE;
	}

}
