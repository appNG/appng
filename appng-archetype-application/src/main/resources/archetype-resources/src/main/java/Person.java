package ${package};

import org.appng.api.NotBlank;

public class Person {

	private Integer id;
	private String name;
	private String lastname;

	public Person() {

	}

	public Person(Integer id, String name, String lastname) {
		this.id = id;
		this.name = name;
		this.lastname = lastname;
	}

	@NotBlank
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@NotBlank
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
