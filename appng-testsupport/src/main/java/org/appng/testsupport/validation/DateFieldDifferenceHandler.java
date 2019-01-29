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

import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldType;
import org.custommonkey.xmlunit.Difference;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * A {@link DateFieldDifferenceHandler} that ignores the {@link Difference} in the value of a {@link Datafield} of type
 * {@link FieldType#DATE}. Useful if a {@link Datasource} contains dates that vary during test-execution, like the
 * creation-date of an entity.<br/>
 * For example, the following nodes would be considered as identical:
 * 
 * <pre>
 * &lt;field name="creationDate" type="date">
 *  &lt;value>2013-10-22&lt;/value>
 * &lt;/field>
 * </pre>
 * 
 * and
 * 
 * <pre>
 * &lt;field name="creationDate" type="date">
 *  &lt;value>2012-09-21&lt;/value>
 * &lt;/field>
 * </pre>
 * 
 * Note {@link DateFieldDifferenceHandler} is getting applied to <b>the whole document</b>, ignoring every
 * {@link Difference} in the value of any {@link Datafield}s of type {@link FieldType#DATE}. If a more precise
 * distinction is required, use {@link XPathDifferenceHandler} instead.
 * 
 * @author Matthias Müller
 * 
 * 
 */
public class DateFieldDifferenceHandler extends DifferenceHandler {

	private static final String TYPE = "type";
	private static final String FIELD = "field";

	@Override
	public int differenceFound(Difference difference) {
		Node parentNode = difference.getTestNodeDetail().getNode().getParentNode();
		if (null != parentNode) {
			Node fieldNode = parentNode.getParentNode();
			if (FIELD.equals(fieldNode.getNodeName())) {
				Node typeAttr = fieldNode.getAttributes().getNamedItem(TYPE);
				if (typeAttr != null && typeAttr instanceof Attr) {
					String textContent = typeAttr.getTextContent();
					if (FieldType.DATE.name().equalsIgnoreCase(textContent)) {
						return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
					}
				}
			}
		}
		return RETURN_ACCEPT_DIFFERENCE;
	}

}
