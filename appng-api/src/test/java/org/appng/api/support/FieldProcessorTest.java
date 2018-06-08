/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.api.support;

import org.appng.api.FieldProcessor;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Sort;
import org.appng.xml.platform.SortOrder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

public class FieldProcessorTest {

	@Test
	public void testNoErrorsNoMetaData() {
		FieldProcessor fp = new FieldProcessorImpl("");
		Assert.assertFalse(fp.hasErrors());
	}

	@Test
	public void testNoErrors() {
		FieldProcessor fp = getFieldProcessor();
		Assert.assertFalse(fp.hasErrors());
	}

	@Test
	public void testGlobalError() {
		FieldProcessor fp = getFieldProcessor();
		fp.addErrorMessage("error");
		Assert.assertTrue(fp.hasErrors());
	}

	@Test
	public void testErrors() {
		FieldProcessor fp = getFieldProcessor();
		fp.addErrorMessage(fp.getField("field"), "error");
		Assert.assertTrue(fp.hasErrors());
	}

	@Test
	public void testErrorsNestedField() {
		FieldProcessor fp = getFieldProcessor();
		FieldDef field = fp.getField("field");

		FieldDef nestedField = getField("field.nested");
		nestedField.getMessages().getMessageList().add(getMessage(MessageType.ERROR));
		field.getFields().add(nestedField);

		Assert.assertTrue(fp.hasErrors());
	}

	private FieldProcessorImpl getFieldProcessor() {
		MetaData metaData = new MetaData();
		FieldDef fieldDef = getField("field");
		metaData.getFields().add(fieldDef);
		fieldDef.getMessages().getMessageList().add(getMessage(MessageType.OK));
		fieldDef.getMessages().getMessageList().add(getMessage(MessageType.NOTICE));
		fieldDef.getMessages().getMessageList().add(getMessage(MessageType.INVALID));
		return new FieldProcessorImpl("", metaData);
	}

	private FieldDef getField(String binding) {
		FieldDef fieldDef = new FieldDef();
		fieldDef.setBinding(binding);
		fieldDef.setSort(new Sort());
		Messages messages = new Messages();
		fieldDef.setMessages(messages);
		return fieldDef;
	}

	private Message getMessage(MessageType type) {
		Message message = new Message();
		message.setClazz(type);
		return message;
	}

	@Test
	public void testPageable() {
		Pageable pageable = new PageRequest(0, 20, Direction.ASC, "field");
		FieldProcessorImpl fp = getFieldProcessor();
		fp.setPageable(pageable);
		Sort sort = fp.getField("field").getSort();
		Assert.assertEquals(Integer.valueOf(0), sort.getPrio());
		Assert.assertEquals(SortOrder.ASC, sort.getOrder());
		Assert.assertEquals(fp.getPageable(), pageable);
	}

}
