package ${package};

import java.io.IOException;
import java.util.Properties;

import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = PersonActionTest.class)
public class PersonActionTest extends TestBase {

	public PersonActionTest() {
		super("myapp", "application-home");
	}

	@Override
	protected Properties getProperties() {
		return new Properties();
	}

	@Test
	public void testEditPerson() throws ProcessingException, IOException {
		CallableAction callableAction = getAction("personEvent", "editPerson").withParam(FORM_ACTION, "editPerson")
				.withParam("id", "1").getCallableAction(new Person(1, "Luke the Duke", "Skywalker"));
		callableAction.perform();
		WritingXmlValidator.validateXml(callableAction.getAction(), "xml/PersonActionTest-testEditPerson.xml");
	}

	@Test
	public void testCreatePerson() throws ProcessingException, IOException {
		CallableAction callableAction = getAction("personEvent", "createPerson").withParam(FORM_ACTION, "createPerson")
				.getCallableAction(new Person(null, "Obi Wan", "Kenobi"));
		callableAction.perform();
		WritingXmlValidator.validateXml(callableAction.getAction(), "xml/PersonActionTest-testCreatePerson.xml");
	}
}
