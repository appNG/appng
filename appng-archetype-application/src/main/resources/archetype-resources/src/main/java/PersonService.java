package ${package};

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

@Service
public class PersonService {

	private final Map<Integer, Person> persons = new HashMap<>();

	public Collection<Person> getPersons() {
		return persons.values();
	}

	public Person getPerson(Integer id) {
		return persons.get(id);
	}

	@PostConstruct
	public void createPersons() {
		List<Person> list = Arrays.asList(new Person(1, "Luke", "Skywalker"), new Person(2, "Han", "Solo"),
				new Person(3, "R2", "D2"), new Person(4, "C3", "P0"));
		list.forEach(p -> persons.put(p.getId(), p));
	}

	public void updatePerson(Person p) {
		persons.put(p.getId(), p);
	}

	public void createPerson(Person p) {
		p.setId(persons.size() + 1);
		updatePerson(p);
	}

}
