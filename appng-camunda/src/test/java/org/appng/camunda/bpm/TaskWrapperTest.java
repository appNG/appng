package org.appng.camunda.bpm;

import java.util.Arrays;
import java.util.Date;

import org.appng.api.support.FieldProcessorImpl;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.FormType;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.camunda.bpm.engine.impl.form.TaskFormDataImpl;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;
import org.camunda.bpm.engine.impl.form.type.DateFormType;
import org.camunda.bpm.engine.impl.form.type.LongFormType;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.camunda.bpm.engine.impl.persistence.entity.IdentityLinkEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.camunda.bpm.engine.variable.value.BooleanValue;
import org.camunda.bpm.engine.variable.value.DateValue;
import org.camunda.bpm.engine.variable.value.LongValue;
import org.camunda.bpm.engine.variable.value.StringValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.junit.Assert;
import org.junit.Test;

public class TaskWrapperTest {

	@Test
	public void test() {
		IdentityLinkEntity user = new IdentityLinkEntity();
		user.setType(IdentityLinkType.CANDIDATE);
		user.setUserId("user");
		IdentityLinkEntity group = new IdentityLinkEntity();
		group.setType(IdentityLinkType.CANDIDATE);
		group.setGroupId("group");
		TaskEntity task = new TaskEntity("testtask");
		TaskWrapper wrapper = new TaskWrapper(task, Arrays.asList(user, group), new VariableMapImpl());

		FieldProcessorImpl fp = new FieldProcessorImpl("test");
		TaskFormDataImpl taskFormData = new TaskFormDataImpl();
		taskFormData.setTask(task);

		StringValue stringValue = Variables.stringValue("a");
		taskFormData.getFormFields().add(getField("stringField", new StringFormType(), stringValue));

		BooleanValue booleanValue = Variables.booleanValue(true);
		taskFormData.getFormFields().add(getField("booleanField", new BooleanFormType(), booleanValue));

		LongValue longValue = Variables.longValue(42L);
		taskFormData.getFormFields().add(getField("longField", new LongFormType(), longValue));

		DateValue dateValue = Variables.dateValue(new Date());
		String datePattern = "yy-MM-dd";
		taskFormData.getFormFields().add(getField("dateField", new DateFormType(datePattern), dateValue));

		String mandatoryMessage = "Mandatory!";
		wrapper.addFormFields(fp, taskFormData, mandatoryMessage);

		fp = new FieldProcessorImpl(fp.getReference(), fp.getMetaData());
		FieldDef stringField = fp.getField("formFields['stringField']");
		Assert.assertEquals(FieldType.TEXT, stringField.getType());
		Assert.assertEquals(FieldType.CHECKBOX, fp.getField("formFields['booleanField']").getType());
		Assert.assertEquals(FieldType.LONG, fp.getField("formFields['longField']").getType());
		FieldDef dateField = fp.getField("formFields['dateField']");
		Assert.assertEquals(datePattern, dateField.getFormat());
		Assert.assertEquals(FieldType.DATE, dateField.getType());

		Assert.assertEquals(stringValue.getValue(), wrapper.getFormFields().get("stringField"));
		Assert.assertEquals(booleanValue.getValue(), wrapper.getFormFields().get("booleanField"));
		Assert.assertEquals(longValue.getValue(), wrapper.getFormFields().get("longField"));
		Assert.assertEquals(dateValue.getValue(), wrapper.getFormFields().get("dateField"));

		Assert.assertNotNull(stringField.getValidation());
		Assert.assertNotNull(stringField.getValidation().getNotNull());

		wrapper.getFormFields().remove("stringField");
		wrapper.validate(null, null, null, null, null, fp, taskFormData, mandatoryMessage);
		Assert.assertTrue(fp.hasErrors());

		Assert.assertEquals(mandatoryMessage, stringField.getMessages().getMessageList().get(0).getContent());

	}

	private FormField getField(String id, FormType type, TypedValue value) {
		FormFieldImpl field = new FormFieldImpl();
		field.setId(id);
		field.setType(type);
		field.getValidationConstraints().add(new FormFieldValidationConstraintImpl("required", null));
		field.setValue(value);
		return field;
	}

}
