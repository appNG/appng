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
package org.appng.testsupport.validation;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.NodeDetail;

/**
 * A {@link DifferenceListener} which ignores the {@link Difference} if the XPath of the node was added to ignorable
 * expressions using {@link #ignoreDifference(String)}. Multiple XPath expressions can be added to the ignore list by
 * calling {@link #ignoreDifference(String)}. Note that these expressions must be the complete absolute XPath location
 * starting from the document root.
 * 
 * @author Matthias Müller
 * 
 * @see Difference#getControlNodeDetail()
 * @see Difference#getTestNodeDetail()
 * @see NodeDetail#getXpathLocation()
 */
public class XPathDifferenceHandler extends DifferenceHandler {

	private static final String POSITON_PATTERN = "\\[\\d+\\]";
	private Map<String, DifferenceListener> handlers = new HashMap<>();
	private boolean stripPositions;

	/**
	 * Creates a new {@link XPathDifferenceHandler}.
	 * <p>
	 * If there is a difference you want to ignore at the following XPath location
	 * 
	 * <pre>
	 * /datasource/data/selection[2]/option[4]
	 * </pre>
	 * 
	 * you can achieve this by either setting up your {@link XPathDifferenceHandler} like this
	 * 
	 * <pre>
	 * DifferenceHandler differenceHandler = new XPathDifferenceHandler(true);
	 * differenceHandler.ignoreDifference(&quot;/datasource/data/selection/option&quot;);
	 * </pre>
	 * 
	 * or this
	 * 
	 * <pre>
	 * DifferenceHandler differenceHandler = new XPathDifferenceHandler(false);
	 * differenceHandler.ignoreDifference(&quot;/datasource/data/selection[2]/option[4]&quot;);
	 * </pre>
	 * 
	 * In the first case, with {@code stripPositions = true}, you can omit the positions from the expression. As a side
	 * effect, every {@link Difference} with the (stripped) XPath of {@code /datasource/data/selection/option} will be
	 * ignored.<br/>
	 * In the second case, there needs to be an exact match between the registered XPath location and the one from the
	 * {@link Difference}.
	 * </p>
	 * 
	 * @param stripPositions
	 *                       whether or not to strip the positions from the actual XPath of the {@link Difference}
	 *                       before checking the registry for an appropriate {@link DifferenceListener}
	 */
	public XPathDifferenceHandler(boolean stripPositions) {
		this.stripPositions = stripPositions;
	}

	/**
	 * Delegates to {@link #XPathDifferenceHandler(boolean)} with {@code stripPositions = true}.
	 * 
	 * @see #XPathDifferenceHandler(boolean)
	 */
	public XPathDifferenceHandler() {
		this(true);
	}

	/**
	 * Ignore the {@link Difference} with the given XPath expression
	 * 
	 * @param xpath
	 *              the XPath expression. Note that this must be the complete absolute XPath location starting from the
	 *              document root.
	 */
	public void ignoreDifference(String xpath) {
		addDifferenceListener(xpath, new DifferenceHandler() {
			@Override
			public int differenceFound(Difference difference) {
				return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
			}
		});
	}

	/**
	 * Registers a custom {@link DifferenceListener} for the given XPath expression
	 * 
	 * @param xpath
	 *                           the XPath expression. Note that this must be the complete absolute XPath location
	 *                           starting from the document root.
	 * @param differenceListener
	 *                           the {@link DifferenceListener} to register
	 */
	public void addDifferenceListener(String xpath, DifferenceListener differenceListener) {
		handlers.put(xpath, differenceListener);
	}

	@Override
	public int differenceFound(Difference difference) {
		NodeDetail controlNodeDetail = difference.getControlNodeDetail();
		String xpathLocation = controlNodeDetail.getXpathLocation();
		if (null == xpathLocation) {
			xpathLocation = difference.getTestNodeDetail().getXpathLocation();
		}
		DifferenceListener differenceListener = handlers.get(xpathLocation);
		if (null == differenceListener && stripPositions) {
			differenceListener = handlers.get(xpathLocation.replaceAll(POSITON_PATTERN, ""));
		}
		if (null != differenceListener) {
			return differenceListener.differenceFound(difference);
		}
		return RETURN_ACCEPT_DIFFERENCE;
	}
}
