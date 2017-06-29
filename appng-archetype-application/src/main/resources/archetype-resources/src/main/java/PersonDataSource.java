package ${package};

import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonDataSource implements DataProvider {

	private PersonService service;

	@Autowired
	public PersonDataSource(PersonService service) {
		this.service = service;
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fieldProcessor) {
		DataContainer dataContainer = new DataContainer(fieldProcessor);
		Integer personId = request.convert(options.getOptionValue("person", "id"), Integer.class);
		if (null == personId) {
			dataContainer.setPage(service.getPersons(), fieldProcessor.getPageable());
		} else if (Integer.valueOf(-1).equals(personId)) {
			dataContainer.setItem(new Person());
		} else {
			dataContainer.setItem(service.getPerson(personId));
		}
		return dataContainer;
	}
}
