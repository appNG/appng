package ${package};

import org.appng.api.ActionProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonAction implements ActionProvider<Person> {

	private PersonService service;

	@Autowired
	public PersonAction(PersonService service) {
		this.service = service;
	}

	@Override
	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			Person formBean, FieldProcessor fieldProcessor) {
		String mode = options.getOptionValue("action", "id");
		if ("edit".equals(mode)) {
			Integer personId = request.convert(options.getOptionValue("action", "person"), Integer.class);
			formBean.setId(personId);
			service.updatePerson(formBean);
			String message = request.getMessage("person.edited");
			fieldProcessor.addOkMessage(message);
		} else if ("create".equals(mode)) {
			service.createPerson(formBean);
			String message = request.getMessage("person.created");
			fieldProcessor.addOkMessage(message);
		}

	}
}
