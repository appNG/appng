package org.appng.api.support.field;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CheckboxFieldConverterTest extends AbstractFieldConverterTest {

	@Before
	public void setup() throws Exception {
		super.setup(FieldType.CHECKBOX);
		Mockito.when(request.hasParameter(OBJECT))
				.thenAnswer(i -> ((RequestContainer) i.getMock()).getParameter(OBJECT) != null);
	}

	@Override
	@Test
	public void testSetObject() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("true");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Boolean.TRUE, fieldWrapper.getObject());
	}
	
	@Test
	public void testSetObjectOn() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("on");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Boolean.TRUE, fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectFalse() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("false");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Boolean.TRUE, fieldWrapper.getObject());
	}

	@Override
	@Test
	public void testSetObjectEmptyValue() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Boolean.TRUE, fieldWrapper.getObject());
	}

	@Override
	public void testSetObjectInvalidValue() throws Exception {
		// nothing to do
	}

	@Override
	@Test
	public void testSetObjectNull() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn(null);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Boolean.FALSE, fieldWrapper.getObject());
	}

	@Override
	@Test
	public void testSetString() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, Boolean.FALSE);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals("false", fieldWrapper.getStringValue());
	}

	@Override
	@Test
	public void testSetStringNullObject() throws Exception {
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
	}

	@Override
	public void testSetStringInvalidType() throws Exception {
		// nothing to do
	}

	@Override
	public Container<?> getContainer() {
		return new Container<Boolean>() {
		};
	}

	@Override
	@Test
	public void testAddField() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, Boolean.TRUE);
		DatafieldOwner datafieldOwner = getDatafieldOwner();
		fieldConverter.addField(datafieldOwner, fieldWrapper);
		Datafield datafield = datafieldOwner.getFields().get(0);
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals("true", datafield.getValue());
		Assert.assertEquals(0, datafield.getFields().size());
	}
}
