package ${package};

import java.io.IOException;
import java.util.Properties;

import org.appng.api.ProcessingException;
import org.appng.api.support.CallableDataSource;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = PersonDataSourceTest.class)
public class PersonDataSourceTest extends TestBase {

	public PersonDataSourceTest() {
		super("myapp", "application-home");
	}

	@Override
	protected Properties getProperties() {
		return new Properties();
	}

	@Test
	public void testPersons() throws ProcessingException, IOException {
		CallableDataSource callableDataSource = getDataSource("persons").getCallableDataSource();
		callableDataSource.perform("page");
		WritingXmlValidator.validateXml(callableDataSource.getDatasource(), "xml/PersonDataSourceTest-testPersons.xml");
	}
}
